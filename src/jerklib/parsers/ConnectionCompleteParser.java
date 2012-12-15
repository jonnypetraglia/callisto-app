package jerklib.parsers;

import jerklib.events.ConnectionCompleteEvent;
import jerklib.events.IRCEvent;

public class ConnectionCompleteParser implements CommandParser
{
	
	/* :irc.nmglug.org 001 namnar :Welcome to the nmglug.org 
	 	
	 	Lets user know channels can now be joined etc.
	 	
	  Lets user update *records* 
	  A requested connection to irc.freenode.net might actually
	  connect to kubrick.freenode.net etc 
	*/
	
	public ConnectionCompleteEvent createEvent(IRCEvent event)
	{
		return new ConnectionCompleteEvent
		(
				event.getRawEventData(), 
				event.prefix(), // actual hostname
				event.getSession(), 
				event.getSession().getConnectedHostName() // requested hostname
		);
	}
}
