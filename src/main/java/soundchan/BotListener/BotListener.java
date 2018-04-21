package soundchan.BotListener;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.client.events.call.voice.CallVoiceJoinEvent;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.AudioManager;
import soundchan.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class BotListener extends ListenerAdapter{

    private long monitoredGuildId = -1;
    private Guild monitoredGuild;
    private static String followingUser;
    private static String localFilePath;
    private static LocalAudioManager localManager;
    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;
    private BotListenerHelpers helper = new BotListenerHelpers();

    public BotListener(Properties properties) {
        this.musicManagers = new HashMap<>();

        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);

        localFilePath = properties.getProperty("localFilePath");
        followingUser = properties.getProperty("followingUser");
        localManager = new LocalAudioManager(localFilePath);
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

    @Override
    public void onCallVoiceJoin(CallVoiceJoinEvent event){

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
    public void onMessageReceived(MessageReceivedEvent event) {
        String[] command = event.getMessage().getContentRaw().split(" ", 2);

        Guild guild = event.getGuild();
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
                    }

                }else if(enumCommand == Commands.pause){
                    // Pause the song/sound in the queue
                        pauseTrack(channel);
                }else if(enumCommand == Commands.unpause){
                    // Unpause the song/sound in the queue
                    unpauseTrack(channel);

                }else if(enumCommand == Commands.playingnow){
                    // Print the currently playing song
                    printCurrentlyPlaying(channel);
                }else if(enumCommand == Commands.summon){
                    connectToUserVoiceChannel(monitoredGuild.getAudioManager(), event.getMember().getEffectiveName());
                }else if(enumCommand == Commands.help){
                    help(channel);
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

    private void printCurrentlyPlaying(final MessageChannel channel){
        GuildMusicManager musicManager = getGuildAudioPlayer();
        AudioTrack currentlyPlaying = musicManager.player.getPlayingTrack();
        channel.sendMessage("Currently Playing: " + currentlyPlaying.getInfo().title + " by " + currentlyPlaying.getInfo().author).queue();

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
                "~playingnow  - prints out the name of the currently playing song\n" +
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

}