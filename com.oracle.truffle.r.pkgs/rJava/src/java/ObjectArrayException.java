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
 * Generated when one tries to access an array of primitive
 * values as an array of Objects
 */
public class ObjectArrayException extends Exception{
	public ObjectArrayException(String type){
		super( "array is of primitive type : " + type ) ; 
	}
	
}
