package soundchan.BotListener;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;


public class BotListenerHelpers {

    /**
     * Given a MessageReceivedEvent, determines the correct reply channel for SoundChan.
     * @param event a MessageReceivedEvent from the JDA ListenerAdapter
     * @return Either a PrivateChannel (if SoundChan was DM'd) or a TextChannel (If SoundChan was commanded in one)
     */
    public MessageChannel GetReplyChannel(MessageReceivedEvent event){
        Guild guild = null;
        if(event.isFromGuild()) {
            guild = event.getGuild();
        }
        return (guild == null) ? event.getPrivateChannel() : event.getTextChannel();
    }

    public long urlToTimeStamp(String url){

        int[] timeConversions = {1, 60, 3600};
        int position = url.indexOf("?t=");
        int seekTime = 0;

        if(position != -1){
            String linkTimestamp = url.substring(position + 3);
            String[] times = linkTimestamp.split("[hms]");
            for(int i = 0; i < times.length / 2; i++)
            {
                String temp = times[i];
                times[i] = times[times.length - i - 1];
                times[times.length - i - 1] = temp;
            }

            for (int i = 0; i < times.length; i++) {
                seekTime = seekTime + timeConversions[i]*Integer.parseInt(times[i]);
            }
            System.out.println(seekTime);

        }
        return seekTime;
    }


}
