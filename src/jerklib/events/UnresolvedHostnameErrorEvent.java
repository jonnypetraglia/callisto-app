package jerklib.events;

import java.nio.channels.UnresolvedAddressException;

import jerklib.Session;


/**
 * Error generated when a DNS lookup fails during connection.
 * 
 * @author mohadib
 *
 */
public class UnresolvedHostnameErrorEvent extends ErrorEvent
{
	private String hostName;
	private UnresolvedAddressException exception;

	public UnresolvedHostnameErrorEvent
	(
		Session session, 
		String rawEventData, 
		String hostName, 
		UnresolvedAddressException exception
	)
	{
		super(rawEventData, session, ErrorType.UNRESOLVED_HOSTNAME);
		this.hostName = hostName;
		this.exception = exception;
	}

  /**
   * Gets the wrapped UnresolvedAddressException
   * @return UnresolvedAddressException
   */
	public UnresolvedAddressException getException()
	{
		return exception;
	}

	 /**
   * Gets the unresolvable hostname
   * @return hostname that could not be resloved
   */
	public String getHostName()
	{
		return hostName;
	}
}
