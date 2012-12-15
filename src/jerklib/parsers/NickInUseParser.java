package jerklib.parsers;

import jerklib.events.IRCEvent;
import jerklib.events.NickInUseEvent;

public class NickInUseParser implements CommandParser
{
	public IRCEvent createEvent(IRCEvent event)
	{
		return new NickInUseEvent
		(
				event.arg(1),
				event.getRawEventData(), 
				event.getSession()
		); 
	}
}
