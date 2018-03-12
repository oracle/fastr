package org.rosuda.JRI;

import java.util.Vector;

/**
 * This class encapsulates and caches R objects as returned from R. Currently it
 * only converts certain SEXPs references from R into Java obejcts, but
 * eventually bi-directional support should be added. The currently supported
 * objects are string, integer and numeric vectors. All other types can be
 * accessed only using {@link #xp} reference and RNI methods.
 */
public class REXP {
	/** xpression type: NULL */
	public static final int XT_NULL = 0;

	/** xpression type: integer */
	public static final int XT_INT = 1;

	/** xpression type: double */
	public static final int XT_DOUBLE = 2;

	/** xpression type: String */
	public static final int XT_STR = 3;

	/** xpression type: language construct (currently content is same as list) */
	public static final int XT_LANG = 4;

	/** xpression type: symbol (content is symbol name: String) */
	public static final int XT_SYM = 5;

	/** xpression type: RBool */
	public static final int XT_BOOL = 6;

	/** xpression type: Vector */
	public static final int XT_VECTOR = 16;

	/** xpression type: RList */
	public static final int XT_LIST = 17;
	
	/**
	 * xpression type: closure (there is no java class for that type (yet?).
	 * currently the body of the closure is stored in the content part of the
	 * REXP. Please note that this may change in the future!)
	 */
	public static final int XT_CLOS = 18;

	/** xpression type: int[] */
	public static final int XT_ARRAY_INT = 32;

	/** xpression type: double[] */
	public static final int XT_ARRAY_DOUBLE = 33;

	/** xpression type: String[] (currently not used, Vector is used instead) */
	public static final int XT_ARRAY_STR = 34;

	/** internal use only! this constant should never appear in a REXP */
	public static final int XT_ARRAY_BOOL_UA = 35;

	/** xpression type: RBool[] */
	public static final int XT_ARRAY_BOOL = 36;

	/** xpression type: int[] to be interpreted as boolean */
	public static final int XT_ARRAY_BOOL_INT = 37;

	/** xpression type: unknown; no assumptions can be made about the content */
	public static final int XT_UNKNOWN = 48;

	/** xpression type: pure reference, no internal type conversion performed */
	public static final int XT_NONE = -1;

	/**
	 * xpression type: RFactor; this XT is internally generated (ergo is does
	 * not come from Rsrv.h) to support RFactor class which is built from
	 * XT_ARRAY_INT
	 */
	public static final int XT_FACTOR = 127;

	/* internal SEXP types in R - taken directly from Rinternals.h */
	public static final int NILSXP = 0; /* nil = NULL */

	public static final int SYMSXP = 1; /* symbols */

	public static final int LISTSXP = 2; /* lists of dotted pairs */

	public static final int CLOSXP = 3; /* closures */

	public static final int ENVSXP = 4; /* environments */

	public static final int PROMSXP = 5; /*
											 * promises: [un]evaluated closure
											 * arguments
											 */

	public static final int LANGSXP = 6; /*
											 * language constructs (special
											 * lists)
											 */

	public static final int SPECIALSXP = 7; /* special forms */

	public static final int BUILTINSXP = 8; /* builtin non-special forms */

	public static final int CHARSXP = 9; /*
											 * "scalar" string type (internal
											 * only)
											 */

	public static final int LGLSXP = 10; /* logical vectors */

	public static final int INTSXP = 13; /* integer vectors */

	public static final int REALSXP = 14; /* real variables */

	public static final int CPLXSXP = 15; /* complex variables */

	public static final int STRSXP = 16; /* string vectors */

	public static final int DOTSXP = 17; /* dot-dot-dot object */

	public static final int ANYSXP = 18; /* make "any" args work */

	public static final int VECSXP = 19; /* generic vectors */

	public static final int EXPRSXP = 20; /* expressions vectors */

	public static final int BCODESXP = 21; /* byte code */

	public static final int EXTPTRSXP = 22; /* external pointer */

	public static final int WEAKREFSXP = 23; /* weak reference */

	public static final int RAWSXP = 24; /* raw bytes */

	public static final int S4SXP = 25; /* S4 object */

	public static final int FUNSXP = 99; /* Closure or Builtin */

	/**
	 * Engine which this EXP was obtained from. EXPs are valid only for the
	 * engine they were obtained from - it's illegal to mix EXP between engines.
	 * There is a speacial case when the engine may be null - if a REXP creating
	 * was requested but deferred until an engine is available.
	 */
	Rengine eng;

	/**
	 * native reference to the SEXP represented in R. It's usually a pointer,
	 * but can be any handle obtained from the engine. This reference can be
	 * used when calling RNI commands directly.
	 */
	public long xp;

	/**
	 * native type of the represented expression (see ...SXP constants in R).
	 * Please note that this type is cached and may have changed in the
	 * meantime. If the possibility of changing type exists (mainly list/lang)
	 * then use rniExpType to make sure
	 */
	public int rtype;

	/**
	 * create a REXP directly from a R SEXP reference. SEXP types STRSXP, INTSXP
	 * and REALSXP are automatically converted. All others are represented as
	 * SEXP references only.
	 */
	public REXP(Rengine re, long exp) {
		this(re, exp, true);
	}
	
	protected void finalize() throws Throwable {
		try {
			if (Xt == XT_NONE && xp != 0 && eng != null) // release underlying R obejct if it was preserved
				eng.rniRelease(xp);
		} finally {
			super.finalize();
		}
	}

	public REXP(Rengine re, long exp, boolean convert) {
		eng = re;
		xp = exp;
		rtype = re.rniExpType(xp);
		//System.out.println("["+rtype+"@"+exp+","+convert+"]");

		if (!convert) {
			Xt = XT_NONE;
			if (re != null && xp != 0)
				re.rniPreserve(xp); // preserve the object so it doesn't get garbage-collected while we are referencing it
			return;
		}
				
		if (rtype == STRSXP) {
			String[] s = re.rniGetStringArray(xp);
			if (s != null && s.length == 1) {
				cont = s[0];
				Xt = XT_STR;
			} else {
				cont = s;
				Xt = XT_ARRAY_STR;
			}
		} else if (rtype == INTSXP) {
			cont = null;
			if (re.rniInherits(xp, "factor")) {
				long levx = re.rniGetAttr(xp, "levels");
				if (levx != 0) {
					String[] levels = null;
					// we're using low-lever calls here (FIXME?)
					int rlt = re.rniExpType(levx);
					if (rlt == STRSXP) {
						levels = re.rniGetStringArray(levx);
						int[] ids = re.rniGetIntArray(xp);
						cont = new RFactor(ids, levels, 1);
						Xt = XT_FACTOR;
					}
				}
			}
			// if it's not a factor, then we use int[] instead
			if (cont == null ) {
				cont = re.rniGetIntArray(xp);
				Xt = XT_ARRAY_INT;
			}
		} else if (rtype == REALSXP) {
			cont = re.rniGetDoubleArray(xp);
			Xt = XT_ARRAY_DOUBLE;
		} else if (rtype == LGLSXP) {
		    cont = re.rniGetBoolArrayI(xp);
		    Xt = XT_ARRAY_BOOL_INT;
		} else if (rtype == VECSXP) {
			long[] l = re.rniGetVector(xp);
			cont = new RVector();
			int i = 0;
			//System.out.println("VECSXP, length="+l.length);
			Xt = XT_VECTOR;
			while (i < l.length)
				((RVector)cont).addElement(new REXP(re, l[i++]));
			long na = re.rniGetAttr(xp, "names");
			if (na!=0 && re.rniExpType(na)==STRSXP)
				((RVector)cont).setNames(re.rniGetStringArray(na));
		} else if (rtype == LISTSXP) {
			long car = re.rniCAR(xp);
			long cdr = re.rniCDR(xp);
			long tag = re.rniTAG(xp);

			REXP cdrx = (cdr==0 || re.rniExpType(cdr)!=LISTSXP)?null:new REXP(re,re.rniCDR(xp));
			cont = new RList(new REXP(re,car), (tag==0)?null:new REXP(re,tag), cdrx);
			Xt = XT_LIST;
		} else if (rtype == SYMSXP) {
			cont = re.rniGetSymbolName(xp);
			Xt = XT_SYM;
		} else
			Xt = XT_NULL;
		
		//System.out.println("new REXP: "+toString());
	}

	/** xpression type */
	int Xt;

	/** attribute xpression or <code>null</code> if none */
	REXP attr;

	/** content of the xpression - its object type is dependent of {@link #Xt} */
	Object cont;

	/** cached binary length; valid only if positive */
	long cachedBinaryLength = -1;

	/** construct a new, empty (NULL) expression w/o attribute */
	public REXP() {
		Xt = XT_NULL;
		attr = null;
		cont = null;
	}

	/**
	 * construct a new xpression of type t and content o, but no attribute
	 * 
	 * @param t
	 *            xpression type (XT_...)
	 * @param o
	 *            content
	 */
	public REXP(int t, Object o) {
		Xt = t;
		cont = o;
		attr = null;
	}

	/**
	 * construct a new xpression of type t, content o and attribute at
	 * 
	 * @param t
	 *            xpression type
	 * @param o
	 *            content
	 * @param at
	 *            attribute
	 */
	public REXP(int t, Object o, REXP at) {
		Xt = t;
		cont = o;
		attr = at;
	}

	/**
	 * construct a new xpression of type XT_ARRAY_DOUBLE and content val
	 * 
	 * @param val
	 *            array of doubles to store in the REXP
	 */
	public REXP(double[] val) {
		this(XT_ARRAY_DOUBLE, val);
	}

	/**
	 * construct a new xpression of type XT_ARRAY_INT and content val
	 * 
	 * @param val
	 *            array of integers to store in the REXP
	 */
	public REXP(int[] val) {
		this(XT_ARRAY_INT, val);
	}

	/**
	 * construct a new xpression of type XT_ARRAY_INT and content val
	 * 
	 * @param val
	 *            array of integers to store in the REXP
	 */
	public REXP(String[] val) {
		this(XT_ARRAY_STR, val);
	}

    /** construct new expression with the contents of a boolean vector
	@since JRI 0.3-2
	@param val contents */
	public REXP(boolean[] val) {
	    Xt = XT_ARRAY_BOOL_INT;
	    if (val==null) { cont = new int[0]; } else {
		int [] ic = new int[val.length];
		int i=0;
		while (i<val.length) { ic[i]=val[i]?1:0; i++; }
		cont = ic;
	    }
	    attr = null;
	}

	/**	get attributes of the REXP. In R every object can have attached attribute
		xpression. Some more complex structures such as classes are built that
		way.
		@return attribute xpression or <code>null</code> if there is none associated
		@since JRI 0.3, replaces <code>getAttribute()</code> but should be avoided if possible - use {@link #getAttribute(String)} instead.
		*/
	public REXP getAttributes() {
		return attr;
	}

	/** retrieve a specific attribute.<br>
		<b>Note:</b> the current representation fetches the attribute ad-hoc, so it breaks the assumption that the expression is no longer accessed after the constructor was called. This should change in the future.
		@param name name of the attribute
		@return REXP containing the attribute or <code>null</code> if the attribute doesn't exist. The conversion flag is inherited from this REXP.
		@since JRI 0.3
		*/
	public REXP getAttribute(String name) {
		// FIXME: we could do some caching if attr is not null ...
		long aref = eng.rniGetAttr(xp, name);
		if (aref==0) return null;
		return new REXP(eng, aref, (Xt != XT_NONE));
	}
	
	/**
	 * get raw content. Use as... methods to retrieve contents of known type.
	 * 
	 * @return content of the REXP
	 */
	public Object getContent() {
		return cont;
	}

	/**
	 * get xpression type (see XT_.. constants) of the content. It defines the
	 * type of the content object.
	 * 
	 * @return xpression type
	 */
	public int getType() {
		return Xt;
	}

	/**
	 * Obtains R engine object which supplied this REXP.
	 * 
	 * @returns {@link Rengine} object
	 */
	Rengine getEngine() {
		return eng;
	};

	/** return the first element of a character vector if this REXP is a character vector of length 1 or more, return <code>null</code> otherwise */
	public String asString() {
		if (cont == null)
			return null;
		if (Xt == XT_STR)
			return (String) cont;
		if (Xt == XT_ARRAY_STR) {
			String[] sa = (String[]) cont;
			return (sa.length > 0) ? sa[0] : null;
		}
		return null;
	}

	/** return the name of the symbol represented by this REXP if is it a symbol or <code>null</code> otherwise */
	public String asSymbolName() {
		return (Xt == XT_SYM)?((String) cont):null;
	}
	
	/** return the contents of this REXP as an array of strings if this REXP is a character vector, return <code>null</code> otherwise */
	public String[] asStringArray() {
		if (cont == null)
			return null;
		if (Xt == XT_STR) {
			String[] sa = new String[1];
			sa[0] = (String) cont;
			return sa;
		}
		if (Xt == XT_ARRAY_STR)
			return (String[]) cont;
		return null;
	}

	/**
	 * get content of the REXP as int (if it is one)
	 * 
	 * @return int content or 0 if the REXP is no integer
	 */
	public int asInt() {
		if (Xt == XT_ARRAY_INT) {
			int i[] = (int[]) cont;
			if (i != null && i.length > 0)
				return i[0];
		}
		return (Xt == XT_INT) ? ((Integer) cont).intValue() : 0;
	}

	/**
	 * get content of the REXP as double (if it is one)
	 * 
	 * @return double content or 0.0 if the REXP is no double
	 */
	public double asDouble() {
		if (Xt == XT_ARRAY_DOUBLE) {
			double d[] = (double[]) cont;
			if (d != null && d.length > 0)
				return d[0];
		}
		return (Xt == XT_DOUBLE) ? ((Double) cont).doubleValue() : 0.0;
	}

	/**
	 * get content of the REXP as {@link Vector} (if it is one)
	 * 
	 * @return Vector content or <code>null</code> if the REXP is no Vector
	 */
	public RVector asVector() {
		return (Xt == XT_VECTOR) ? (RVector) cont : null;
	}

	/**
	 * get content of the REXP as {@link RFactor} (if it is one)
	 * 
	 * @return {@link RFactor} content or <code>null</code> if the REXP is no
	 *         factor
	 */
	public RFactor asFactor() {
		return (Xt == XT_FACTOR) ? (RFactor) cont : null;
	}

	/**
	 * get content of the REXP as {@link RList} if the contents is a list or a generic vector
	 * 
	 * @return {@link RList} content or <code>null</code> if the REXP is neither a list nor a generic vector
	 */
	public RList asList() {
		return (Xt == XT_LIST) ?
		(RList) cont :
		( // for compatibility with Rserve we convert vectors to lists
		  (Xt == XT_VECTOR) ? new RList((RVector)cont) : null
		  );
	}

	/**
	 * get content of the REXP as {@link RBool} (if it is one)
	 * 
	 * @return {@link RBool} content or <code>null</code> if the REXP is no
	 *         logical value
	 */
	public RBool asBool() {
	    if (Xt == XT_ARRAY_BOOL_INT) {
		int [] ba = (int[]) cont;
		return (ba!=null && ba.length>0)?new RBool(ba[0]):null;
	    }
	    return (Xt == XT_BOOL) ? (RBool) cont : null;
	}

	/**
	 * get content of the REXP as an array of doubles. Array of integers, single
	 * double and single integer are automatically converted into such an array
	 * if necessary.
	 * 
	 * @return double[] content or <code>null</code> if the REXP is not a
	 *         array of doubles or integers
	 */
	public double[] asDoubleArray() {
		if (Xt == XT_ARRAY_DOUBLE)
			return (double[]) cont;
		if (Xt == XT_DOUBLE) {
			double[] d = new double[1];
			d[0] = asDouble();
			return d;
		}
		if (Xt == XT_INT) {
			double[] d = new double[1];
			d[0] = ((Integer) cont).doubleValue();
			return d;
		}
		if (Xt == XT_ARRAY_INT) {
			int[] i = asIntArray();
			if (i == null)
				return null;
			double[] d = new double[i.length];
			int j = 0;
			while (j < i.length) {
				d[j] = (double) i[j];
				j++;
			}
			return d;
		}
		return null;
	}

	/**
	 * get content of the REXP as an array of integers. Unlike
	 * {@link #asDoubleArray} <u>NO</u> automatic conversion is done if the
	 * content is not an array of the correct type, because there is no
	 * canonical representation of doubles as integers. A single integer is
	 * returned as an array of the length 1. This method can be also used
	 * to access a logical array in its integer form (0=FALSE, 1=TRUE, 2=NA).
	 * 
	 * @return int[] content or <code>null</code> if the REXP is not a
	 *         array of integers
	 */
	public int[] asIntArray() {
		if (Xt == XT_ARRAY_INT || Xt == XT_ARRAY_BOOL_INT)
			return (int[]) cont;
		if (Xt == XT_INT) {
			int[] i = new int[1];
			i[0] = asInt();
			return i;
		}
		return null;
	}

	/**
	 * returns the content of the REXP as a matrix of doubles (2D-array:
	 * m[rows][cols]). This is the same form as used by popular math packages
	 * for Java, such as JAMA. This means that following leads to desired
	 * results:<br>
	 * <code>Matrix m=new Matrix(c.eval("matrix(c(1,2,3,4,5,6),2,3)").asDoubleMatrix());</code>
	 * 
	 * @return 2D array of doubles in the form double[rows][cols] or
	 *         <code>null</code> if the contents is no 2-dimensional matrix of
	 *         doubles
	 */
	public double[][] asDoubleMatrix() {
		double[] ct = asDoubleArray();
		if (ct==null) return null;
		REXP dim = getAttribute("dim");
		if (dim == null || dim.Xt != XT_ARRAY_INT)
			return null; // we need dimension attr
		int[] ds = dim.asIntArray();
		if (ds == null || ds.length != 2)
			return null; // matrix must be 2-dimensional
		int m = ds[0], n = ds[1];
		double[][] r = new double[m][n];
		if (ct == null)
			return null;
		// R stores matrices as matrix(c(1,2,3,4),2,2) = col1:(1,2), col2:(3,4)
		// we need to copy everything, since we create 2d array from 1d array
		int i = 0, k = 0;
		while (i < n) {
			int j = 0;
			while (j < m) {
				r[j++][i] = ct[k++];
			}
			i++;
		}
		return r;
	}

	/** this is just an alias for {@link #asDoubleMatrix()}. */
	public double[][] asMatrix() {
		return asDoubleMatrix();
	}

	/**
	 * displayable contents of the expression. The expression is traversed
	 * recursively if aggregation types are used (Vector, List, etc.)
	 * 
	 * @return String descriptive representation of the xpression
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer("[" + xtName(Xt) + " ");

		if (attr != null)
			sb.append("\nattr=" + attr + "\n ");
		if (Xt == XT_DOUBLE)
			sb.append((Double) cont);
		if (Xt == XT_INT)
			sb.append((Integer) cont);
		if (Xt == XT_BOOL)
			sb.append((RBool) cont);
		if (Xt == XT_FACTOR)
			sb.append((RFactor) cont);
		if (Xt == XT_ARRAY_DOUBLE) {
			double[] d = (double[]) cont;
			sb.append("(");
			for (int i = 0; i < d.length; i++) {
				sb.append(d[i]);
				if (i < d.length - 1)
					sb.append(", ");
				if (i == 99) {
					sb.append("... (" + (d.length - 100)
							+ " more values follow)");
					break;
				}
			}
			sb.append(")");
		}

		if (Xt == XT_ARRAY_INT) {
			int[] d = (int[]) cont;
			sb.append("(");
			for (int i = 0; i < d.length; i++) {
				sb.append(d[i]);
				if (i < d.length - 1)
					sb.append(", ");
				if (i == 99) {
					sb.append("... (" + (d.length - 100)
							+ " more values follow)");
					break;
				}
			}
			sb.append(")");
		}

		if (Xt == XT_ARRAY_BOOL) {
			RBool[] d = (RBool[]) cont;
			sb.append("(");
			for (int i = 0; i < d.length; i++) {
				sb.append(d[i]);
				if (i < d.length - 1)
					sb.append(", ");
			}
			sb.append(")");
		}

		if (Xt == XT_ARRAY_STR) {
			String[] d = (String[]) cont;
			sb.append("(");
			for (int i = 0; i < d.length; i++) {
				sb.append((d[i] == null) ? "NA" : ("\"" + d[i] + "\""));
				if (i < d.length - 1)
					sb.append(", ");
				if (i == 10 && d.length > 14) {
					sb.append("... (" + (d.length - 10) + " more values follow)");
					break;
				}
			}
			sb.append(")");
		}
		
		if (Xt == XT_VECTOR) {
			Vector v = (Vector) cont;
			sb.append("(");
			for (int i = 0; i < v.size(); i++) {
				sb.append(((REXP) v.elementAt(i)).toString());
				if (i < v.size() - 1)
					sb.append(", ");
			}
			sb.append(")");
		}

		if (Xt == XT_STR) {
			if (cont == null)
				sb.append("NA");
			else {
				sb.append("\"");
				sb.append((String) cont);
				sb.append("\"");
			}
		}

		if (Xt == XT_SYM) {
			sb.append((String) cont);
		}

		if (Xt == XT_LIST || Xt == XT_LANG) {
			RList l = (RList) cont;
			sb.append(l.head);
			sb.append(":");
			sb.append(l.tag);
			sb.append(",(");
			sb.append(l.body); 
			sb.append(")");
		}

		if (Xt == XT_NONE) {
			sb.append("{"+rtype+"}");
		}

		if (Xt == XT_UNKNOWN)
			sb.append((Integer) cont);
		sb.append("]");
		return sb.toString();
	}

	public static String quoteString(String s) {
		// this code uses API introdiced in 1.4 so it needs to be re-written for
		// earlier JDKs
		if (s.indexOf('\\') >= 0)
			s.replaceAll("\\", "\\\\");
		if (s.indexOf('"') >= 0)
			s.replaceAll("\"", "\\\"");
		return "\"" + s + "\"";
	}
	
    /** returns human-readable name of the xpression type as string. Arrays are denoted by a trailing asterisk (*).
	@param xt xpression type
	@return name of the xpression type */
    public static String xtName(int xt) {
		if (xt==XT_NULL) return "NULL";
		if (xt==XT_INT) return "INT";
		if (xt==XT_STR) return "STRING";
		if (xt==XT_DOUBLE) return "REAL";
		if (xt==XT_BOOL) return "BOOL";
		if (xt==XT_ARRAY_INT) return "INT*";
		if (xt==XT_ARRAY_STR) return "STRING*";
		if (xt==XT_ARRAY_DOUBLE) return "REAL*";
		if (xt==XT_ARRAY_BOOL) return "BOOL*";
		if (xt==XT_ARRAY_BOOL_INT) return "BOOLi*";
		if (xt==XT_SYM) return "SYMBOL";
		if (xt==XT_LANG) return "LANG";
		if (xt==XT_LIST) return "LIST";
		if (xt==XT_CLOS) return "CLOS";
		if (xt==XT_VECTOR) return "VECTOR";
		if (xt==XT_FACTOR) return "FACTOR";
		if (xt==XT_UNKNOWN) return "UNKNOWN";
		if (xt==XT_NONE) return "(SEXP)";
		return "<unknown "+xt+">";
    }	
}
