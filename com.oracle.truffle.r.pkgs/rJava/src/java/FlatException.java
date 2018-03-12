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
 * Generated when one attemps to flatten an array that is not rectangular
 */
public class FlatException extends Exception{
	public FlatException(){
		super( "Can only flatten rectangular arrays" ); 
	}
}
