package jerklib.events;

import jerklib.Session;
import jerklib.events.IRCEvent;


/**
 * The event fired when a line from a channel listing is parsed
 *
 * @author mohaidb
 * @see Session#chanList()
 * @see Session#chanList(String)
 */
public class ChannelListEvent extends IRCEvent
{

	private final String channelName, topic;
	private final int numUsers;

	public ChannelListEvent
	(
		String rawEventData, 
		String channelName, 
		String topic, 
		int numUsers, 
		Session session)
	{
		super(rawEventData , session , Type.CHANNEL_LIST_EVENT);
		this.channelName = channelName;
		this.topic = topic;
		this.numUsers = numUsers;
	}

  /**
   * Gets the channel name
   *
   * @return the channel name
   */
	public String getChannelName()
	{
		return channelName;
	}

  /**
   * Gets the number of users in the channel
   *
   * @return number of users
   */
	public int getNumberOfUser()
	{
		return numUsers;
	}


  /**
   * Gets the topic of the channel
   *
   * @return the channel topic
   */
	public String getTopic()
	{
		return topic;
	}
}
