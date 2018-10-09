package soundchan;

import com.sun.istack.internal.NotNull;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import soundchan.BotListener.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.apache.commons.cli.*;

public class Main {

  private static String defaultPropertiesFile = "soundchan.properties";
  private static String versionString = "alpha-0.9";

  public static void main(String[] args) throws Exception {

    // Command Line parsing
    CommandLine cmd = genCommandLine(args);

    if(cmd.hasOption("version")) {
      System.out.println("SoundChan version: " + versionString);
      System.exit(0);
    }
    if(cmd.hasOption("help")) {
      HelpFormatter helpFormatter = new HelpFormatter();
      Options options = new Options();
      for(Option option : buildOptionList()) {
        options.addOption(option);
      }
      helpFormatter.printHelp("SoundChan", options);
      System.exit(0);
    }

    String propertiesFile = cmd.getOptionValue("properties");
    if(propertiesFile == null || propertiesFile.contentEquals("")) {
      propertiesFile = defaultPropertiesFile;
    }

    // SoundChan setup
    Properties properties = LoadProperties(propertiesFile);

    JDA jda = new JDABuilder(AccountType.BOT)
        .setToken(properties.getProperty("botToken"))
        .buildBlocking();


    jda.addEventListener(new BotListener(properties));
  }

  /**
   * Builds a CommandLine that can be used to get command line arguments
   * @param args Array of command line arguments
   * @return A CommandLine that has properties that have been parsed
   */
  private static CommandLine genCommandLine(String[] args) {
    Options options = new Options();
    for(Option option : buildOptionList()) {
      options.addOption(option);
    }

    CommandLineParser cmdParser = new DefaultParser();
    HelpFormatter helpFormatter = new HelpFormatter();
    CommandLine cmd = null;

    try {
      cmd = cmdParser.parse(options, args);
    } catch (ParseException e) {
      System.out.println(e.getMessage());
      helpFormatter.printHelp("SoundChan", options);
      System.exit(1);
    }

    return cmd;
  }

  /**
   * Builds a list of command line options for a CommandLine parser
   * @return ArrayList of command line options, in no particular order
   */
  private static ArrayList<Option> buildOptionList() {
    ArrayList<Option> optionList = new ArrayList<>();

    Option altPropertiesFile = new Option("p", "properties", true, "properties file path -- defaults to soundchan.properties");
    altPropertiesFile.setRequired(false);
    Option helpFlag = new Option("h", "help", false, "prints help information");
    helpFlag.setRequired(false);
    Option versionFlag = new Option("v", "version", false, "prints out version information");
    versionFlag.setRequired(false);

    optionList.add(altPropertiesFile);
    optionList.add(helpFlag);
    optionList.add(versionFlag);

    return optionList;
  }

  /**
   * Builds a Properties object from a given properties file
   * @param filePath Path to properties file
   * @return A Properties object
   */
  private static Properties LoadProperties(@NotNull String filePath){
    Properties properties = new Properties();
    InputStream input = null;
    try{
      input = new FileInputStream(filePath);
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

}
