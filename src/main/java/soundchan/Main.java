package soundchan;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.client.events.call.voice.CallVoiceJoinEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.AudioManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class Main extends ListenerAdapter {
  public static void main(String[] args) throws Exception {

    Properties properties = LoadProperties();

    JDA jda = new JDABuilder(AccountType.BOT)
        .setToken(properties.getProperty("botToken"))
        .buildBlocking();

    jda.addEventListener(new Main());
  }

  private static Properties LoadProperties(){
    Properties properties = new Properties();
    InputStream input = null;
      try{
        input = new FileInputStream("soundchan.properties");
        properties.load(input);

      }catch (IOException ex){
        ex.printStackTrace();
      } finally {
        try {
          input.close();
        } catch (IOException ex) {
          ex.printStackTrace();
        }
      }
    return properties;
  }

  private long monitoredGuildId = -1;
  private Guild monitoredGuild;
  private final AudioPlayerManager playerManager;
  private final Map<Long, GuildMusicManager> musicManagers;

  private Main() {
    this.musicManagers = new HashMap<>();

    this.playerManager = new DefaultAudioPlayerManager();
    AudioSourceManagers.registerRemoteSources(playerManager);
    AudioSourceManagers.registerLocalSource(playerManager);
  }

  private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
    long guildId = Long.parseLong(guild.getId());
    GuildMusicManager musicManager = musicManagers.get(guildId);

    if (musicManager == null) {
      musicManager = new GuildMusicManager(playerManager);
      musicManagers.put(guildId, musicManager);
    }

    guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

    return musicManager;
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
  public void onMessageReceived(MessageReceivedEvent event) {
    String[] command = event.getMessage().getContentRaw().split(" ", 2);
    Guild guild = event.getGuild();
    MessageChannel channel = null;

    // This means SoundChan was DM'd
    if (guild == null){
      channel = event.getPrivateChannel();
    }else{
      // This means SoundChan was referred to in a TextChannel
      channel = event.getTextChannel();
    }

    if(monitoredGuildId == -1 && guild != null){
      monitoredGuildId = Long.parseLong(guild.getId());
      monitoredGuild = guild;
    }

    if(monitoredGuild != null){
    if ("~play".equals(command[0]) && command.length == 2) {
        loadAndPlay(channel, command[1]);
    } else if ("~skip".equals(command[0])) {
        skipTrack(channel);
    } else if ("~volume".equals(command[0]) && command.length == 2) {
        changeVolume(channel, command[1]);
    } else if ("~pause".equals(command[0])) {
        pauseTrack(channel);
    } else if ("~unpause".equals(command[0])) {
        unpauseTrack(channel);
    } else if ("~list".equals(command[0])) {
      listTracks(channel);
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

  private void loadAndPlay(final MessageChannel channel, final String trackUrl) {
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
        channel.sendMessage("Adding to queue " + track.getInfo().title).queue();

        play(monitoredGuild, musicManager, track);
      }

      @Override
      public void playlistLoaded(AudioPlaylist playlist) {
        AudioTrack firstTrack = playlist.getSelectedTrack();

        if (firstTrack == null) {
          firstTrack = playlist.getTracks().get(0);
        }

        channel.sendMessage("Adding to queue " + firstTrack.getInfo().title + " (first track of playlist " + playlist.getName() + ")").queue();

        play(monitoredGuild, musicManager, firstTrack);
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

  private void play(Guild guild, GuildMusicManager musicManager, AudioTrack track) {
    connectToFirstVoiceChannel(guild.getAudioManager());

    musicManager.scheduler.queue(track);
  }

  private void skipTrack(MessageChannel channel) {
    GuildMusicManager musicManager = getGuildAudioPlayer();
    musicManager.scheduler.nextTrack();

    channel.sendMessage("Skipped to next track.").queue();
  }

  private static void connectToFirstVoiceChannel(AudioManager audioManager) {
    if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
      for (VoiceChannel voiceChannel : audioManager.getGuild().getVoiceChannels()) {
        audioManager.openAudioConnection(voiceChannel);
        break;
      }
    }
  }
}
