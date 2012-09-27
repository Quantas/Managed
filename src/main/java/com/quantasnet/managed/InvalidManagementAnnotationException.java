package com.quantasnet.managed;

/**
 * Exceptions for the Management Annotation package
 * @author Quantas
 */
public class InvalidManagementAnnotationException extends Exception 
{
	private static final long serialVersionUID = 1L;

	public InvalidManagementAnnotationException()
	{
		super();
	}
	
	public InvalidManagementAnnotationException(String s)
	{
		super(s);
	}
	
	public InvalidManagementAnnotationException(String s, Exception e)
	{
		super(s,e);
	}
}
