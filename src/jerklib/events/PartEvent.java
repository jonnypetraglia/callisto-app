package jerklib.events;

import jerklib.Channel;
import jerklib.Session;
import jerklib.events.IRCEvent;

/**
 * PartIRCEvent is made when someone parts a channel
 *
 * @author mohadib
 */
public class PartEvent extends IRCEvent
{

	private final String who, partMessage;
	private final Channel channel;

	public PartEvent(String rawEventData, Session session, String who, Channel channel, String partMessage)
	{
		super(rawEventData, session, Type.PART);
		this.channel = channel;
		this.who = who;
		this.partMessage = partMessage;
	}

  /**
   * returns the nick of who parted
   *
   * @return nick of parted
   */
	public final String getNick()
	{
		return who;
	}

  /**
   * returns the name of the channel parted
   *
   * @return name of channel parted
   */
	public final String getChannelName()
	{
		return channel.getName();
	}

  /**
   * returns IRCChannel object for channel parted
   *
   * @return Channel object parted
   * @see Channel
   */
	public final Channel getChannel()
	{
		return channel;
	}

  /**
   * returns part message if there is one
   *
   * @return part message
   */
	public final String getPartMessage()
	{
		return this.partMessage;
	}

}
