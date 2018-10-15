package soundchan;

import net.dv8tion.jda.core.entities.MessageChannel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class LocalAudioManager {

    /*
      A list of local files for the sound bot to play.
     */

    public Map<String, String> filenameDict;
    public Map<String, String> usernameDict;
    private String filepath;

    public LocalAudioManager(String filepath_in){
        filepath = filepath_in;
        filenameDict = PopulateFiles();
    }

    /**
     * Constructor for when there is a file listing users and sounds to play for them
     * @param filepath_in Path to folder where sounds are located
     * @param userSoundFile Path to file with users and sounds
     */
    public LocalAudioManager(String filepath_in, String userSoundFile) {
        filepath = filepath_in;
        filenameDict = PopulateFiles();
        usernameDict = new HashMap<>();
        MapUserAudio(userSoundFile);
    }

    /**
     * Gives filepath to sound to play either from a command or when given a username
     * @param command Command or username to play soundbite
     * @return Path to sound, or "" if no sound for given command
     */
    public String GetFilePath(String command){
        String path;
        try{
            path = filepath + "/" + filenameDict.get(command);
        }catch(Exception ex){
            System.out.println("File " + command + " not found!");
            path = "";
        }
        if(path.contentEquals("") || path.contentEquals(filepath + "/null")) {
            try {
                path = filepath + "/" + usernameDict.get(command);
            } catch (Exception ex) {
                System.out.println("File " + command + " not found!");
                path = "";
            }
        }
        return path;
    }

    public void ListSounds(MessageChannel channel){
        Set<String> localSounds = filenameDict.keySet();
        String toPrint = "The following sounds you can play are:\n```";
        for (String sound:
                localSounds) {
            toPrint = toPrint + " * " + sound + "\n";
        }
        toPrint = toPrint + "```";
        channel.sendMessage(toPrint).queue();
    }

    /**
     * Lists users with sounds that will play when they join the voice channel
     * @param channel Text channel messaged on
     */
    public void ListUserAudio(MessageChannel channel) {
        Set<String> userSounds = usernameDict.keySet();
        String toPrint = "The following users have sounds that will play when they join the voice channel:\n```";
        for (String user : userSounds) {
            String sound = usernameDict.get(user);
            toPrint =  toPrint + " * " + user + "\t" + sound.substring(0, sound.indexOf('.')) + "\n";
        }
        toPrint = toPrint + "```";
        channel.sendMessage(toPrint).queue();
    }

    /**
     * Updates the map of sound files
     */
    public void UpdateFiles() {
        filenameDict = PopulateFiles();
    }

    private Map<String, String> PopulateFiles(){
        File folder = new File(filepath);
        File[] listOfFiles = folder.listFiles();

        Map<String, String> fileDict = new HashMap<>();

        for (File file : listOfFiles) {
            if (file.isFile()) {
                String filename = file.getName();
                fileDict.put(filename.substring(0, filename.indexOf('.')), filename);
            }
        }
        return fileDict;
    }

    /**
     * Reads in users and their respective sounds from file, then builds a map of users to the filenames. This assumes
     * filenames for the sounds are valid, but doesn't check for them.
     * @param userSoundFile The file (with path if required) with listing of users and the sounds to play when they join
     */
    private void MapUserAudio(String userSoundFile) {
        Properties userSoundProp = LoadProperties(userSoundFile);
        Set<String> users = userSoundProp.stringPropertyNames();
        for(String user : users) {
            String soundFile = userSoundProp.getProperty(user);
            usernameDict.put(user, soundFile);
        }

    }

    /**
     * Builds a property object from a file.
     * @param filename File to be read
     * @return Property object with information from file
     */
    private static Properties LoadProperties(String filename){
        Properties properties = new Properties();
        InputStream input = null;
        File file = new File(filename);
        if(file.exists() && !file.isDirectory()) {
            try {
                input = new FileInputStream(filename);
                properties.load(input);
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                try {
                    input.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            return properties;
        } else {
            return properties;
        }
    }
}
