package jerklib.events;

import jerklib.Channel;
import jerklib.Session;
import jerklib.events.IRCEvent;

/**
 * JoinIRCEvent is the event that will be dispatched when someone joins a channel
 *
 * @author mohadib
 */
public class JoinEvent extends IRCEvent
{

	private final String channelName;
	private final Channel chan;

	public JoinEvent
	(
		String rawEventData, 
		Session session, 
		Channel chan
	)
	{
		super(rawEventData,session,Type.JOIN);
		this.channelName = chan.getName();
		this.chan = chan;
	}

  /**
   * returns the name of the channel joined to cause this event
   *
   * @return Name of channel
   */
	public final String getChannelName()
	{
		return channelName;
	}

  /**
   * returns the Channel object joined
   *
   * @return The Channel object
   * @see Channel
   */
	public final Channel getChannel()
	{
		return chan;
	}
}
