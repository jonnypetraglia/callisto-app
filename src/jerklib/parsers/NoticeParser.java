package jerklib.parsers;

import jerklib.Channel;
import jerklib.Session;
import jerklib.events.IRCEvent;
import jerklib.events.NoticeEvent;

public class NoticeParser implements CommandParser
{
	
	/*
	 *:DIBLET!n=fran@c-68-35-11-181.hsd1.nm.comcast.net NOTICE #jerklib :test
	 *:anthony.freenode.net NOTICE mohadib_ :NickServ set your hostname to foo
	 *:DIBLET!n=fran@c-68-35-11-181.hsd1.nm.comcast.net NOTICE #jerklib :test
	 *:NickServ!NickServ@services. NOTICE mohadib_ :This nickname is owned by someone else
	 * NOTICE AUTH :*** No identd (auth) response
	 */
	
	public IRCEvent createEvent(IRCEvent event)
	{
		Session session = event.getSession();
		
		String toWho = "";
		String byWho = session.getConnectedHostName();
		Channel chan = null;
		
		if(!session.isChannelToken(event.arg(0)))
		{
			toWho = event.arg(0);
			if(toWho.equals("AUTH")) toWho = "";
		}
		else
		{
			chan = session.getChannel(event.arg(0));
		}
		
		if(event.prefix().length() > 0)
		{
			if(event.prefix().contains("!"))
			{
				byWho = event.getNick();
			}
			else
			{
				byWho = event.prefix();
			}
		}
		
		return new NoticeEvent
		(
			event.getRawEventData(),
			event.getSession(),
			event.arg(1),
			toWho,
			byWho,
			chan
		);
	}
}
