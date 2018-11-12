package soundchan.BotListener;

import soundchan.LocalAudioManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import static java.lang.Thread.sleep;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class MediaWatcher implements Runnable {

    private LocalAudioManager localAudioManager;
    private String mediaFilename;
    private Path mediaDir;
    private WatchService watchService;
    private WatchKey watchKey;
    private boolean isDirectory;

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    /**
     * Creates a MediaWatcher, which monitors changes to files either within a directory or for a specific file.
     * If the given filepath is name of a directory, it is assumed that we want to monitor changes to the sound files.
     * If the given filepath is a single file, it is assumed that we want to monitor changes to the userSoundFile.
     * @param audioManager AudioManager for this bot instance
     * @param filepath Path to either directory or specific file
     */
    public MediaWatcher(LocalAudioManager audioManager, String filepath) {
        this.localAudioManager = audioManager;
        File mediaFile = new File(filepath);
        this.mediaFilename = mediaFile.getName();
        if(mediaFile.isFile()) {
            try {
                this.mediaDir = mediaFile.getCanonicalFile().getParentFile().toPath();
            } catch (IOException e) {
                System.out.println("Error getting parent path of " + mediaFilename);
            }
            isDirectory = false;
        } else if(mediaFile.isDirectory()) {
            this.mediaDir = mediaFile.toPath();
            isDirectory = true;
        }
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            this.watchKey = mediaDir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        } catch (IOException e) {
            System.out.println("Error setting up watcher for " + filepath);
        }
    }

    /**
     * Called by an executor, checks for changes to file(s)
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
                    if(isDirectory) {
                        localAudioManager.UpdateFiles();
                    } else {
                        if(pathEvent.context().endsWith(mediaFilename)) {
                            localAudioManager.UpdateUserAudio();
                        }
                    }
                }

                if(!key.reset()) {
                    break;
                }

                sleep(5000);
            }
        } catch(InterruptedException e) {
            return;
        }
    }
}
