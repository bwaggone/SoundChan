package soundchan;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.requests.GatewayIntent;
import soundchan.BotListener.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static net.dv8tion.jda.api.JDABuilder.createDefault;

public class Main {
  public static void main(String[] args) throws Exception {

    Properties properties = LoadProperties();

    JDA jda = createDefault(properties.getProperty("botToken"))
        .enableIntents(GatewayIntent.GUILD_MESSAGES)
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
