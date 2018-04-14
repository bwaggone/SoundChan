package soundchan;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class BotListenerHelpers {

    /**
     * Given a MessageReceivedEvent, determines the correct reply channel for SoundChan.
     * @param event a MessageReceivedEvent from the JDA ListenerAdapter
     * @return Either a PrivateChannel (if SoundChan was DM'd) or a TextChannel (If SoundChan was commanded in one)
     */
    public MessageChannel GetReplyChannel(MessageReceivedEvent event){
        Guild guild = event.getGuild();
        return (guild == null) ? event.getPrivateChannel() : event.getTextChannel();
    }


}
