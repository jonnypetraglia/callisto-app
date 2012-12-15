package jerklib.parsers;

import jerklib.events.IRCEvent;
import jerklib.events.PartEvent;

/**
 * @author mohadib
 *
 */
public class PartParser implements CommandParser
{
	public PartEvent createEvent(IRCEvent event)
	{
			return new PartEvent
			(
					event.getRawEventData(), 
					event.getSession(),
					event.getNick(), // who
					event.getSession().getChannel(event.arg(0)), 
					event.args().size() == 2? event.arg(1) : ""
			);
	}
}
