package jerklib.events;

import jerklib.Session;
import jerklib.events.IRCEvent;

/**
 * Event fired when whowas data received
 * @author mohadib
 */
public class WhowasEvent extends IRCEvent
{
	private final String hostName, userName, nick, realName;

	public WhowasEvent
	(
		String hostName, 
		String userName, 
		String nick, 
		String realName, 
		String rawEventData, 
		Session session
	)
	{
		super(rawEventData, session, Type.WHOWAS_EVENT);
		this.hostName = hostName;
		this.userName = userName;
		this.nick = nick;
		this.realName = realName;
	}

  /**
   * get hostname of whoised user
   *
   * @return hostname
   */
	public String getHostName()
	{
		return hostName;
	}

  /**
   * get nick who was event is about
   *
   * @return nick who was event is about
   */
	public String getNick()
	{
		return nick;
	}

  /**
   * get users realname
   *
   * @return realname
   */
	public String getRealName()
	{
		return realName;
	}

  /**
   * get username
   *
   * @return username
   */
	public String getUserName()
	{
		return userName;
	}
}
