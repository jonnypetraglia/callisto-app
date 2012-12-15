package jerklib.events;

import jerklib.Session;

public class GenericErrorEvent extends ErrorEvent
{
	private final Exception ex;
	
	public GenericErrorEvent(String data, Session session, Exception exception)
	{
		super(data, session, ErrorType.GENERIC);
		ex = exception;
	}
	
	public Exception getException()
	{
		return ex;
	}
	
}
