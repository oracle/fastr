/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2006, Simon Urbanek
 * Copyright (c) 2018, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

/**
 * Exception indicating that an object is not a java array
 */
public class NotAnArrayException extends Exception{
	public NotAnArrayException(Class clazz){
		super( "not an array : " + clazz.getName() ) ;
	}
	public NotAnArrayException(String message){
		super( message ) ;
	}
}
	
