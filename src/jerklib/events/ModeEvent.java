package jerklib.events;


import java.util.List;

import jerklib.Channel;
import jerklib.ModeAdjustment;
import jerklib.Session;
import jerklib.events.IRCEvent;

/**
 * @author mohadib
 * @see ModeEvent
 *
 */
public class ModeEvent extends IRCEvent
{

	private final ModeType modeType;
	private final String setBy;
	private final Channel channel;
	private final List<ModeAdjustment>modeAdjustments;

	public ModeEvent
	(
		ModeType type,
		String rawEventData, 
		Session session, 
		List<ModeAdjustment>modeAdjustments, 
		String setBy, 
		Channel channel
	)
	{
		super(rawEventData, session, Type.MODE_EVENT);
		modeType = type;
		this.modeAdjustments = modeAdjustments;
		this.setBy = setBy;
		this.channel = channel;
	}

	public enum ModeType
	{
		USER,
		CHANNEL
	}
	
  /**
   * If mode event adjusted a Channel mode
   * then the Channel effected will be returned
   *
   * @return Channel
   * @see Channel
   */
	public Channel getChannel()
	{
		return channel;
	}

  /**
   * Gets the list of mode adjustments generated 
   * @return List of mode adjustments
   */
	public List<ModeAdjustment> getModeAdjustments()
	{
		return modeAdjustments;
	}

  /**
   * Gets who set the mode
   *
   * @return who set the mode
   */
	public String setBy()
	{
		return setBy;
	}
	
	/**
	 * Indicates if this is a user mode or channel mode event
	 * @return the ModeType 
	 */
	public ModeType getModeType()
	{
		return modeType;
	}
}
