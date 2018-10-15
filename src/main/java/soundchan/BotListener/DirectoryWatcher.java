package soundchan.BotListener;

import soundchan.LocalAudioManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class DirectoryWatcher implements Runnable {

    private LocalAudioManager localAudioManager;
    private Path soundDir;
    private WatchService watchService;
    private WatchKey watchKey;

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    public DirectoryWatcher(LocalAudioManager audioManager, String filepath) {
        this.localAudioManager = audioManager;
        this.soundDir = new File(filepath).toPath();
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            this.watchKey = soundDir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        } catch(IOException e) {
            System.out.println("Error setting up watcher for " + filepath);
        }
    }

    /**
     * Called by an executor, checks for changes in the directory
     */
    public void run() {
        try {
            while(true) {
                WatchKey key = watchService.take();
                if(this.watchKey != key) {
                    System.out.println("Error with WatchKey");
                    continue;
                }

                for(WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent<Path> pathEvent = cast(event);
                    //System.out.format("%s: %s\n", pathEvent.kind(), soundDir.resolve(pathEvent.context()));
                    localAudioManager.UpdateFiles();
                }

                if(!key.reset()) {
                    break;
                }
            }
        } catch(InterruptedException e) {
            return;
        }
    }
}
