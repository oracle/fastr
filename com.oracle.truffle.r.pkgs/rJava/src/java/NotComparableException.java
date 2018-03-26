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
 * Exception generated when two objects cannot be compared
 * 
 * Such cases happen when an object does not implement the Comparable 
 * interface or when the comparison produces a ClassCastException
 */
public class NotComparableException extends Exception{
	public NotComparableException(Object a, Object b){
		super( "objects of class " + a.getClass().getName() + 
			" and " + b.getClass().getName() + " are not comparable"  ) ;
	}
	public NotComparableException( Object o){
		this( o.getClass().getName() ) ;
	}
	
	public NotComparableException( Class cl){
		this( cl.getName() ) ;
	}
	
	public NotComparableException( String type ){
		super( "class " + type + " does not implement java.util.Comparable" ) ; 
	}
	
}
