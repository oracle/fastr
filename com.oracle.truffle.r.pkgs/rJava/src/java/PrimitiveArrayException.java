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
 * Generated when one tries to convert an arrays into 
 * a primitive array of the wrong type
 */
public class PrimitiveArrayException extends Exception{
	public PrimitiveArrayException(String type){
		super( "cannot convert to single dimension array of primitive type" + type ) ; 
	}
	
}
