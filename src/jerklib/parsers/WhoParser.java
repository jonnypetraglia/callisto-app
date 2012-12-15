package jerklib.parsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jerklib.events.IRCEvent;
import jerklib.events.WhoEvent;


public class WhoParser implements CommandParser
{
	public IRCEvent createEvent(IRCEvent event)
	{
		String data = event.getRawEventData();
		Pattern p = Pattern.compile("^:.+?\\s+352\\s+.+?\\s+(.+?)\\s+(.+?)\\s+(.+?)\\s+(.+?)\\s+(.+?)\\s+(.+?):(\\d+)\\s+(.+)$");
		Matcher m = p.matcher(data);
		if (m.matches())
		{
			
			boolean away = m.group(6).charAt(0) == 'G';
			return new WhoEvent(m.group(1), // channel
					Integer.parseInt(m.group(7)), // hop count
					m.group(3), // hostname
					away, // status indicator
					m.group(5), // nick
					data, // raw event data
					m.group(8), // real name
					m.group(4), // server name
					event.getSession(), // session
					m.group(2) // username
			);
		}
		return event;
	}
}
