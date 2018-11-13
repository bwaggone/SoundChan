package soundchan.BotListener;

import java.nio.file.WatchEvent;

public interface MediaWatcherListener {

    /**
     * Called by MediaWatcher when there is a file event. Any created, deleted, or modified events will be passed for use.
     * @param event The type of event that triggered the update.
     */
    void onWatchEvent(WatchEvent event);
}
