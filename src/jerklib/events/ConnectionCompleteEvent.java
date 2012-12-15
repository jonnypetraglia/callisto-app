package jerklib.events;

import jerklib.Session;
import jerklib.events.IRCEvent;

/**
 * Event made when connected to the server
 * This event contains the real server name. Example. When connection
 * to 'irc.freenode.net' we might actually connect to kornbluf.freenode.net
 * or some other host. This event will have the real hosts name.
 * <p/>
 * After receiving this event a Session is ready to join channels
 *
 * @author mohadib
 */
public class ConnectionCompleteEvent extends IRCEvent
{

	private final String hostName, oldHostName;

	public ConnectionCompleteEvent(String rawEventData, String hostName, Session session, String oldHostName)
	{
		super(rawEventData,session,Type.CONNECT_COMPLETE);
		this.hostName = hostName;
		this.oldHostName = oldHostName;
	}

  /**
   * Get the hostname used for the requested connection
   * @return old host name
   */
	public String getOldHostName()
	{
		return oldHostName;
	}

  /**
   * Gets the actual hostname
   * @return actual host name
   */
	public String getActualHostName()
	{
		return hostName;
	}

}
