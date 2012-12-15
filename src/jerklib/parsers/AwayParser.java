package jerklib.parsers;

import jerklib.events.AwayEvent;
import jerklib.events.IRCEvent;
import jerklib.events.AwayEvent.EventType;

public class AwayParser implements CommandParser
{
	public IRCEvent createEvent(IRCEvent event)
	{

		/*
		 * :swiftco.wa.us.dal.net 306 mohadib__ :You have been marked as being away 
		 * :swiftco.wa.us.dal.net 305 mohadib__ :You are no longer marked as being away 
		 * :card.freenode.net 301 r0bby_ r0bby :foo
		 * :calvino.freenode.net 301 jetirc1 jetirc :gone 
		 * :jetirc!jetirc@745d63.host 301 jetirc1 :gone for now
		 */

		switch (event.numeric())
		{
			case 305:
			{
				return new AwayEvent("", EventType.RETURNED_FROM_AWAY, false, true, event.arg(0), event.getRawEventData(), event.getSession());
			}
			case 306:
			{
				return new AwayEvent("", EventType.WENT_AWAY, true, true, event.arg(0), event.getRawEventData(), event.getSession());
			}
			default:
				return new AwayEvent(event.arg(event.args().size() - 1), AwayEvent.EventType.USER_IS_AWAY, true, false, event.arg(event.args().size() - 2), event.getRawEventData(), event.getSession());
		}
	}
}
