package jerklib.parsers;

import jerklib.Channel;
import jerklib.Session;
import jerklib.events.IRCEvent;
import jerklib.events.KickEvent;

/**
 * @author mohadib
 *
 */
public class KickParser implements CommandParser
{
	public IRCEvent createEvent(IRCEvent event)
	{
		Session session = event.getSession();
		Channel channel = session.getChannel(event.arg(0));
		
		String msg = "";
		if (event.args().size() == 3)
		{
			msg = event.arg(2);
		}
		
		return new KickEvent
		(
			event.getRawEventData(), 
			session, 
			event.getNick(), // byWho
			event.arg(1), // victim
			msg, // message
			channel
		);
	}
}
