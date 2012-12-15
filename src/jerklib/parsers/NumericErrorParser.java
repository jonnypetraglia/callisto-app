package jerklib.parsers;

import jerklib.events.IRCEvent;
import jerklib.events.NumericErrorEvent;

public class NumericErrorParser implements CommandParser
{
	public IRCEvent createEvent(IRCEvent event)
	{
		return new NumericErrorEvent
		(
				event.arg(0), 
				event.getRawEventData(), 
				event.getSession()
		); 
	}
}
