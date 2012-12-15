package jerklib.parsers;

import jerklib.events.IRCEvent;
import jerklib.events.WhowasEvent;

public class WhoWasParser implements CommandParser
{
	
	/* :kubrick.freenode.net 314 scripy1 ty n=ty 71.237.206.180 * :ty
	 "<nick> <user> <host> * :<real name>" */
	public IRCEvent createEvent(IRCEvent event)
	{
		return new WhowasEvent
		(
				event.arg(3), 
				event.arg(2), 
				event.arg(1), 
				event.arg(5), 
				event.getRawEventData(), 
				event.getSession()
		); 
	}
}
