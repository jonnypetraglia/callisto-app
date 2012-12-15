package jerklib.parsers;

import jerklib.Session;
import jerklib.events.IRCEvent;
import jerklib.events.ServerInformationEvent;

public class ServerInformationParser implements CommandParser
{
	public IRCEvent createEvent(IRCEvent event)
	{
		Session session = event.getSession();
		session.getServerInformation().parseServerInfo(event.getRawEventData());
		return new ServerInformationEvent(session, event.getRawEventData(), session.getServerInformation());
	}
}
