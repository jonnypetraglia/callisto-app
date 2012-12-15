package jerklib.parsers;

import java.util.List;

import jerklib.Channel;
import jerklib.Session;
import jerklib.events.IRCEvent;
import jerklib.events.QuitEvent;


public class QuitParser implements CommandParser
{
	public QuitEvent createEvent(IRCEvent event)
	{
		Session session = event.getSession();
		String nick = event.getNick();
		List<Channel> chanList = event.getSession().removeNickFromAllChannels(nick);
		return new QuitEvent
		(
			event.getRawEventData(), 
			session, 
			event.arg(0), // message
			chanList
		);
	}
}
