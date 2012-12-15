package jerklib.parsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jerklib.events.IRCEvent;


public class TopicUpdatedParser implements CommandParser
{
	public IRCEvent createEvent(IRCEvent event)
	{
		Pattern p = Pattern.compile("^.+?TOPIC\\s+(.+?)\\s+.*$");
		Matcher m = p.matcher(event.getRawEventData());
		m.matches();
		event.getSession().sayRaw("TOPIC " + m.group(1));
		return event;
	}
}
