package soundchan.BotListener;

import java.nio.file.WatchEvent;

public interface MediaWatcherListener {

    void runTask(WatchEvent event);
}
