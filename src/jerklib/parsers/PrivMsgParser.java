package jerklib.parsers;

import jerklib.Channel;
import jerklib.Session;
import jerklib.events.CtcpEvent;
import jerklib.events.IRCEvent;
import jerklib.events.MessageEvent;
import jerklib.events.IRCEvent.Type;

public class PrivMsgParser implements CommandParser
{
	/*
	 * :gh00p!~ghoti@nix-58E3BFC5.cpe.net.cable.rogers.com PRIVMSG #tvtorrents :gotcha
	 * :NeWtoz!jimmy@nix-2F996C9F.dhcp.aldl.mi.charter.com PRIVMSG #tvtorrents :No problem
	 * :cute_bettong!n=elphias@about/apple/IIe/B0FH PRIVMSG #ubuntu :Elphias (elphias)
	 */
	
	public MessageEvent createEvent(IRCEvent event)
	{
		Session session = event.getSession();
		Type type = session.isChannelToken(event.arg(0))?Type.CHANNEL_MESSAGE:Type.PRIVATE_MESSAGE;
		Channel chan = type == Type.CHANNEL_MESSAGE? session.getChannel(event.arg(0)):null;
		
		MessageEvent me =  new MessageEvent
		(
			chan,
			event.arg(1), 
			event.getRawEventData(), 
			session, 
			type 
		);
		
		String msg = me.getMessage();
		if (msg.startsWith("\u0001"))
		{
				return new CtcpEvent
				(
					msg.substring(1, msg.length() - 1), 
					me.getMessage(), 
					me.getRawEventData(), 
					me.getChannel(), 
					me.getSession()
				);
		}
		
		return me;
	}
}
