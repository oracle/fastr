// JRIEngine - REngine-based interface to JRI
// Copyright(c) 2009 Simon Urbanek
//
// Currently it uses low-level calls from org.rosuda.JRI.Rengine, but
// all REXP representations are created based on the org.rosuda.REngine API

package org.rosuda.REngine.JRI;

import org.rosuda.JRI.Rengine;
import org.rosuda.JRI.Mutex;
import org.rosuda.JRI.RMainLoopCallbacks;
import org.rosuda.REngine.*;

/** <code>JRIEngine</code> is a <code>REngine</code> implementation using JRI (Java/R Interface).
 <p>
 Note that at most one JRI instance can exist in a given JVM process, because R does not support multiple threads. <code>JRIEngine</code> itself is thread-safe, so it is possible to invoke its methods from any thread. However, this is achieved by serializing all entries into R, so be aware of possible deadlock conditions if your R code calls back into Java (<code>JRIEngine</code> is re-entrant from the same thread so deadlock issues can arise only with multiple threads inteacting thorugh R). */
public class JRIEngine extends REngine implements RMainLoopCallbacks {
	// internal R types as defined in Rinternals.h
	static final int NILSXP = 0; /* nil = NULL */
	static final int SYMSXP = 1; /* symbols */
	static final int LISTSXP = 2; /* lists of dotted pairs */
	static final int CLOSXP = 3; /* closures */
	static final int ENVSXP = 4; /* environments */
	static final int PROMSXP = 5; /* promises: [un]evaluated closure arguments */
	static final int LANGSXP = 6; /* language constructs */
	static final int SPECIALSXP = 7; /* special forms */
	static final int BUILTINSXP = 8; /* builtin non-special forms */
	static final int CHARSXP = 9; /* "scalar" string type (internal only) */
	static final int LGLSXP = 10; /* logical vectors */
	static final int INTSXP = 13; /* integer vectors */
	static final int REALSXP = 14; /* real variables */
	static final int CPLXSXP = 15; /* complex variables */
	static final int STRSXP = 16; /* string vectors */
	static final int DOTSXP = 17; /* dot-dot-dot object */
	static final int ANYSXP = 18; /* make "any" args work */
	static final int VECSXP = 19; /* generic vectors */
	static final int EXPRSXP = 20; /* expressions vectors */
	static final int BCODESXP = 21; /* byte code */
	static final int EXTPTRSXP = 22; /* external pointer */
	static final int WEAKREFSXP = 23; /* weak reference */
	static final int RAWSXP = 24; /* raw bytes */
	static final int S4SXP = 25; /* S4 object */
	
	/** minimal JRI API version that is required by this class in order to work properly (currently API 1.10, corresponding to JRI 0.5-1 or higher) */
	static public final long requiredAPIversion = 0x010a;
	
	/** currently running <code>JRIEngine</code> - there can be only one and we store it here. Essentially if it is <code>null</code> then R was not initialized. */
	static JRIEngine jriEngine = null;
	
	/** reference to the underlying low-level JRI (RNI) engine */
	Rengine rni = null;
	
	/** event loop callbacks associated with this engine. */
	REngineCallbacks callbacks = null;
	
	/** mutex synchronizing access to R through JRIEngine.<p> NOTE: only access through this class is synchronized. Any other access (e.g. using RNI directly) is NOT. */
	Mutex rniMutex = null;
	
	// cached pointers of special objects in R
	long R_UnboundValue, R_NilValue;
	
	/** special, global references */
	public REXPReference globalEnv, emptyEnv, baseEnv, nullValueRef;
	
	/** canonical NULL object */
	public REXPNull nullValue;

	/** class used for wrapping raw pointers such that they are adequately protected and released according to the lifespan of the Java object */
	class JRIPointer {
		long ptr;
		JRIPointer(long ptr, boolean preserve) {
			this.ptr = ptr;
			if (preserve && ptr != 0 && ptr != R_NilValue) {
				boolean obtainedLock = rniMutex.safeLock(); // this will inherently wait for R to become ready
				try {
					rni.rniPreserve(ptr);
				} finally {
					if (obtainedLock) rniMutex.unlock();
				}
			}
		}
		
		protected void finalize() throws Throwable {
			try {
				if (ptr != 0 && ptr != R_NilValue) {
					boolean obtainedLock = rniMutex.safeLock();
					try {
						rni.rniRelease(ptr);
					} finally {
						if (obtainedLock)
							rniMutex.unlock();
					}
				}
			} finally {
				super.finalize();
			}
		}	
		
		long pointer() { return ptr; }
	}
	
	/** factory method called by <code>engineForClass</code> 
	 @return new or current engine (new if there is none, current otherwise since R allows only one engine at any time) */
	public static REngine createEngine() throws REngineException {
		// there can only be one JRI engine in a process
		if (jriEngine == null)
			jriEngine = new JRIEngine();
		return jriEngine;
	}
	
	public static REngine createEngine(String[] args, REngineCallbacks callbacks, boolean runREPL) throws REngineException {
		if (jriEngine != null)
			throw new REngineException(jriEngine, "engine already running - cannot use extended constructor on a running instance");
		return jriEngine = new JRIEngine(args, callbacks, runREPL);
	}
	
	public Rengine getRni() {
		return rni;
	}
	
	/** default constructor - this constructor is also used via <code>createEngine</code> factory call and implies --no-save R argument, no callbacks and no REPL.
	 <p>This is equivalent to <code>JRIEngine(new String[] { "--no-save" }, null, false)</code> */
	public JRIEngine() throws REngineException {
		this(new String[] { "--no-save" }, (REngineCallbacks) null, false);
	}
	
	/** create <code>JRIEngine</code> with specified R command line arguments, no callbacks and no REPL.
	 <p>This is equivalent to <code>JRIEngine(args, null, false)</code> */
	public JRIEngine(String args[]) throws REngineException {
		this(args, (REngineCallbacks) null, false);
	}

	/** creates a JRI engine with specified delegate for callbacks (JRI compatibility mode ONLY!). The event loop is started if <Code>callbacks</code> in not <code>null</code>.
	 *  @param args arguments to pass to R (note that R usually requires something like <code>--no-save</code>!)
	 *  @param callbacks delegate class to process event loop callback from R or <code>null</code> if no event loop is desired 
	 **/
	public JRIEngine(String args[], RMainLoopCallbacks callbacks) throws REngineException {
		this(args, callbacks, (callbacks == null) ? false : true);
	}

	/** creates a JRI engine with specified delegate for callbacks
	 *  @param args arguments to pass to R (note that R usually requires something like <code>--no-save</code>!)
	 *  @param callback delegate class to process callbacks from R or <code>null</code> if no callbacks are desired
	 *  @param runREPL if set to <code>true</code> then the event loop (REPL) will be started, otherwise the engine is in direct operation mode.
	 */
	public JRIEngine(String args[], REngineCallbacks callbacks, boolean runREPL) throws REngineException {
		// if Rengine hasn't been able to load the native JRI library in its static 
		// initializer, throw an exception 
		if (!Rengine.jriLoaded)
			throw new REngineException (null, "Cannot load JRI native library");
		
		if (Rengine.getVersion() < requiredAPIversion)
			throw new REngineException(null, "JRI API version is too old, update rJava/JRI to match the REngine API");
		
		this.callbacks = callbacks;
		// the default modus operandi is without event loop and with --no-save option
		rni = new Rengine(args, runREPL, (callbacks == null) ? null : this);
		rniMutex = rni.getRsync();
		boolean obtainedLock = rniMutex.safeLock(); // this will inherently wait for R to become ready
		try {
			if (!rni.waitForR())
				throw(new REngineException(this, "Unable to initialize R"));
			if (rni.rniGetVersion() < requiredAPIversion)
				throw(new REngineException(this, "JRI API version is too old, update rJava/JRI to match the REngine API"));
			globalEnv = new REXPReference(this, new Long(rni.rniSpecialObject(Rengine.SO_GlobalEnv)));
			nullValueRef = new REXPReference(this, new Long(R_NilValue = rni.rniSpecialObject(Rengine.SO_NilValue)));
			emptyEnv = new REXPReference(this, new Long(rni.rniSpecialObject(Rengine.SO_EmptyEnv)));
			baseEnv = new REXPReference(this, new Long(rni.rniSpecialObject(Rengine.SO_BaseEnv)));
			nullValue = new REXPNull();
			R_UnboundValue = rni.rniSpecialObject(Rengine.SO_UnboundValue);
		} finally {
			if (obtainedLock) rniMutex.unlock();
		}
		// register ourself as the main and last engine
		lastEngine = this;
		if (jriEngine == null)
			jriEngine = this;
	}
	
	/** creates a JRI engine with specified delegate for callbacks (JRI compatibility mode ONLY! Will be deprecated soon!)
	 *  @param args arguments to pass to R (note that R usually requires something like <code>--no-save</code>!)
	 *  @param callback delegate class to process callbacks from R or <code>null</code> if no callbacks are desired
	 *  @param runREPL if set to <code>true</code> then the event loop (REPL) will be started, otherwise the engine is in direct operation mode.
	 */
	public JRIEngine(String args[], RMainLoopCallbacks callbacks, boolean runREPL) throws REngineException {
		// if Rengine hasn't been able to load the native JRI library in its static 
		// initializer, throw an exception 
		if (!Rengine.jriLoaded)
			throw new REngineException (null, "Cannot load JRI native library");
		
		if (Rengine.getVersion() < requiredAPIversion)
			throw new REngineException(null, "JRI API version is too old, update rJava/JRI to match the REngine API");
		// the default modus operandi is without event loop and with --no-save option
		rni = new Rengine(args, runREPL, callbacks);
		rniMutex = rni.getRsync();
		boolean obtainedLock = rniMutex.safeLock(); // this will inherently wait for R to become ready
		try {
			if (!rni.waitForR())
				throw(new REngineException(this, "Unable to initialize R"));
			if (rni.rniGetVersion() < requiredAPIversion)
				throw(new REngineException(this, "JRI API version is too old, update rJava/JRI to match the REngine API"));
			globalEnv = new REXPReference(this, new Long(rni.rniSpecialObject(Rengine.SO_GlobalEnv)));
			nullValueRef = new REXPReference(this, new Long(R_NilValue = rni.rniSpecialObject(Rengine.SO_NilValue)));
			emptyEnv = new REXPReference(this, new Long(rni.rniSpecialObject(Rengine.SO_EmptyEnv)));
			baseEnv = new REXPReference(this, new Long(rni.rniSpecialObject(Rengine.SO_BaseEnv)));
			nullValue = new REXPNull();
			R_UnboundValue = rni.rniSpecialObject(Rengine.SO_UnboundValue);
		} finally {
			if (obtainedLock) rniMutex.unlock();
		}
		// register ourself as the main and last engine
		lastEngine = this;
		if (jriEngine == null)
			jriEngine = this;
	}
	
	/** WARNING: legacy fallback for hooking from R into an existing Rengine - do NOT use for creating a new Rengine - it will go away eventually */
	public JRIEngine(Rengine eng) throws REngineException {
		// if Rengine hasn't been able to load the native JRI library in its static 
		// initializer, throw an exception 
		if (!Rengine.jriLoaded)
			throw new REngineException (null, "Cannot load JRI native library");

		rni = eng;
		if (rni.rniGetVersion() < 0x109)
			throw(new REngineException(this, "R JRI engine is too old - RNI API 1.9 (JRI 0.5) or newer is required"));
		rniMutex = rni.getRsync();
		boolean obtainedLock = rniMutex.safeLock();
		try {
			globalEnv = new REXPReference(this, new Long(rni.rniSpecialObject(Rengine.SO_GlobalEnv)));
			nullValueRef = new REXPReference(this, new Long(R_NilValue = rni.rniSpecialObject(Rengine.SO_NilValue)));
			emptyEnv = new REXPReference(this, new Long(rni.rniSpecialObject(Rengine.SO_EmptyEnv)));
			baseEnv = new REXPReference(this, new Long(rni.rniSpecialObject(Rengine.SO_BaseEnv)));
			nullValue = new REXPNull();
			R_UnboundValue = rni.rniSpecialObject(Rengine.SO_UnboundValue);
		} finally {
			if (obtainedLock) rniMutex.unlock();
		}
		// register ourself as the main and last engine
		lastEngine = this;
		if (jriEngine == null)
			jriEngine = this;
	}
	
	public REXP parse(String text, boolean resolve) throws REngineException {
		REXP ref = null;
		boolean obtainedLock = rniMutex.safeLock();
		try {
			long pr = rni.rniParse(text, -1);
			if (pr == 0 || pr == R_NilValue) throw(new REngineException(this, "Parse error"));
			rni.rniPreserve(pr);
			ref = new REXPReference(this, new Long(pr));
			if (resolve)
				try { ref = resolveReference(ref); } catch (REXPMismatchException me) { };
		} finally {
			if (obtainedLock)
				rniMutex.unlock();
		}
		return ref;
	}

	public REXP eval(REXP what, REXP where, boolean resolve) throws REngineException, REXPMismatchException {
		REXP ref = null;
		long rho = 0;
		if (where != null && !where.isReference()) {
			if (!where.isEnvironment() || ((REXPEnvironment)where).getHandle() == null)
				throw(new REXPMismatchException(where, "environment"));
			else
				rho = ((JRIPointer)((REXPEnvironment)where).getHandle()).pointer();
		} else
			if (where != null) rho = ((Long)((REXPReference)where).getHandle()).longValue();
		if (what == null) throw(new REngineException(this, "null object to evaluate"));
		if (!what.isReference()) {
			if (what.isExpression() || what.isLanguage())
				what = createReference(what);
			else
				throw(new REXPMismatchException(where, "reference, expression or language"));
		}
		boolean obtainedLock = rniMutex.safeLock();
		try {
			long pr = rni.rniEval(((Long)((REXPReference)what).getHandle()).longValue(), rho);
			if (pr == 0) // rniEval() signals error by passing 0
				throw new REngineEvalException(this, "error during evaluation", REngineEvalException.ERROR) ;
			rni.rniPreserve(pr);
			ref = new REXPReference(this, new Long(pr));
			if (resolve)
				ref = resolveReference(ref);
		} finally {
			if (obtainedLock)
				rniMutex.unlock();
		}
		return ref;
	}

	public void assign(String symbol, REXP value, REXP env) throws REngineException, REXPMismatchException {
		long rho = 0;
		if (env != null && !env.isReference()) {
			if (!env.isEnvironment() || ((REXPEnvironment)env).getHandle() == null)
				throw(new REXPMismatchException(env, "environment"));
			else
				rho = ((JRIPointer)((REXPEnvironment)env).getHandle()).pointer();
		} else
			if (env != null) rho = ((Long)((REXPReference)env).getHandle()).longValue();
		if (value == null) value = nullValueRef;
		if (!value.isReference())
			value = createReference(value); // if value is not a reference, we have to create one
		boolean obtainedLock = rniMutex.safeLock(), succeeded = false;
		try {
			succeeded = rni.rniAssign(symbol, ((Long)((REXPReference)value).getHandle()).longValue(), rho);
		} finally {
			if (obtainedLock)
				rniMutex.unlock();
		}
		if (!succeeded)
			throw new REngineException(this, "assign failed (probably locked binding");
	}
	
	public REXP get(String symbol, REXP env, boolean resolve) throws REngineException, REXPMismatchException {
		REXP ref = null;
		long rho = 0;
		if (env != null && !env.isReference()) {
			if (!env.isEnvironment() || ((REXPEnvironment)env).getHandle() == null)
				throw(new REXPMismatchException(env, "environment"));
			else
				rho = ((JRIPointer)((REXPEnvironment)env).getHandle()).pointer();
		} else
			if (env != null) rho = ((Long)((REXPReference)env).getHandle()).longValue();
		boolean obtainedLock = rniMutex.safeLock();
		try {
			long pr = rni.rniFindVar(symbol, rho);
			if (pr == R_UnboundValue || pr == 0) return null;
			rni.rniPreserve(pr);
			ref = new REXPReference(this, new Long(pr));
			if (resolve)
				try { ref = resolveReference(ref); } catch (REXPMismatchException me) { };
		} finally {
			if (obtainedLock)
				rniMutex.unlock();
		}
		return ref;
	}

	public REXP resolveReference(REXP ref) throws REngineException, REXPMismatchException {
		REXP res = null;
		if (ref == null) throw(new REngineException(this, "resolveReference called on NULL input"));
		if (!ref.isReference()) throw(new REXPMismatchException(ref, "reference"));
		long ptr = ((Long)((REXPReference)ref).getHandle()).longValue();
		if (ptr == 0) return nullValue;
		return resolvePointer(ptr);
	}

	/** 
	 * Turn an R pointer (long) into a REXP object.
	 * 
	 * This is the actual implementation of <code>resolveReference</code> but it works directly on the long pointers to be more efficient when performing recursive de-referencing */
	REXP resolvePointer(long ptr) throws REngineException, REXPMismatchException {
		if (ptr == 0) return nullValue;
		REXP res = null;
		boolean obtainedLock = rniMutex.safeLock();
		try {
			int xt = rni.rniExpType(ptr);
			String an[] = rni.rniGetAttrNames(ptr);
			REXPList attrs = null;
			if (an != null && an.length > 0) { // are there attributes? Then we need to resolve them first
				// we allow special handling for Java references so we need the class and jobj
				long jobj = 0;
				String oclass = null;
				RList attl = new RList();
				for (int i = 0; i < an.length; i++) {
					long aptr = rni.rniGetAttr(ptr, an[i]);
					if (aptr != 0 && aptr != R_NilValue) {
						if (an[i].equals("jobj")) jobj = aptr;
						REXP av = resolvePointer(aptr);
						if (av != null && av != nullValue) {
							attl.put(an[i], av);
							if (an[i].equals("class") && av.isString())
								oclass = av.asString();
						}
					}
				}
				if (attl.size() > 0)
					attrs = new REXPList(attl);
				// FIXME: in general, we could allow arbitrary convertors here ...
				// Note that the jobj hack is only needed because we don't support EXTPTRSXP conversion
				// (for a good reason - we can't separate the PTR from the R object so the only way it can
				// live is as a reference and we don't want resolvePointer to ever return REXPReference as
				// that could trigger infinite recursions), but if we did, we could allow post-processing
				// based on the class attribute on the converted REXP.. (better, we can leverage REXPUnknown
				// and pass the ptr to the convertor so it can pull things like EXTPTR via rni)
				if (jobj != 0 && oclass != null &&
				    (oclass.equals("jobjRef") ||
				     oclass.equals("jarrayRef") ||
				     oclass.equals("jrectRef")))
					return new REXPJavaReference(rni.rniXrefToJava(jobj), attrs);
			}
			switch (xt) {
				case NILSXP:
					return nullValue;
					
				case STRSXP:
					String[] s = rni.rniGetStringArray(ptr);
					res = new REXPString(s, attrs);
					break;
					
				case INTSXP:
					if (rni.rniInherits(ptr, "factor")) {
						long levx = rni.rniGetAttr(ptr, "levels");
						if (levx != 0) {
							String[] levels = null;
							// we're using low-level calls here (FIXME?)
							int rlt = rni.rniExpType(levx);
							if (rlt == STRSXP) {
								levels = rni.rniGetStringArray(levx);
								int[] ids = rni.rniGetIntArray(ptr);
								res = new REXPFactor(ids, levels, attrs);
							}
						}
					}
					// if it's not a factor, then we use int[] instead
					if (res == null)
						res = new REXPInteger(rni.rniGetIntArray(ptr), attrs);
					break;
					
				case REALSXP:
					res = new REXPDouble(rni.rniGetDoubleArray(ptr), attrs);
					break;
					
				case LGLSXP:
				{
					int ba[] = rni.rniGetBoolArrayI(ptr);
					byte b[] = new byte[ba.length];
					for (int i = 0; i < ba.length; i++)
						b[i] = (ba[i] == 0 || ba[i] == 1) ? (byte) ba[i] : REXPLogical.NA;
					res = new REXPLogical(b, attrs);
				}
					break;
					
				case VECSXP:
				{
					long l[] = rni.rniGetVector(ptr);
					REXP rl[] = new REXP[l.length];
					long na = rni.rniGetAttr(ptr, "names");
					String[] names = null;
					if (na != 0 && rni.rniExpType(na) == STRSXP)
						names = rni.rniGetStringArray(na);
					for (int i = 0; i < l.length; i++)
						rl[i] = resolvePointer(l[i]);
					RList list = (names == null) ? new RList(rl) : new RList(rl, names);
					res = new REXPGenericVector(list, attrs);
				}
					break;
					
				case RAWSXP:
					res = new REXPRaw(rni.rniGetRawArray(ptr), attrs);
					break;
					
				case LISTSXP:
				case LANGSXP:
				{
					RList l = new RList();
					// we need to plow through the list iteratively - the recursion occurs at the value level
					long cdr = ptr;
					while (cdr != 0 && cdr != R_NilValue) {
						long car = rni.rniCAR(cdr);
						long tag = rni.rniTAG(cdr);
						String name = null;
						if (rni.rniExpType(tag) == SYMSXP)
							name = rni.rniGetSymbolName(tag);
						REXP val = resolvePointer(car);
						if (name == null) l.add(val); else l.put(name, val);
						cdr = rni.rniCDR(cdr);
					}
					res = (xt == LANGSXP) ? new REXPLanguage(l, attrs) : new REXPList(l, attrs);
				}
					break;
					
				case SYMSXP:
					res = new REXPSymbol(rni.rniGetSymbolName(ptr));
					break;
					
				case ENVSXP:
					if (ptr != 0) rni.rniPreserve(ptr);
					res = new REXPEnvironment(this, new JRIPointer(ptr, false));
					break;
					
				case S4SXP:
					res = new REXPS4(attrs);
					break;
					
				default:
					res = new REXPUnknown(xt, attrs);
					break;
			}
		} finally {
			if (obtainedLock)
				rniMutex.unlock();
		}
		return res;
	}

	public REXP createReference(REXP value) throws REngineException, REXPMismatchException {
		if (value == null) throw(new REngineException(this, "createReference from a NULL value"));
		if (value.isReference()) return value;
		long ptr = createReferencePointer(value);
		if (ptr == 0) return null;
		boolean obtainedLock = rniMutex.safeLock();
		try {
			rni.rniPreserve(ptr);
		} finally {
			if (obtainedLock)
				rniMutex.unlock();
		}
		return new REXPReference(this, new Long(ptr));
	}
	
	/** 
	 * Create an R object, returning its pointer, from an REXP java object.
	 * 
	 * @param value
	 * @return long R pointer
	 * @throws REngineException if any of the RNI calls fails
	 * @throws REXPMismatchException only if some internal inconsistency happens. The internal logic should prevent invalid access to valid objects.
	 */
	long createReferencePointer(REXP value) throws REngineException, REXPMismatchException {
		if (value.isReference()) { // if it's reference, return the handle if it's from this engine
			REXPReference vref = (REXPReference) value;
			if (vref.getEngine() != this)
				throw new REXPMismatchException(value, "reference (cross-engine reference is invalid)");
			return ((Long)vref.getHandle()).longValue();
		}
		boolean obtainedLock = rniMutex.safeLock();
		int upp = 0;
		try {
			long ptr = 0;
			if (value.isNull()) // NULL cannot have attributes, hence get out right away
				return R_NilValue;
			else if (value.isLogical()) {
				int v[] = value.asIntegers();
				for (int i = 0; i < v.length; i++)
					v[i] = (v[i] < 0) ? 2 : ((v[i] == 0) ? 0 : 1); // convert to logical NAs as used by R
				ptr = rni.rniPutBoolArrayI(v);
			}
			else if (value.isInteger())
				ptr = rni.rniPutIntArray(value.asIntegers());
			else if (value.isRaw())
				ptr = rni.rniPutRawArray(value.asBytes());
			else if (value.isNumeric())
				ptr = rni.rniPutDoubleArray(value.asDoubles());
			else if (value.isString())
				ptr = rni.rniPutStringArray(value.asStrings());
			else if (value.isEnvironment()) {
				JRIPointer l = (JRIPointer) ((REXPEnvironment)value).getHandle();
				if (l == null) { // no associated reference, create a new environemnt
					long p = rni.rniParse("new.env(parent=baseenv())", 1);
					ptr = rni.rniEval(p, 0);
					/* TODO: should we handle REngineEvalException.ERROR and REngineEvalException.INVALID_INPUT here, for completeness */
				} else
					ptr = l.pointer();
			} else if (value.isPairList()) { // LISTSXP / LANGSXP
				boolean lang = value.isLanguage();
				RList rl = value.asList();
				ptr = R_NilValue;
				int j = rl.size();
				if (j == 0)
					ptr = rni.rniCons(R_NilValue, 0, 0, lang);
				else
					// we are in a somewhat unfortunate situation because we cannot append to the list (RNI has no rniSetCDR!) so we have to use Preserve and bulild the list backwards which may be a bit slower ...
					for (int i = j - 1; i >= 0; i--) {
						REXP v = rl.at(i);
						String n = rl.keyAt(i);
						long sn = 0;
						if (n != null) sn = rni.rniInstallSymbol(n);
						long vptr = createReferencePointer(v);
						if (vptr == 0) vptr = R_NilValue;
						long ent = rni.rniCons(vptr, ptr, sn, (i == 0) && lang); /* only the head should be LANGSXP I think - verify ... */
						rni.rniPreserve(ent); // preserve current head
						rni.rniRelease(ptr); // release previous head (since it's part of the new one already)
						ptr = ent;
					}
			} else if (value.isList()) { // VECSXP
				int init_upp = upp;
				RList rl = value.asList();
				long xl[] = new long[rl.size()];
				for (int i = 0; i < xl.length; i++) {
					REXP rv = rl.at(i);
					if (rv == null || rv.isNull())
						xl[i] = R_NilValue;
					else {
						long lv = createReferencePointer(rv);
						if (lv != 0 && lv != R_NilValue) {
							rni.rniProtect(lv);
							upp++;
						} else lv = R_NilValue;
						xl[i] = lv;
					}
				}
				ptr = rni.rniPutVector(xl);
				if (init_upp > upp) {
					rni.rniUnprotect(upp - init_upp);
					upp = init_upp;
				}
			} else if (value.isSymbol())
				return rni.rniInstallSymbol(value.asString()); // symbols need no attribute handling, hence get out right away
			else if (value instanceof REXPJavaReference) { // we wrap Java references by calling new("jobjRef", ...)
				Object jval = ((REXPJavaReference)value).getObject();
				long jobj = rni.rniJavaToXref(jval);
				rni.rniProtect(jobj);
				long jobj_sym = rni.rniInstallSymbol("jobj");
				long jclass_sym = rni.rniInstallSymbol("jclass");
				String clname = "java/lang/Object";
				if (jval != null) {
					clname = jval.getClass().getName();
					clname = clname.replace('.', '/');
				}
				long jclass = rni.rniPutString(clname);
				rni.rniProtect(jclass);
				long jobjRef = rni.rniPutString("jobjRef");
				rni.rniProtect(jobjRef);
				long ro = rni.rniEval(rni.rniLCons(rni.rniInstallSymbol("new"),
								   rni.rniCons(jobjRef,
									       rni.rniCons(jobj, 
											   rni.rniCons(jclass, R_NilValue, jclass_sym, false),
											   jobj_sym, false))
								   ), 0);
				rni.rniUnprotect(3);
				ptr = ro;
			}
			if (ptr == R_NilValue)
				return ptr;
			if (ptr != 0) {
				REXPList att = value._attr();
				if (att == null || !att.isPairList()) return ptr; // no valid attributes? the we're done
				RList al = att.asList();
				if (al == null || al.size() < 1 || !al.isNamed()) return ptr; // again - no valid list, get out
				rni.rniProtect(ptr); // symbols and other exotic creatures are already out by now, so it's ok to protect
				upp++;
				for (int i = 0; i < al.size(); i++) {
					REXP v = al.at(i);
					String n = al.keyAt(i);
					if (n != null) {
						long vptr = createReferencePointer(v);
						if (vptr != 0 && vptr != R_NilValue)
							rni.rniSetAttr(ptr, n, vptr);
					}
				}
				return ptr;
			}
		} finally {
			if (upp > 0)
				rni.rniUnprotect(upp);
			if (obtainedLock)
				rniMutex.unlock();
		}
		// we fall thgough here if the object cannot be handled or something went wrong
		return 0;
	}
	
	public void finalizeReference(REXP ref) throws REngineException, REXPMismatchException {
		if (ref != null && ref.isReference()) {
			long ptr = ((Long)((REXPReference)ref).getHandle()).longValue();
			boolean obtainedLock = rniMutex.safeLock();
			try {
				rni.rniRelease(ptr);
			} finally {
				if (obtainedLock)
					rniMutex.unlock();
			}
		}
	}

	public REXP getParentEnvironment(REXP env, boolean resolve) throws REngineException, REXPMismatchException {
		REXP ref = null;
		long rho = 0;
		if (env != null && !env.isReference()) {
			if (!env.isEnvironment() || ((REXPEnvironment)env).getHandle() == null)
				throw(new REXPMismatchException(env, "environment"));
			else
				rho = ((JRIPointer)((REXPEnvironment)env).getHandle()).pointer();
		} else
			if (env != null) rho = ((Long)((REXPReference)env).getHandle()).longValue();
		boolean obtainedLock = rniMutex.safeLock();
		try {
			long pr = rni.rniParentEnv(rho);
			if (pr == 0 || pr == R_NilValue) return null; // this should never happen, really
			rni.rniPreserve(pr);
			ref = new REXPReference(this, new Long(pr));
			if (resolve)
				ref = resolveReference(ref);
		} finally {
			if (obtainedLock)
				rniMutex.unlock();
		}
		return ref;
	}

	public REXP newEnvironment(REXP parent, boolean resolve) throws REXPMismatchException, REngineException {
		REXP ref = null;
		boolean obtainedLock = rniMutex.safeLock();
		try {
			long rho = 0;
			if (parent != null && !parent.isReference()) {
				if (!parent.isEnvironment() || ((REXPEnvironment)parent).getHandle() == null)
					throw(new REXPMismatchException(parent, "environment"));
				else
					rho = ((JRIPointer)((REXPEnvironment)parent).getHandle()).pointer();
			} else
				if (parent != null) rho = ((Long)((REXPReference)parent).getHandle()).longValue();
			if (rho == 0)
				rho = ((Long)((REXPReference)globalEnv).getHandle()).longValue();
			long p = rni.rniEval(rni.rniLCons(rni.rniInstallSymbol("new.env"), rni.rniCons(rho, R_NilValue, rni.rniInstallSymbol("parent"), false)), 0);
			/* TODO: should we handle REngineEvalException.INVALID_INPUT and REngineEvalException.ERROR here, for completeness */
			if (p != 0) rni.rniPreserve(p);
			ref = new REXPReference(this, new Long(p));
			if (resolve)
				ref = resolveReference(ref);
		} finally {
			if (obtainedLock)
				rniMutex.unlock();
		}
		return ref;
	}

	public boolean close() {
		if (rni == null) return false;
		rni.end();
		return true;
	}

	/** attempts to obtain a lock for this R engine synchronously (without waiting for it).
	 @return 0 if the lock could not be obtained (R is busy) and some other value otherwise (1 = lock obtained, 2 = the current thread already holds a lock) -- the returned value must be used in a matching call to {@link #unlock(int)}. */
	public synchronized int tryLock() {
		int res = rniMutex.tryLock();
		return (res == 1) ? 0 : ((res == -1) ? 2 : 1);
	}
	
	/** obains a lock for this R engine, waiting until it becomes available.
	 @return value that must be passed to {@link #unlock} in order to release the lock */
	public synchronized int lock() {
		return rniMutex.safeLock() ? 1 : 2;
	}
	
	/** releases a lock previously obtained by {@link #lock()} or {@link #tryLock()}.
	 @param lockValue value returned by {@link #lock()} or {@link #tryLock()}. */	 
	public synchronized void unlock(int lockValue) {
		if (lockValue == 1) rniMutex.unlock();
	}
	
	public boolean supportsReferences() { return true; }
	public boolean supportsEnvironments() { return true; }
	// public boolean supportsREPL() { return true; }
	public boolean supportsLocking() { return true; }
	
	/**
	 * creates a <code>jobjRef</code> reference in R via rJava.<br><b>Important:</b> rJava must be loaded and intialized in R (e.g. via <code>eval("{library(rJava);.jinit()}",false)</code>, otherwise this will fail. Requires rJava 0.4-13 or higher!
	 *
	 * @param o object to push to R
	 * 
	 * @return unresolved REXPReference of the newly created <code>jobjRef</code> object
	 *         or <code>null</code> upon failure
	 */
	public REXPReference createRJavaRef(Object o) throws REngineException {
		/* precaution */
		if( o == null ){
			return null ;
		}
		
		/* call Rengine api and make REXPReference from the result */
		REXPReference ref = null ; 
		boolean obtainedLock = rniMutex.safeLock();
		try {
			org.rosuda.JRI.REXP rx = rni.createRJavaRef( o );
			if( rx == null){
				throw new REngineException( this, "Could not push java Object to R" ) ; 
			} else{ 
				long p = rx.xp;
				rni.rniPreserve(p) ;
				ref = new REXPReference( this, new Long(p) ) ;
			}
		} finally {
			if (obtainedLock)
				rniMutex.unlock();
		}
		return ref ; 
	}
	
	/** JRI callbacks forwarding */
	public void   rWriteConsole (Rengine re, String text, int oType) {
		if (callbacks != null && callbacks instanceof REngineOutputInterface)
			((REngineOutputInterface)callbacks).RWriteConsole(this, text, oType);
	}
	
	public void   rBusy         (Rengine re, int which) {
		if (callbacks != null && callbacks instanceof REngineUIInterface)
			((REngineUIInterface)callbacks).RBusyState(this, which);
	}
	
	public synchronized String rReadConsole  (Rengine re, String prompt, int addToHistory) {
		if (callbacks != null && callbacks instanceof REngineInputInterface)
			return ((REngineInputInterface)callbacks).RReadConsole(this, prompt, addToHistory);
		
		try { wait(); } catch (Exception e) {}
		return "";
	}
	
	public void   rShowMessage  (Rengine re, String message) {
		if (callbacks != null && callbacks instanceof REngineOutputInterface)
			((REngineOutputInterface)callbacks).RShowMessage(this, message);
	}
	
	public String rChooseFile   (Rengine re, int newFile) {
		if (callbacks != null && callbacks instanceof REngineUIInterface)
			return ((REngineUIInterface)callbacks).RChooseFile(this, (newFile == 0));
		return null;
	}
	
	public void   rFlushConsole (Rengine re) {
		if (callbacks != null && callbacks instanceof REngineOutputInterface)
			((REngineOutputInterface)callbacks).RFlushConsole(this);
	}
	
	public void   rSaveHistory  (Rengine re, String filename) {
		if (callbacks != null && callbacks instanceof REngineConsoleHistoryInterface)
			((REngineConsoleHistoryInterface)callbacks).RSaveHistory(this, filename);
	}
	
	public void   rLoadHistory  (Rengine re, String filename) {
		if (callbacks != null && callbacks instanceof REngineConsoleHistoryInterface)
			((REngineConsoleHistoryInterface)callbacks).RLoadHistory(this, filename);
	}
}
