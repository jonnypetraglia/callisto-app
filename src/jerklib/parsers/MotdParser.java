package jerklib.parsers;

import jerklib.events.IRCEvent;
import jerklib.events.MotdEvent;

public class MotdParser implements CommandParser
{
	public IRCEvent createEvent(IRCEvent event)
	{
		return new MotdEvent
		(
			event.getRawEventData(), 
			event.getSession(), 
			event.arg(1), 
			event.prefix()
		);
	}
}
