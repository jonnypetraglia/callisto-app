package jerklib.events;


import java.util.Date;

import jerklib.Channel;
import jerklib.Session;
import jerklib.events.IRCEvent;

/**
 * 
 * Event fired when topic is received
 * @author mohadib
 * @see Channel
 */
public class TopicEvent extends IRCEvent
{

	private String setBy,hostname;
	private Date setWhen;
	private Channel channel;
	private StringBuffer buff = new StringBuffer();

	public TopicEvent(String rawEventData, Session session, Channel channel, String topic)
	{
		super(rawEventData, session, Type.TOPIC);
		this.channel = channel;
		buff.append(topic);
	}

  /**
   * Gets the topic
   *
   * @return the topic
   */
	public String getTopic()
	{
		return buff.toString();
	}

	/**
	 * @return hostname
	 */
	public String getHostName()
	{
		return hostname;
	}

	/**
	 * @param setWhen
	 */
	public void setSetWhen(String setWhen)
	{
		this.setWhen = new Date(1000L * Long.parseLong(setWhen));
	}

	/**
	 * @param setBy
	 */
	public void setSetBy(String setBy)
	{
		this.setBy = setBy;
	}

  /**
   * Gets who set the topic
   *
   * @return topic setter
   */
	public String getSetBy()
	{
		return setBy;
	}

  /**
   * Gets when topic was set
   *
   * @return when
   */
	public Date getSetWhen()
	{
		return setWhen;
	}

	/* (non-Javadoc)
	 * @see jerklib.events.TopicEvent#getChannel()
	 */
	public Channel getChannel()
	{
		return channel;
	}

	/**
	 * @param topic
	 */
	public void appendToTopic(String topic)
	{
		buff.append(topic);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode()
	{
		return channel.hashCode();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o)
	{
		if (o == this) { return true; }
		if (o instanceof TopicEvent && o.hashCode() == hashCode()) { return ((TopicEvent) o).getChannel().equals(getChannel()); }
		return false;
	}

}
