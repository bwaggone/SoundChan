package soundchan;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class schedules tracks for the audio player. It contains the queue of tracks.
 */
public class TrackScheduler extends AudioEventAdapter {
  private final AudioPlayer player;
  private final BlockingDeque<AudioTrack> queue;

  /**
   * @param player The audio player this scheduler uses
   */
  public TrackScheduler(AudioPlayer player) {
    this.player = player;
    this.queue = new LinkedBlockingDeque<>();
  }

  /**
   * Add the next track to queue or play right away if nothing is in the queue.
   *
   * @param track The track to play or add to queue.
   */
  public void queue(AudioTrack track) {
    // Calling startTrack with the noInterrupt set to true will start the track only if nothing is currently playing. If
    // something is playing, it returns false and does nothing. In that case the player was already playing so this
    // track goes to the queue instead.
    if (!player.startTrack(track, true)) {
      queue.offer(track);
    }
  }

  public void playNow(AudioTrack track) {
    AudioTrack currenlyPlaying = player.getPlayingTrack();
    // If something is currently playing, pause it and put it back in the queue
    if(currenlyPlaying != null) {
      AudioTrack cloned = currenlyPlaying.makeClone();
      cloned.setPosition(currenlyPlaying.getPosition());

      // Don't re-enqueue if its just a soundclip
      if (!(currenlyPlaying.getInfo().uri.contains(".mp3") || currenlyPlaying.getInfo().uri.contains(".wav")))
        // Re-enqueue the track
        queue.addFirst(cloned);
    }
    player.startTrack(track, false);

  }

  public List<String> getQueueContents() {
    // Returns a list of the tracks in the queue
    Object[] queueInfo = queue.toArray();
    List<String> tracks = new ArrayList<>();
    for (Object item:
         queueInfo) {
      tracks.add(((DelegatedAudioTrack) item).getInfo().title);
    }
    return tracks;
  }

  /**
   * Start the next track, stopping the current one if it is playing.
   */
  public void nextTrack() {
    // Start the next track, regardless of if something is already playing or not. In case queue was empty, we are
    // giving null to startTrack, which is a valid argument and will simply stop the player.
    player.startTrack(queue.poll(), false);
  }

  /**
   * Cleans out the queue of tracks and stops any playing track
   */
  public void emptyQueue() {
    player.stopTrack();
    queue.clear();
  }

  @Override
  public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
    // Only start the next track if the end reason is suitable for it (FINISHED or LOAD_FAILED)
    if (endReason.mayStartNext) {
      nextTrack();
    }
  }
}
