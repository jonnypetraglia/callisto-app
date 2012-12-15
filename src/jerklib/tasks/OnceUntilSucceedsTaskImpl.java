package jerklib.tasks;

import jerklib.Session;
import jerklib.events.IRCEvent;
import jerklib.events.IRCEvent.Type;

/**
 * An augmented {@link TaskImpl} that only executes once
 * as it cancels itself out of the task queue once completed,
 * <em>until it succeeds</em> (if it bails out with a
 * <code>RuntimeException</code> or
 * {@linkplain #receiveEventOnce(IRCEvent) returns <code>false</code>},
 * it will remain in the task queue.
 * 
 * @see OnceTaskImpl
 * @see Session#onEvent(Task)
 * @see Session#onEvent(jerklib.tasks.Task, jerklib.events.IRCEvent.Type...)
 * @see TaskImpl
 * @see Type
 * 
 * @author pbleser
 *
 */
public abstract class OnceUntilSucceedsTaskImpl extends TaskImpl
{
    public OnceUntilSucceedsTaskImpl(String name)
    {
    	super(name);
    }
    
    /**
     * Process the {@link IRCEvent}.
     * 
     * @param e the {@link IRCEvent} to process
     * @return whether the task was successful or not; when
     * 		returning <code>false</code>, the task will
     * 		remain in the task queue; when returning <code>true</code>,
     * 		it will be removed (and not executed any more)
     */
    public abstract boolean receiveEventOnce(IRCEvent e);

    /**
     * {@inheritDoc}
     */
	public final void receiveEvent(IRCEvent e)
	{
		if (receiveEventOnce(e)) {
			this.cancel();
		}
	}

}
