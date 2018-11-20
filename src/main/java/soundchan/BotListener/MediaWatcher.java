package soundchan.BotListener;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

import static java.lang.Thread.sleep;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class MediaWatcher implements Runnable {

    private MediaWatcherListener listener;
    private String mediaFilename;
    private Path mediaDir;
    private WatchService watchService;
    private WatchKey watchKey;
    private ArrayList<WatchKey> subDirKeys;
    private boolean isDirectory;
    private int sleepTime = 5000;

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    /**
     * Creates a MediaWatcher, which monitors changes to files either within a directory or for a specific file.
     * Defaults to scanning every 5 seconds.
     * @param listener Object that will get a callback when there is a watch event
     * @param filepath Path to either directory or specific file
     * @param watchSubDirs Also watch any subdirectories in the given directory (doesn't do anything if watching a file)
     */
    public MediaWatcher(MediaWatcherListener listener, String filepath, boolean watchSubDirs) {
        this.listener = listener;
        startWatchService(filepath, watchSubDirs);
    }

    /**
     * Creates a MediaWatcher, which monitors changes to files either within a directory or for a specific file.
     * @param listener Object that will get a callback when there is a watch event
     * @param filepath Path to either directory or specific file
     * @param watchSubDirs Also watch any subdirectories in the given directory (doesn't do anything if watching a file)
     * @param sleepTime How long to put the scanner thread to sleep between rescans (time in milliseconds)
     */
    public MediaWatcher(MediaWatcherListener listener, String filepath, boolean watchSubDirs, int sleepTime) {
        this.listener = listener;
        this.sleepTime = sleepTime;
        startWatchService(filepath, watchSubDirs);
    }

    /**
     * Sets up watch service for the file/directory
     * @param filepath Path to file or directory to be scanned
     * @param watchSubDirs Also watch any subdirectories in the given directory (doesn't do anything if watching a file)
     */
    private void startWatchService(String filepath, boolean watchSubDirs) {
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
            subDirKeys = new ArrayList<>();
        }
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            this.watchKey = mediaDir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            if(isDirectory && watchSubDirs) {
                Files.walkFileTree(mediaDir, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        try {
                            WatchKey temp = dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                            subDirKeys.add(temp);
                            return FileVisitResult.CONTINUE;
                        } catch (IOException e) {
                            System.out.println("Error setting up watch service with sub dirs in " + filepath);
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                    }
                });
            }
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
                WatchKey key = watchService.take(); // Wait for an event to happen

                // Check this event happened in a place we are monitoring, otherwise ignore it
                if(!isDirectory && this.watchKey != key) {
                    System.out.println("Error with WatchKey");
                    continue;
                } else if(isDirectory) {
                    if(this.watchKey != key) { // Our event doesn't happen in the root of the sounds directory
                        boolean noKeyMatch = true;
                        for(WatchKey subKey : subDirKeys) { // Check if it happened in on of the sub directories
                            if(subKey == key) {
                                noKeyMatch = false;
                                break;
                            }
                        }
                        if(noKeyMatch) {
                            System.out.println("Error with WatchKey");
                            continue;
                        }
                    }
                }

                for(WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent<Path> pathEvent = cast(event);
                    if(isDirectory) {
                        listener.onWatchEvent(event);
                    } else {
                        if(pathEvent.context().endsWith(mediaFilename)) {
                            listener.onWatchEvent(event);
                        }
                    }
                }

                if(!key.reset()) {
                    break;
                }

                sleep(sleepTime);
            }
        } catch(InterruptedException e) {
            return;
        }
    }
}
