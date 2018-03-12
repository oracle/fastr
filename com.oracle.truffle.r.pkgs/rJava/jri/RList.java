package org.rosuda.JRI;

// JRclient library - client interface to Rserve, see http://www.rosuda.org/Rserve/
// Copyright (C) 2004 Simon Urbanek
// --- for licensing information see LICENSE file in the original JRclient distribution ---

import java.util.*;

/** implementation of R-lists<br>
    This is rather preliminary and may change in future since it's not really proper.
    The point is that the parser tries to interpret lists to be of the form entry=value,
    where entry is stored in the "head" part, and value is stored in the "body" part.
    Then using {@link #at(String)} it is possible to fetch "body" for a specific "head".
    The terminology used is partly from hash fields - "keys" are the elements in "head"
    and values are in "body" (see {@link #keys}).
    <p>
    On the other hand, R uses lists to store complex internal structures, which are not
    parsed according to the structure - in that case "head" and "body" have to be evaluated
    separately according to their meaning in that context.

    @version $Id: RList.java 2720 2007-03-15 17:35:42Z urbanek $
*/
public class RList extends Object {
    /** xpressions containing head, body and tag. 
	The terminology is a bit misleading (for historical reasons) - head corresponds to CAR, body to CDR and finally tag is TAG. */
    public REXP head, body, tag;

	/** cached keys (from TAG) */
    String[] keys = null;
	/** cached values(from CAR) */
	REXP[] values = null;
	/** flag denoting whether we need to re-fetch the cached values.<p><b.Note:</b> the current assumption is that the contents don't change after first retrieval - there is currently no recursive check! */
	boolean dirtyCache = true;
	
    /** constructs an empty list */
    public RList() { head=body=tag=null; }
    
	/** fake constructor to keep compatibility with Rserve (for now, will be gone soon) */
	public RList(RVector v) {
		Vector n = v.getNames();
		if (n != null) {
			keys = new String[n.size()];
			n.copyInto(keys);
		}
		values=new REXP[v.size()];
		v.copyInto(values);
		dirtyCache=false;
		// head,tail,tag are all invalid!
	}	

    /** constructs an initialized list
	@param h head xpression
	@param b body xpression */
    public RList(REXP h, REXP b) { head=h; body=b; tag=null; }

	/** constructs an initialized list
		@param h head xpression (CAR)
		@param t tag xpression (TAG)
		@param b body/tail xpression (CDR)
		*/
    public RList(REXP h, REXP t, REXP b) { head=h; body=b; tag=t; }
	
    /** get head xpression (CAR)
	@return head xpression */
    public REXP getHead() { return head; }
    
    /** get body xpression (CDR)
	@return body xpression */
    public REXP getBody() { return body; }

    /** get tag xpression
	@return tag xpression */
    public REXP getTag() { return tag; }

    /** internal function that updates cached vectors
        @return <code>true</code> if the conversion was successful */
    boolean updateVec() {
		if (!dirtyCache) return true;
		// we do NOT run it recursively, because in most cases only once instance is asked
		RList cur = this;
		int l = 0;
		while (cur!=null) {
			l++;
			REXP bd = cur.getBody();
			cur = (bd==null)?null:bd.asList();
		}
		keys=new String[l];
		values=new REXP[l];
		cur = this;
		l=0;
		while (cur != null) {
			REXP x = cur.getTag();
			if (x!=null) keys[l]=x.asSymbolName();
			values[l] = cur.getHead();
			REXP bd = cur.getBody();
			cur = (bd==null)?null:bd.asList();
			l++;
		}
		dirtyCache=false;
		return true;
    }

    /** get xpression given a key
		@param v key
		@return xpression which corresponds to the given key or
		<code>null</code> if list is not standartized or key not found */
    public REXP at(String v) {
		if (!updateVec() || keys==null || values==null) return null;
		int i=0;
		while (i<keys.length) {
			if (keys[i].compareTo(v)==0) return values[i];
			i++;
		}
		return null;
    }

    /** get element at the specified position
	@param i index
	@return xpression at the index or <code>null</code> if list is not standartized or
	        if index out of bounds */
    public REXP at(int i) {
		return (!updateVec() || values==null || i<0 || i>=values.length)?null:values[i];
    }

    /** returns all keys of the list
	@return array containing all keys or <code>null</code> if list is not standartized */
    public String[] keys() {
		return (!updateVec())?null:this.keys;
    }
}
