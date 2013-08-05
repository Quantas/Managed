/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 Andrew Landsverk
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.quantasnet.managed;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

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