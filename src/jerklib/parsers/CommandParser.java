package jerklib.parsers;

import jerklib.events.IRCEvent;

public interface CommandParser
{
	public IRCEvent createEvent(IRCEvent event);
}
