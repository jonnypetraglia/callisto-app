package jerklib.events;

import jerklib.Session;
import jerklib.events.IRCEvent;

/**
 * <p>
 * This is an event that is fired under three conditions:
 * <ul>
 * <li>Sending a message to a user who is marked as away.</li>
 * <li>User of the lib marks self as away.</li>
 * <li>User of the lib returns from away.</li>
 * </ul>
 * You can determine under which circumstance the event was fired by looking at
 * the {@link EventType}.</p>
 *
 * @author <a href="mailto:robby.oconnor@gmail.com">Robert O'Connor<a/>
 */
public class AwayEvent extends IRCEvent
{
    private final boolean isAway, isYou;
    private final String awayMessage, nick;
    private EventType eventType;

    /**
     * An enum to determine the type of event that was fired.
     * <br>WENT_AWAY is when user of the lib goes away.<br>
     * RETURNED_FROM_AWAY is when user of the lib returns from an away state.<br>
     * USER_IS_AWAY is when some other user goes away<br>
     */
    public enum EventType
    {
        WENT_AWAY,
        RETURNED_FROM_AWAY,
        USER_IS_AWAY
    }
    
    public AwayEvent
    (
    	String awayMessage, 
    	EventType eventType, 
    	boolean away, 
    	boolean you, 
    	String nick, 
    	String rawEventData, 
    	Session session
    )
    {
    		super(rawEventData , session , Type.AWAY_EVENT);
        this.awayMessage = awayMessage;
        this.eventType = eventType;
        isAway = away;
        isYou = you;
        this.nick = nick;
    }

    
    /**
     * Returns the away message or an empty String if it was user of lib who caused the event to fire.
     *
     * @return the away message
     */
    public String getAwayMessage()
    {
        return awayMessage;
    }

    /**
     * Whether or not subject of event is away.
     *
     * @return if we're away or not.
     */
    public boolean isAway()
    {
        return isAway;
    }

    /**
     * Whether or not it was user of lib that caused this event
     *
     * @return if it was us or not.
     */
    public boolean isYou()
    {
        return isYou;
    }
    
    /**
     * Get the nick who fired the event.
     *
     * @return the nick of the user who caused the event to fire.
     */
    public String getNick()
    {
        return nick;
    }

    /**
     * Return the event type that was fired
     *
     * @return the type of event that was fired.
     * @see EventType
     */
    public EventType getEventType()
    {
        return eventType;
    }
}