package jerklib.parsers;

import jerklib.Session;
import jerklib.events.IRCEvent;
import jerklib.events.NickChangeEvent;

public class NickParser implements CommandParser
{
	public IRCEvent createEvent(IRCEvent event)
	{
		Session session = event.getSession();
		return new NickChangeEvent
		(
				event.getRawEventData(), 
				session, 
				event.getNick(), // old
				event.arg(0)// new nick
		); 
	}
}
