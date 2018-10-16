package soundchan.BotListener;

import soundchan.LocalAudioManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class UserSoundWatcher implements Runnable {

    private LocalAudioManager localAudioManager;
    private String userSoundFile;
    private Path soundFileDirectory;
    private WatchService watchService;
    private WatchKey watchKey;

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    public UserSoundWatcher(LocalAudioManager audioManager, String filepath) {
        this.localAudioManager = audioManager;
        File soundFile = new File(filepath);
        this.userSoundFile = soundFile.getName();
        try {
            this.soundFileDirectory = soundFile.getCanonicalFile().getParentFile().toPath();
        } catch(IOException e) {
            System.out.println("Error getting parent path of " + userSoundFile);
        }
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            this.watchKey = soundFileDirectory.register(watchService, ENTRY_MODIFY);
        } catch(IOException e) {
            System.out.println(e.getMessage());
            System.out.println("Error setting up watcher for " + filepath);
        }
    }

    /**
     * Called by an executor, checks for changes of the userSoundFile
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
                    if(pathEvent.context().endsWith(userSoundFile)) {
                        localAudioManager.UpdateUserAudio();
                    }
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
