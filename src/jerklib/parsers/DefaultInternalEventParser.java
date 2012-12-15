package jerklib.parsers;

import java.util.HashMap;
import java.util.Map;

import jerklib.events.IRCEvent;


/**
 * This is the default impl of InternalEventParser. This class is used to turn
 * raw irc text into IRCEvents. You can change the parsing of data and the creation of
 * events by adding/removing CommandParsers.
 * 
 * 
 * @see CommandParser
 * @author mohadib
 *
 */
public class DefaultInternalEventParser implements InternalEventParser
{
	private final Map<String , CommandParser> parsers = new HashMap<String, CommandParser>();
	private CommandParser defaultParser;
	
	public DefaultInternalEventParser()
	{
		initDefaultParsers();
	}
	
	public IRCEvent receiveEvent(IRCEvent e)
	{
		CommandParser parser = parsers.get(e.command());
		parser = parser == null? defaultParser : parser;
		return parser == null?e:parser.createEvent(e);
	}

	
	public void removeAllParsers()
	{
		parsers.clear();
	}
	
	public void addParser(String command , CommandParser parser)
	{
		parsers.put(command, parser);
	}
	
	public CommandParser getParser(String command)
	{
		return parsers.get(command);
	}
	
	public boolean removeParser(String command)
	{
		return parsers.remove(command) != null;
	}
	
	public void setDefaultParser(CommandParser parser)
	{
		defaultParser = parser;
	}
	
	public CommandParser getDefaultParser()
	{
		return defaultParser;
	}
	
	public void initDefaultParsers()
	{
		parsers.put("001" , new ConnectionCompleteParser());
		parsers.put("002" , new ServerVersionParser());
		parsers.put("005" , new ServerInformationParser());
		
		CommandParser awayParser = new AwayParser();
		parsers.put("301" , awayParser);
		parsers.put("305" , awayParser);
		parsers.put("306" , awayParser);
		
		parsers.put("314", new WhoWasParser());
		
		WhoisParser whoisParser = new WhoisParser();
		parsers.put("311", whoisParser);
		parsers.put("312", whoisParser);
		parsers.put("317", whoisParser);
		parsers.put("318", whoisParser);
		parsers.put("319", whoisParser);
		parsers.put("320", whoisParser);
		
		ChanListParser chanListParser = new ChanListParser();
		parsers.put("321" , chanListParser);
		parsers.put("322" , chanListParser);
		
		parsers.put("324" , new ModeParser());
		
		TopicParser topicParser = new TopicParser();
		parsers.put("332", topicParser);
		parsers.put("333", topicParser);
		
		parsers.put("351" , new ServerVersionParser());
		parsers.put("352", new WhoParser());
		
		NamesParser namesParser = new NamesParser();
		parsers.put("353" , namesParser);
		parsers.put("366" , namesParser);
		
		MotdParser motdParser = new MotdParser();
		parsers.put("372", motdParser);
		parsers.put("375", motdParser);
		parsers.put("376", motdParser);
		
		
		
		parsers.put("PRIVMSG", new PrivMsgParser());
		parsers.put("QUIT" , new QuitParser());
		parsers.put("JOIN" , new JoinParser());
		parsers.put("PART", new PartParser());
		parsers.put("NOTICE", new NoticeParser());
		parsers.put("TOPIC", new TopicUpdatedParser());
		parsers.put("INVITE", new InviteParser());
		parsers.put("NICK", new NickParser());
		parsers.put("MODE", new ModeParser());
        parsers.put("KICK",new KickParser());

		
		//numeric errors
		CommandParser errorParser = new NumericErrorParser();
		for(int i = 400 ; i < 553 ; i++)
		{
			parsers.put(String.valueOf(i), errorParser);
		}
		
		parsers.put("433", new NickInUseParser());
	}
}
