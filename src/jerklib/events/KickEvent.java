package jerklib.events;

import jerklib.Channel;
import jerklib.Session;
import jerklib.events.IRCEvent;


/**
 * Event fired when someone is kicked from a channel
 * @author mohadib
 * @see Channel#kick(String, String)
 */
public class KickEvent extends IRCEvent
{

	private final String byWho, who, message;
	private final Channel channel;

	public KickEvent
	(
		String rawEventData, 
		Session session, 
		String byWho, 
		String who, 
		String message, 
		Channel channel
	)
	{
		super(rawEventData,session,Type.KICK_EVENT);
		this.byWho = byWho;
		this.who = who;
		this.message = message;
		this.channel = channel;
	}

  /**
   * Gets the nick of the user who
   * did the kicking
   *
   * @return nick
   */
	public String byWho()
	{
		return byWho;
	}

  /**
   * Gets the nick of who was kicked
   *
   * @return who was kicked
   */
	public String getWho()
	{
		return who;
	}

  /**
   * Gets the kick message
   *
   * @return message
   */
	public String getMessage()
	{
		return message;
	}

  /**
   * Gets the channel object someone was kicked from
   *
   * @return The Channel
   */
	public Channel getChannel()
	{
		return channel;
	}
}
