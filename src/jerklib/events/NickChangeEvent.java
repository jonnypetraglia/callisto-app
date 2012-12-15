package jerklib.events;

import jerklib.Session;
import jerklib.events.IRCEvent;

/**
 * NickChangeIRCEvent is created when someone in a channel changes their nick
 *
 * @author mohadib
 */
public class NickChangeEvent extends IRCEvent
{

	private final String oldNick, newNick;

	public NickChangeEvent
	(
		String rawEventData, 
		Session session, 
		String oldNick, 
		String newNick
	)
	{
		super(rawEventData, session,Type.NICK_CHANGE);
		this.oldNick = oldNick;
		this.newNick = newNick;
	}

  /**
   * Returns the previous nick of the user before the change
   *
   * @return Old nick for user.
   */
	public final String getOldNick()
	{
		return oldNick;
	}
	
  /**
   * getNewNick() returns the new nick of the user
   *
   * @return New nick for user
   */
	public final String getNewNick()
	{
		return newNick;
	}
}
