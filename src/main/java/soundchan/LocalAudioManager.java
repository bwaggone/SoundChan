package soundchan;

import net.dv8tion.jda.core.entities.MessageChannel;

import java.io.File;
import java.util.*;

public class LocalAudioManager {

    /*
      A list of local files for the sound bot to play.
     */

    public Map<String, String> filenameDict;
    private String filepath;

    public LocalAudioManager(String filepath_in){
        filepath = filepath_in;
        filenameDict = new HashMap<>();
        PopulateFiles();
    }

    public String GetFilePath(String command){
        try{
            return filepath + "/" + filenameDict.get(command);
        }catch(Exception ex){
            System.out.println("File " + command + " not found!");
        }
        return "";
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

    private void PopulateFiles(){
        File folder = new File(filepath);
        File[] listOfFiles = folder.listFiles();

        for (File file : listOfFiles) {
            if (file.isFile()) {
                String filename = file.getName();
                filenameDict.put(filename.substring(0, filename.indexOf('.')), filename);
            }
        }
    }


}
