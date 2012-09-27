package com.quantasnet.managed;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.CONSTRUCTOR;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to denote an object as being able to be controlled through JMX<br />
 * Put it on a field to be able to manage a field<br />
 * Put it on a method to be able to invoke that method<br/>
 * @author Quantas
 */
@Target({FIELD,METHOD,CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface Managed 
{
	/**
	 * Provide a description to be displayed in a JMX Console
	 * @return String containing the description
	 */
	public String description() default "Dynamic Management MBean";
	
	/**
	 * Say if we are able to write to the field
	 * @return writable - defaults to false
	 */
	public boolean writable() default false;
	
	/**
	 * Say if we are able to read from the field
	 * @return readable -  defaults to true
	 */
	public boolean readable() default true;
}