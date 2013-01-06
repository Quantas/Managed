package com.quantasnet.managed;

/**
 * Exceptions for the Management Annotation package
 * 
 * @author Quantas
 */
/*package*/ final class InvalidManagementAnnotationException extends Exception 
{
	private static final long serialVersionUID = 1L;

	public InvalidManagementAnnotationException()
	{
		super();
	}
	
	public InvalidManagementAnnotationException(final String s)
	{
		super(s);
	}
	
	public InvalidManagementAnnotationException(final String s, final Exception e)
	{
		super(s,e);
	}
}