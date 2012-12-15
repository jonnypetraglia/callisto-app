package jerklib.events;

import jerklib.Channel;
import jerklib.Session;
import jerklib.events.IRCEvent;

/**
 * NoticeIRCEvent - the event for notices from the server
 *
 * @author mohadib
 */
public class NoticeEvent extends IRCEvent
{

	private final String message, toWho, byWho;
	private final Channel channel;

	public NoticeEvent(String rawEventData, Session session, String message, String toWho, String byWho, Channel channel)
	{
		super(rawEventData, session, Type.NOTICE);
		this.message = message;
		this.toWho = toWho;
		this.byWho = byWho;
		this.channel = channel;
	}

  /**
   * returns notice message
   *
   * @return notice message
   */
	public String getNoticeMessage()
	{
		return message;
	}

  /**
   * Gets who sent the notice event
   *
   * @return who
   */
	public String byWho()
	{
		return byWho;
	}

  /**
   * If this is a Channel notice this will return the Channel
   *
   * @return Channel
   * @see Channel
   */
	public Channel getChannel()
	{
		return channel;
	}

  /**
   * If this notice is sent to a user this will return who
   *
   * @return who
   */
	public String toWho()
	{
		return toWho;
	}

}
