package jerklib.events;

import jerklib.Session;
import jerklib.events.IRCEvent;

/**
 * 
 * Event fired for server version information
 * @author mohadib
 */
public class ServerVersionEvent extends IRCEvent
{
	private final String comment, hostName, version, debugLevel;

	public ServerVersionEvent
	(
		String comment, 
		String hostName, 
		String version, 
		String debugLevel, 
		String rawEventData, 
		Session session
	)
	{
		super(rawEventData, session, Type.SERVER_VERSION_EVENT);
		this.comment = comment;
		this.hostName = hostName;
		this.version = version;
		this.debugLevel = debugLevel;
	}

  /**
   * Gets the server version comment
   *
   * @return comment
   */
	public String getComment()
	{
		return comment;
	}

  /**
   * Gets the host name
   *
   * @return hostname
   */
	public String getHostName()
	{
		return hostName;
	}

  /**
   * Gets the version string the server sent
   *
   * @return version string
   */
	public String getVersion()
	{
		return version;
	}

  /**
   * Not impled
   *
   * @return Not impled
   */
	public String getdebugLevel()
	{
		return debugLevel;
	}

}
