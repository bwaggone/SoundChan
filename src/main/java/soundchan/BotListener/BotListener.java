package soundchan.BotListener;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import org.jetbrains.annotations.NotNull;
import soundchan.*;

import java.nio.file.WatchEvent;
import java.sql.SQLOutput;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class BotListener extends ListenerAdapter{

    private long monitoredGuildId = -1;
    private Guild monitoredGuild;
    private static LocalAudioManager localManager;
    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;
    private BotListenerHelpers helper = new BotListenerHelpers();
    private Map<String, Future<?> > otherTasks;
    private boolean hasAudience;
    private Timer timer;

    // From configuration file
    private static String followingUser;
    private static String localFilePath;
    private static boolean audioOnUserJoin;

    public BotListener(Properties properties) {
        this.musicManagers = new HashMap<>();

        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);

        hasAudience = false;
        timer = new Timer();
        loadProperties(properties);
    }

    /**
     * Loads various properties from config file
     * @param properties Object holding the contents of the property file
     */
    private void loadProperties(@NotNull Properties properties) {
        localFilePath = properties.getProperty("localFilePath");
        followingUser = properties.getProperty("followingUser");
        audioOnUserJoin = settingEnableCheck(properties.getProperty("audioOnUserJoin"));
        otherTasks = new HashMap<>();
        if(audioOnUserJoin) {
            String userAudioPath = properties.getProperty("userAudioFilePath");
            if(userAudioPath == null || userAudioPath.contentEquals("")) {
                userAudioPath = "usersound.properties";
            }
            localManager = new LocalAudioManager(localFilePath, userAudioPath);

            if(settingEnableCheck(properties.getProperty("watchUserSoundFile"))) {
                addWatcherTask(new MediaWatcherListener() {
                    @Override
                    public void onWatchEvent(WatchEvent event) {
                        localManager.UpdateUserAudio();
                    }
                }, userAudioPath, "watchUserSoundFile", false);
            }
        }
        else
            localManager = new LocalAudioManager(localFilePath);

        if(settingEnableCheck(properties.getProperty("watchLocalFilePath"))) {
            addWatcherTask(new MediaWatcherListener() {
                @Override
                public void onWatchEvent(WatchEvent event) {
                    localManager.UpdateFiles();
                }
            }, localFilePath, "watchLocalFilePath", true);
        }

    }

    private synchronized GuildMusicManager getGuildAudioPlayer() {
        long guildId = monitoredGuildId;
        GuildMusicManager musicManager = musicManagers.get(guildId);

        if (musicManager == null) {
            musicManager = new GuildMusicManager(playerManager);
            musicManagers.put(guildId, musicManager);
        }

        monitoredGuild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

        return musicManager;
    }

    /**
     * Plays an audio clip when a user connects to the voice channel if enabled in the config file. For the sound to play,
     * there needs to be a sound file with the same name as the user, otherwise it won't play anything.
     * @param event
     */
    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        if(audioOnUserJoin) {
            String filepath = localManager.GetFilePath(event.getMember().getEffectiveName());
            if (!filepath.contentEquals("")) {
                GuildMusicManager musicManager = getGuildAudioPlayer();

                playerManager.loadItemOrdered(musicManager, filepath, new AudioLoadResultHandler() {
                    @Override
                    public void trackLoaded(AudioTrack track) {
                        play(monitoredGuild, musicManager, track, true);
                    }

                    @Override
                    public void playlistLoaded(AudioPlaylist playlist) {
                        AudioTrack firstTrack = playlist.getSelectedTrack();

                        if (firstTrack == null) {
                            firstTrack = playlist.getTracks().get(0);
                        }
                        play(monitoredGuild, musicManager, firstTrack, false);
                    }

                    @Override
                    public void noMatches() {
                        // Needed, but shouldn't be called
                        System.out.println("Nothing found for " + event.getMember().getEffectiveName());
                    }

                    @Override
                    public void loadFailed(FriendlyException exception) {
                        // Needed, but shouldn't be called
                        System.out.println("Could not play: " + exception.getMessage());
                    }
                });
            }
        }
        hasAudience = true;
        super.onGuildVoiceJoin(event);
    }


    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        if(event.getMember().getEffectiveName().compareTo(followingUser) == 0) {
            AudioManager audioManager = monitoredGuild.getAudioManager();
            if(!audioManager.isAttemptingToConnect()) {
                audioManager.openAudioConnection(event.getChannelJoined());
            }
        }
        super.onGuildVoiceMove(event);
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        if(event.getChannelLeft().getMembers().size() == 1) {   // If only member in chat is SoundChan
            hasAudience = false;
            getGuildAudioPlayer().player.setPaused(true);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if(!hasAudience) {
                        clearQueue();
                        AudioManager audioManager = monitoredGuild.getAudioManager();
                        audioManager.closeAudioConnection();
                    }
                }
            }, 10000); // Wait 10 seconds before leaving channel
        }
        super.onGuildVoiceLeave(event);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String[] command = event.getMessage().getContentRaw().split(" ", 2);

        Guild guild = null;
        if(event.isFromGuild()) {
            guild = event.getGuild();
        }
        MessageChannel channel = helper.GetReplyChannel(event);

        // If we haven't set the Monitored Guild yet, set the value
        if(monitoredGuildId == -1 && guild != null){
            monitoredGuildId = Long.parseLong(guild.getId());
            monitoredGuild = guild;
        }

        if(monitoredGuild != null){

            // "!" Signifies that you're looking to play a sound effect
            if(command[0].startsWith("!") && command[0].length() > 1){
                String filepath = localManager.GetFilePath(command[0].substring(1));
                if(!filepath.contentEquals("")) {
                    loadAndPlay(channel, filepath, true);
                }
                else{
                    channel.sendMessage("File \"" + command[0].substring(1) + "\" not found!").queue();
                }
            }

            if(command[0].startsWith("~")){
                Commands enumCommand = Commands.valueOf(command[0].substring(1));
                if(enumCommand == Commands.play){
                    // Play a song or video
                    if(command.length == 2)
                        loadAndPlay(channel, command[1], false);

                }else if(enumCommand == Commands.skip){
                    // Skip a song or video in the queue
                    skipTrack(channel);

                }else if(enumCommand == Commands.volume){
                    // Change volume
                    if(command.length == 2)
                        changeVolume(channel, command[1]);

                }else if(enumCommand == Commands.list){
                    // List the songs/commands in the queue
                    if(command.length == 2){
                        if(command[1].equals("queue")){
                            listTracks(channel);
                        }
                        else if(command[1].equals("sounds")){
                            localManager.ListSounds(channel);
                        }
                        else if(command[1].equals("users")) {
                            localManager.ListUserAudio(channel);
                        }
                    }

                }else if(enumCommand == Commands.pause){
                    // Pause the song/sound in the queue
                        pauseTrack(channel);
                }else if(enumCommand == Commands.unpause){
                    // Unpause the song/sound in the queue
                    unpauseTrack(channel);

                }else if(enumCommand == Commands.playingnow) {
                    // Print the currently playing song
                    printCurrentlyPlaying(channel, false);
                } else if(enumCommand == Commands.status) {
                    // Print currently playing song with extra info
                    printCurrentlyPlaying(channel, true);
                }else if(enumCommand == Commands.summon){
                    hasAudience = true;
                    connectToUserVoiceChannel(monitoredGuild.getAudioManager(), event.getMember().getEffectiveName());
                }else if(enumCommand == Commands.help){
                    help(channel);
                }else if(enumCommand == Commands.dropqueue) {
                    clearQueue();
                    channel.sendMessage("Queue has been cleared").queue();
                }

            }

        }

        super.onMessageReceived(event);
    }

    private void changeVolume(final MessageChannel channel, final String volume) {
        GuildMusicManager musicManager = getGuildAudioPlayer();
        musicManager.player.setVolume(Integer.parseInt(volume));
        channel.sendMessage("Volume now set to " + volume + "%").queue();
    }

    private void listTracks(final MessageChannel channel) {
        GuildMusicManager musicManager = getGuildAudioPlayer();
        List<String> queueContents = musicManager.scheduler.getQueueContents();
        String printMessage = "Tracks in the queue:\n";
        for (String track:
                queueContents) {
            printMessage = printMessage + track + "\n";
        }
        channel.sendMessage(printMessage).queue();
    }

    private void pauseTrack(final MessageChannel channel){
        GuildMusicManager musicManager = getGuildAudioPlayer();
        musicManager.player.setPaused(true);
        channel.sendMessage("Playback Paused.").queue();
    }

    private void unpauseTrack(final MessageChannel channel){
        GuildMusicManager musicManager = getGuildAudioPlayer();
        musicManager.player.setPaused(false);
        channel.sendMessage("Unpaused playback.").queue();
    }

    /**
     * Prints out information about what is currently playing and possibly information about the track and/or the audio volume
     * @param channel The channel to respond on
     * @param printStatus Print out extra information such as track information and/or audio volume
     */
    private void printCurrentlyPlaying(final MessageChannel channel, boolean printStatus){
        GuildMusicManager musicManager = getGuildAudioPlayer();
        AudioTrack currentlyPlaying = musicManager.player.getPlayingTrack();
        String message = "";
        if(currentlyPlaying != null) {
            message = "Currently Playing: " + currentlyPlaying.getInfo().title + " by " + currentlyPlaying.getInfo().author;
            if(printStatus) {
                message += "\n" + genTimeInformation(currentlyPlaying.getPosition(), currentlyPlaying.getDuration());
                if(musicManager.player.isPaused()) {
                    message += "\n**Paused**";
                } else {
                    message += "\n*Playing*";
                }
                message += ( "\nVolume = " + musicManager.player.getVolume() + "%");
            }
        } else {
            message = "Nothing currently playing";
            if(printStatus) {
                message += ("\nVolume = " + musicManager.player.getVolume() + "%");
            }
        }
        channel.sendMessage(message).queue();
    }

    private void loadAndPlay(final MessageChannel channel, final String trackUrl, boolean preempt) {
        GuildMusicManager musicManager = getGuildAudioPlayer();

        playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                int timeStart = trackUrl.lastIndexOf('=');
                if(timeStart != -1){
                    String timeString = trackUrl.substring(timeStart);

                    //The format will be 1h2m53s, need to parse that into seconds and then call
                    //track.setPosition(long position)

                }
                if(!preempt) {
                    helper.urlToTimeStamp(trackUrl);
                    channel.sendMessage("Adding to queue " + track.getInfo().title).queue();
                }

                play(monitoredGuild, musicManager, track, preempt);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack firstTrack = playlist.getSelectedTrack();

                if (firstTrack == null) {
                    firstTrack = playlist.getTracks().get(0);
                }

                channel.sendMessage("Adding to queue " + firstTrack.getInfo().title + " (first track of playlist " + playlist.getName() + ")").queue();

                play(monitoredGuild, musicManager, firstTrack, false);
            }

            @Override
            public void noMatches() {
                channel.sendMessage("Nothing found by " + trackUrl).queue();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                channel.sendMessage("Could not play: " + exception.getMessage()).queue();
            }
        });
    }

    private void play(Guild guild, GuildMusicManager musicManager, AudioTrack track, boolean preempt) {
        connectToFollowingVoiceChannel(guild.getAudioManager());

        if(!preempt)
            musicManager.scheduler.queue(track);
        else
            musicManager.scheduler.playNow(track);
    }

    private void skipTrack(MessageChannel channel) {
        GuildMusicManager musicManager = getGuildAudioPlayer();
        musicManager.scheduler.nextTrack();

        channel.sendMessage("Skipped to next track.").queue();
    }

    private void help(final MessageChannel channel) {
        String printMessage = "List of commands:\n ```" +
                "!<sound>     - plays a sound\n" +
                "~play <URL>  - plays audio from link\n" +
                "~volume #    - sets the volume to the number as a percentage \n" +
                "~pause       - pauses the playing audio\n" +
                "~unpause     - unpauses the audio\n" +
                "~skip        - skips to the next song in queue\n" +
                "~list queue  - prints out the names of the songs in the queue\n" +
                "~list sounds - prints out the names of the sounds available\n" +
                "~list users  - prints out users with audio that will play when they join the voice channel\n" +
                "~playingnow  - prints out the name of the currently playing song\n" +
                "~status      - prints out status about the currently playing song\n" +
                "~summon      - brings SoundChan to the voice channel of the summoner\n" +
                "~help        - prints out this help message ```";
        channel.sendMessage(printMessage).queue();
    }


    private static void connectToFollowingVoiceChannel(AudioManager audioManager) {
        if(!audioManager.isConnected()) {
            connectToUserVoiceChannel(audioManager, followingUser);
        }
    }

    private static void connectToUserVoiceChannel(AudioManager audioManager, String user) {
        if (!audioManager.isAttemptingToConnect()) {
            for (VoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels()) {
                for (int i = 0; i < voiceChannel.getMembers().size(); i++) {
                    if (voiceChannel.getMembers().get(i).getEffectiveName().compareTo(user) == 0) {
                        audioManager.openAudioConnection(voiceChannel);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Creates a block of time information with a progressbar
     * @param currentMillis Current position in the audio in milliseconds
     * @param durationMillis Length of audio in milliseconds
     * @return Time information block
     */
    private static String genTimeInformation(long currentMillis, long durationMillis) {
        String message = "|";
        double temp = ((double) currentMillis / (double) durationMillis);
        int fill = (int) (temp * 10.0);
        for(int i = 0; i < fill - 1; i++) {
            message += "--";
        }
        message += "<>";
        for(int i = fill; i < 10; i++) {
            message += "--";
        }
        message += "|\nTime : " + genTimeStamp(currentMillis) + " / " + genTimeStamp(durationMillis);
        return message;
    }

    /**
     * Creates a timestamp string from a number of milliseconds
     * @param durationInMillis Number of milliseconds to turn into timestamp
     * @return Timestamp in form HH:MM:ss
     */
    private static String genTimeStamp(long durationInMillis) {
        long second = (durationInMillis / 1000) % 60;
        long minute = (durationInMillis / (1000 * 60)) % 60;
        long hour = (durationInMillis / (1000 * 60 * 60)) % 24;
        return String.format("%02d:%02d:%02d", hour, minute, second);
    }

    /**
     * Checks the string for some reason to enable/disable a setting.
     * @param value A string (probably read in from config file)
     * @return True if it matches a value to enable, False otherwise
     */
    private static boolean settingEnableCheck(String value) {
        if(value == null)
            return false;
        value = value.toLowerCase();
        if(value.contentEquals("true") || value.contentEquals("1") ||
                value.contentEquals("yes") || value.contentEquals("on") ||
                value.contentEquals("enable"))
            return true;
        else
            return false;
    }

    /**
     * Adds a new MediaWatcher to the list of running tasks
     * @param listener Listener that will get callback during watching of media
     * @param filepath Path to either directory or file
     * @param taskName Thing to name task as
     * @param watchSubDirs Also watch any subdirectories in the given directory (doesn't do anything if watching a file)
     */
    private void addWatcherTask(@NotNull MediaWatcherListener listener, String filepath, String taskName, boolean watchSubDirs) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        MediaWatcher watcher = new MediaWatcher(listener, filepath, watchSubDirs);
        otherTasks.put(taskName, executorService.submit(watcher));
        executorService.shutdown();
    }

    /**
     * Empty out the queue of things to play and stops currently playing audio
     */
    private void clearQueue() {
        getGuildAudioPlayer().scheduler.emptyQueue();
    }

}
