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

public class DummyPoint implements Cloneable {
	public int x; 
	public int y ;
	public DummyPoint(){
		this( 0, 0 ) ;
	}
	public DummyPoint( int x, int y){
		this.x = x ;
		this.y = y ;
	}
	public double getX(){
		return (double)x ;
	}
	public void move(int x, int y){
		this.x += x ;
		this.y += y ;
	}
	
}
