package soundchan;

import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import soundchan.BotListener.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class Main {
  public static void main(String[] args) throws Exception {

    Properties properties = LoadProperties();

    JDA jda = new JDABuilder(AccountType.BOT)
        .setToken(properties.getProperty("botToken"))
        .addEventListeners(new BotListener(properties))
        .build();
    jda.awaitReady();
  }

  private static Properties LoadProperties(){
    Properties properties = new Properties();
    InputStream input = null;
    try{
      input = new FileInputStream("soundchan.properties");
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
