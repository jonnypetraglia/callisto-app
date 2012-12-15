package jerklib.parsers;

import java.util.Arrays;
import java.util.List;

import jerklib.events.IRCEvent;
import jerklib.events.WhoisEvent;


public class WhoisParser implements CommandParser
{
	private WhoisEvent we;
	
	public IRCEvent createEvent(IRCEvent event)
	{
		switch (event.numeric())
		{
			case 311:
			{
				
				// "<nick> <user> <host> * :<real name>"
				we = new WhoisEvent
				(		
					event.arg(0),
					event.arg(4),
					event.arg(1),
					event.arg(2),
					event.getRawEventData(), 
					event.getSession()
				); 
				break;
			}
			case 319:
			{
				// "<nick> :{[@|+]<channel><space>}"
				// :kubrick.freenode.net 319 scripy mohadib :@#jerklib
				// kubrick.freenode.net 319 scripy mohadib :@#jerklib ##swing
				if (we != null )
				{
					List<String> chanNames = Arrays.asList(event.arg(2).split("\\s+"));
					we.setChannelNamesList(chanNames);
					we.appendRawEventData(event.getRawEventData());
				}
				break;
			}
			case 312:
			{
				// "<nick> <server> :<server info>"
				// :kubrick.freenode.net 312 scripy mohadib irc.freenode.net :http://freenode.net/
				if (we != null)
				{
					we.setWhoisServer(event.arg(2));
					we.setWhoisServerInfo(event.arg(3));
					we.appendRawEventData(event.getRawEventData());
				}
				break;
			}
			case 320:
			{
				// not in RFC1459
				// :kubrick.freenode.net 320 scripy mohadib :is identified to services
				if (we != null)
				{
					we.appendRawEventData(event.getRawEventData());
				}
				break;
			}
			case 317:
			{
				//:anthony.freenode.net 317 scripy scripy 2 1202063240 :seconds idle,signon time
				// from rfc "<nick> <integer> :seconds idle"
				if (we != null)
				{
					we.setSignOnTime(Integer.parseInt(event.arg(3)));
					we.setSecondsIdle(Integer.parseInt(event.arg(2)));
					we.appendRawEventData(event.getRawEventData());
				}
				break;
			}
			case 318:
			{
				// end of whois - fireevent
				if (we != null)
				{
					we.appendRawEventData(event.getRawEventData());
					return we;
				}
				break;
			}
		}
		return event;
	}
}
