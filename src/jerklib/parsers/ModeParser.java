package jerklib.parsers;

import java.util.ArrayList;
import java.util.List;

import jerklib.ModeAdjustment;
import jerklib.ServerInformation;
import jerklib.ModeAdjustment.Action;
import jerklib.ServerInformation.ModeType;
import jerklib.events.IRCEvent;
import jerklib.events.ModeEvent;


/**
 * @author mohadib
 * 
 * mode parser
 * 
 * developers see:
 * https://sourceforge.net/tracker/index.php?func=detail&aid=1962621&group_id=214803&atid=1031130
 * http://tools.ietf.org/draft/draft-hardy-irc-isupport/draft-hardy-irc-isupport-00.txt
 * 
 * known shortcoming: usermode event arguments are not correctly lined up
 *  Only way i can think to fix this is to hardcode known usermodes?
 * 
 */
public class ModeParser implements CommandParser
{
	//channel//  :mohadib_!n=mohadib@unaffiliated/mohadib MODE #jerklib +o scripyasas
	//channel//  :kubrick.freenode.net 324 mohadib__ #test +mnPzlfJ 101 #flood 1,2
	//usermode// :services. MODE mohadib :+e
	
	public IRCEvent createEvent(IRCEvent event)
	{
		boolean userMode = event.numeric() != 324 && !event.getSession().isChannelToken(event.arg(0));
		char[] modeTokens = new char[0];
		String[] arguments = new String[0];
		
		int modeOffs = event.numeric() == 324?2:1;
		modeTokens = event.arg(modeOffs).toCharArray();
		
		int size = event.args().size();
		if(modeOffs + 1 < size)
		{
			arguments = event.args().subList(modeOffs + 1, event.args().size()).toArray(arguments);
		}
		
		int argumntOffset = 0;
		char action = '+';
		List<ModeAdjustment> modeAdjustments = new ArrayList<ModeAdjustment>();
			
		for (char mode : modeTokens)
		{
			if (mode == '+' || mode == '-') action = mode;
			else
			{
				if(userMode)
				{
					String argument = argumntOffset >= arguments.length? "":arguments[argumntOffset];
					modeAdjustments.add(new ModeAdjustment(action == '+' ? Action.PLUS : Action.MINUS, mode, argument));
					argumntOffset++;
				}
				else
				{
					ServerInformation info = event.getSession().getServerInformation();
					ModeType type = info.getTypeForMode(String.valueOf(mode));
					// must have an argument on + and -
					if (type == ModeType.GROUP_A || type == ModeType.GROUP_B)
					{
						modeAdjustments.add(new ModeAdjustment(action == '+' ? Action.PLUS : Action.MINUS, mode, arguments[argumntOffset]));
						argumntOffset++;
					}
					// must have args on + , must not have args on -
					else if (type == ModeType.GROUP_C)
					{
						if (action == '-')
						{
							modeAdjustments.add(new ModeAdjustment(Action.MINUS, mode, ""));
						}
						else
						{
							modeAdjustments.add(new ModeAdjustment(Action.PLUS, mode, arguments[argumntOffset]));
							argumntOffset++;
						}
					}
					// no args
					else if (type == ModeType.GROUP_D)
					{
						modeAdjustments.add(new ModeAdjustment(action == '+' ? Action.PLUS : Action.MINUS, mode, ""));
					}
					else
					{
						System.err.println("unreconzied mode " + mode);
					}
				}
			}
		}
			
		if(userMode)
		{
			return new ModeEvent
			(
				ModeEvent.ModeType.USER, 
				event.getRawEventData(), 
				event.getSession(), 
				modeAdjustments , 
				event.getSession().getConnectedHostName(), 
				null
			);
		}
			
		return new ModeEvent
		(
			ModeEvent.ModeType.CHANNEL, 
			event.getRawEventData(), 
			event.getSession(), 
			modeAdjustments, 
			event.numeric() == 324 ? "" :  event.getNick(),
			event.getSession().getChannel(event.numeric() == 324 ?event.arg(1):event.arg(0))
		);
	}
}
