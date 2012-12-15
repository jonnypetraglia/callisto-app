package jerklib.events;

import jerklib.Session;
import jerklib.events.IRCEvent;

/**
 * Event fired when an Invite message is recieved from server
 * 
 * @author <a href="mailto:robby.oconnor@gmail.com">Robert O'Connor</a>
 */
public class InviteEvent extends IRCEvent
{
	private final String channelName;

	public InviteEvent(String channelName, String rawEventData, Session session)
	{
		super(rawEventData, session, Type.INVITE_EVENT);
		this.channelName = channelName;
	}

	/**
	 * Gets the channel to which we were invited to
	 * 
	 * @return the channel we were invited to.
	 */
	public String getChannelName()
	{
		return channelName;
	}

}
