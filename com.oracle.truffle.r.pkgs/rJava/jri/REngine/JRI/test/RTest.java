import org.rosuda.REngine.*;

class TestException extends Exception {
	public TestException(String msg) { super(msg); }
}

// This is the same test as in Rserve but it's using JRI instead

public class RTest {
	public static void main(String[] args) {
		try { 
			// the simple initialization is done using
			// REngine eng = REngine.engineForClass("org.rosuda.REngine.JRI.JRIEngine");
			// but the one below allows us to see all output from R via REngineStdOutput()
			// However, it won't succeed if the engine doesn't support callbacks, so be prepared to fall back
			REngine eng = REngine.engineForClass("org.rosuda.REngine.JRI.JRIEngine", args, new REngineStdOutput(), false);

			if (args.length > 0 && args[0].equals("--debug")) { // --debug waits for <Enter> so a debugger can be attached
				System.out.println("R Version: " + eng.parseAndEval("R.version.string").asString());
				System.out.println("ok, connected, press <enter> to continue\n");
				System.in.read();
			}
			
			{
				System.out.println("* Test string and list retrieval");
				RList l = eng.parseAndEval("{d=data.frame(\"huhu\",c(11:20)); lapply(d,as.character)}").asList();
				int cols = l.size();
				int rows = l.at(0).length();
				String[][] s = new String[cols][];
				for (int i=0; i<cols; i++) s[i]=l.at(i).asStrings();
				System.out.println("PASSED");
			}
			
			{
				System.out.println("* Test NA/NaN support in double vectors...");
				double x[] = { 1.0, 0.5, REXPDouble.NA, Double.NaN, 3.5 };
				eng.assign("x",x);
				String nas = eng.parseAndEval("paste(capture.output(print(x)),collapse='\\n')").asString();
				System.out.println(nas);
				if (!nas.equals("[1] 1.0 0.5  NA NaN 3.5"))
					throw new TestException("NA/NaN assign+retrieve test failed");
				// regression: the inverse failed becasue Java screwed up the bits in the NA value
				REXP v = eng.parseAndEval("c(1.5, NA, NaN)");
				if (v == null || v.length() != 3)
					throw new TestException("NA/NaN double retrieve test failed");
				System.out.println("  v = "+v.toDebugString());
				boolean b[] = v.isNA();
				if (b == null || b.length != 3 || b[0] != false || b[1] != true || b[2] != false)
					throw new TestException("isNA() test on doubles failed");
				double d[] = v.asDoubles();
				if (Double.isNaN(d[0]) || !Double.isNaN(d[1]) || !Double.isNaN(d[2]))
					throw new TestException("Double.isNaN test on doubles failed");
				System.out.println("PASSED");
			}
			
			{
				System.out.println("* Test assigning of lists and vectors ...");
				RList l = new RList();
				l.put("a",new REXPInteger(new int[] { 0,1,2,3}));
				l.put("b",new REXPDouble(new double[] { 0.5,1.2,2.3,3.0}));
				System.out.println("  assign x=pairlist");
				eng.assign("x", new REXPList(l));
				System.out.println("  assign y=vector");
				eng.assign("y", new REXPGenericVector(l));
				System.out.println("  assign z=data.frame");
				eng.assign("z", REXP.createDataFrame(l));
				System.out.println("  pull all three back to Java");
				REXP x = eng.parseAndEval("x");
				System.out.println("  x = "+x);
				x = eng.parseAndEval("y");
				System.out.println("  y = "+x);
				x = eng.parseAndEval("z");
				System.out.println("  z = "+x);
				System.out.println("PASSED");
			}
			
			{ // regression: object bit was not set for Java-side generated objects before 0.5-3
				System.out.println("* Testing functionality of assembled S3 objects ...");
				// we have already assigned the data.frame in previous test, so we jsut re-use it
				REXP x = eng.parseAndEval("z[2,2]");
				System.out.println("  z[2,2] = " + x);
				if (x == null || x.length() != 1 || x.asDouble() != 1.2)
					throw new TestException("S3 object bit regression test failed");
				System.out.println("PASSED");
			}
			
			{ // this test does a pull and push of a data frame. It will fail when the S3 test above failed.
				System.out.println("* Testing pass-though capability for data.frames ...");
				REXP df = eng.parseAndEval("{data(iris); iris}");
				eng.assign("df", df);
				REXP x = eng.parseAndEval("identical(df, iris)");
				System.out.println("  identical(df, iris) = "+x);
				if (x == null || !x.isLogical() || x.length() != 1 || !((REXPLogical)x).isTRUE()[0])
					throw new TestException("Pass-through test for a data.frame failed");
				System.out.println("PASSED");
			}
			
            { // factors
                System.out.println("* Test support of factors");
                REXP f = eng.parseAndEval("factor(paste('F',as.integer(runif(20)*5),sep=''))");
				System.out.println("  f="+f);
                System.out.println("  isFactor: "+f.isFactor()+", asFactor: "+f.asFactor());
                if (!f.isFactor() || f.asFactor() == null) throw new TestException("factor test failed");
                System.out.println("  singe-level factor used to degenerate:");
                f = eng.parseAndEval("factor('foo')");
                System.out.println("  isFactor: "+f.isFactor()+", asFactor: "+f.asFactor());
                if (!f.isFactor() || f.asFactor() == null) throw new TestException("single factor test failed (not a factor)");
				if (!f.asFactor().at(0).equals("foo")) throw new TestException("single factor test failed (wrong value)");
                System.out.println("  test factors with null elements contents:");
				eng.assign("f", new REXPFactor(new RFactor(new String[] { "foo", "bar", "foo", "foo", null, "bar" })));
				f = eng.parseAndEval("f");
                if (!f.isFactor() || f.asFactor() == null) throw new TestException("factor assign-eval test failed (not a factor)");
				System.out.println("  f = "+f.asFactor());
				f = eng.parseAndEval("as.factor(c(1,'a','b',1,'b'))");
				System.out.println("  f = "+f);
                if (!f.isFactor() || f.asFactor() == null) throw new TestException("factor test failed (not a factor)");
				System.out.println("PASSED");
            }
			
			
			{
				System.out.println("* Lowess test");
				double x[] = eng.parseAndEval("rnorm(100)").asDoubles();
				double y[] = eng.parseAndEval("rnorm(100)").asDoubles();
				eng.assign("x", x);
				eng.assign("y", y);
				RList l = eng.parseAndEval("lowess(x,y)").asList();
				System.out.println("  "+l);
				x = l.at("x").asDoubles();
				y = l.at("y").asDoubles();
				System.out.println("PASSED");
			}
			
			{
				// multi-line expressions
				System.out.println("* Test multi-line expressions");
				System.out.print("    multi-line single expression");
				if (eng.parseAndEval("{ a=1:10\nb=11:20\nmean(b-a) }\n").asInteger()!=10)
					throw new TestException("multi-line test failed.");
				System.out.println("    : ok");
				
				System.out.print("    multitle expressions" ); 
				if (eng.parseAndEval("a=1:10; b=11:20; mean(b-a)").asInteger()!=10)
					throw new TestException("multi-expression test failed.");
				System.out.println("    : ok");
				
				System.out.print("    comment (0 expressions)" ); 
				if (! eng.parseAndEval("# comment").isNull() )
					throw new TestException("eval comment (zero expression) failed");
				System.out.println("    : ok");
				
				
				System.out.println("PASSED");
			}
			{
				System.out.println("* Matrix tests\n  matrix: create a matrix");
				int m=100, n=100;
				double[] mat=new double[m*n];
				int i=0;
				while (i<m*n) mat[i++]=i/100;
				System.out.println("  matrix: assign a matrix");
				eng.assign("m", mat);
				eng.parseAndEval("m<-matrix(m,"+m+","+n+")", null, false); // don't return the result - it has a similar effect to voidEval in Rserve
				System.out.println("matrix: cross-product");
				double[][] mr=eng.parseAndEval("crossprod(m,m)").asDoubleMatrix();
				System.out.println("PASSED");
			}
			
			{ /* NAs in character vectors are mapped to null references in String[] and vice versa. JRI 0.5-0 and older incorrectly returned "NA" instead of null */
				System.out.println("* Test handling of NAs in character vectors ('foo', NA, 'NA')");
				System.out.print("  push String[] with NAs: ");
				eng.assign("s", new String[] { "foo", null, "NA" });
				int nas[] = eng.parseAndEval("is.na(s)").asIntegers();
				for (int i = 0; i < nas.length; i++) System.out.print(nas[i] + " ");
				if (nas.length != 3 || nas[0] != REXPLogical.FALSE || nas[1] != REXPLogical.TRUE || nas[2] != REXPLogical.FALSE)
					throw new TestException("assigning null Strings as NAs has failed");
				System.out.println(" - OK");
				System.out.print("  pull String[] with NAs: ");
				String s[] = eng.parseAndEval("c('foo', NA, 'NA')").asStrings();
				for (int i = 0; i < s.length; i++) System.out.print(s[i] + " ");
				if (s.length != 3 || s[0] == null || s[1] != null || s[2] == null)
					throw new TestException("pulling Strings containin NAs has failed");
				System.out.println(" - OK");
				System.out.print("  compare pushed and constructed strings: ");
				if (eng.parseAndEval("identical(s, c('foo', NA, 'NA'))").asInteger() != REXPLogical.TRUE)
					throw new TestException("comparing Strings with NAs has failed");
				System.out.println(" - OK");
				System.out.print("  check isNA() for REXPString");
				boolean na[] = eng.parseAndEval("s").isNA();
				for (int i = 0; i < na.length; i++) System.out.print(" " + na[i]);
				if (na.length != 3 || na[0] || !na[1] || na[2])
					throw new TestException("isNA() test failed");
				System.out.println(" - OK");
				System.out.println("PASSED");
			}
			    
			
			{
				System.out.println("* Test serialization and raw vectors");
				byte[] b = eng.parseAndEval("serialize(ls, NULL, ascii=FALSE)").asBytes();
				System.out.println("  serialized ls is "+b.length+" bytes long");
				eng.assign("r", new REXPRaw(b));
				String[] s = eng.parseAndEval("unserialize(r)()").asStrings();
				System.out.println("  we have "+s.length+" items in the workspace");
				System.out.println("PASSED");
			}
			
			{
				System.out.println("* Test environment support");
				REXP x = eng.parseAndEval("new.env(parent=baseenv())");
				System.out.println("  new.env() = " + x);
				if (x == null || !x.isEnvironment()) throw new TestException("pull of an environemnt failed");
				REXPEnvironment e = (REXPEnvironment) x;
				e.assign("foo", new REXPString("bar"));
				x = e.get("foo");
				System.out.println("  get(\"foo\") = " + x);
				if (x == null || !x.isString() || !x.asString().equals("bar")) throw new TestException("assign/get in an environemnt failed");
				x = eng.newEnvironment(e, true);
				System.out.println("  eng.newEnvironment() = " + x);
				if (x == null || !x.isEnvironment()) throw new TestException("Java-side environment creation failed");
				x = ((REXPEnvironment)x).parent(true);
				System.out.println("  parent = " + x);
				if (x == null || !x.isEnvironment()) throw new TestException("parent environment pull failed");
				x = e.get("foo");
				System.out.println("  get(\"foo\",parent) = " + x);
				if (x == null || !x.isString() || !x.asString().equals("bar")) throw new TestException("get in the parent environemnt failed");
				System.out.println( "  " ) ; 
				eng.parseAndEval( "{ .env <- new.env(); .env$x <- 2 }" ) ;
				REXP env = eng.get(".env", null, false );
				x = eng.parseAndEval( "x+1", env, true ); 
				System.out.println( "  R> { .env <- new.env(); .env$x <- 2 }" ); 
				System.out.println( "  env = eng.get(\".env\", null, false )  " );
				System.out.print( "  parseAndEval( \"x+1\", env, true)" );
				if( !( x instanceof REXPDouble ) || x.asDouble() != 3.0 ) throw new TestException("eval within environment failed") ;
				System.out.println( "  == 3.0     : ok" ); 
				System.out.println("PASSED");
			}
			
			/* SU: wrap() tests removed since they didn't even compile ... */
			
			{
				System.out.println("* Test generation of exceptions");
				
				/* parse exceptions */
				String cmd = "rnorm(10))" ; // syntax error
				System.out.println("  eng.parse(\"rnorm(10))\", false )     ->  REngineException( \"Parse Error\" ) " ) ;
				boolean ok = false; 
				try{
					eng.parse( cmd, false ) ; 
				} catch( REngineException e){
					ok = true ; 
				}
				if( !ok ){
					throw new TestException( "parse did not generate an exception on syntax error" ) ; 
				}
				System.out.println("  eng.parseAndEval(\"rnorm(10))\" )     ->  REngineException( \"Parse Error\" ) " ) ;
				ok = false; 
				try{
					eng.parseAndEval( cmd ) ; 
				} catch( REngineException e){
					ok = true ; 
				}
				if( !ok ){
					throw new TestException( "parseAndEval did not generate an exception on syntax error" ) ; 
				}
				
				/* eval exceptions */
				cmd = "rnorm(5); stop('error'); rnorm(2)" ;
				System.out.print("  " + cmd  ) ;
				ok = false; 
				try{
					eng.parseAndEval( cmd ) ; 
				}	catch( REngineException e){
					if( e instanceof REngineEvalException ){
						ok = true ; 
					}
				}
				if( !ok ){
					throw new TestException( "error in R did not generate REngineEvalException" ) ; 
				}
				System.out.println( "   -> REngineEvalException  : ok" ) ;
					
				System.out.println("PASSED");
				
			}
			
			{
				System.out.println("* Test creation of references to java objects");
				if (!((REXPLogical)eng.parseAndEval("require(rJava)")).isTRUE()[0]) {
					System.out.println("  - rJava is not available, skipping test\n");
				} else if (!(eng instanceof org.rosuda.REngine.JRI.JRIEngine)) {
					System.out.println("  - the used engine is not JRIEngine, skipping test\n");
				} else {
					/* try to use rJava before it is initialized */
					System.out.print("  checking that rJava generate error if not yet loaded" ) ;
					boolean error = false; 
					try{
						eng.parseAndEval( "p <- .jnew( 'java/awt/Point' ) " ) ; 
					} catch( REngineException e){
						error = true ;
					}
					if( !error ){
						throw new TestException( "rJava not initiliazed, but did not generate error" ) ;
					}
					System.out.println( " : ok" ) ;
					
					eng.parseAndEval(".jinit()");
					REXPReference ref = ((org.rosuda.REngine.JRI.JRIEngine)eng).createRJavaRef( null );
					if( ref != null ){
						throw new TestException( "null object should create null REXPReference" ) ; 
					}
					System.out.println("  eng.createRJavaRef(null)     ->  null : ok" ) ;
					
					System.out.println( "  pushing a java.awt.Point to R " ) ;
					java.awt.Point p = new java.awt.Point( 10, 10) ;
					ref = ((org.rosuda.REngine.JRI.JRIEngine)eng).createRJavaRef( p ); 
					eng.assign( "p", ref ) ;
					String cmd = "exists('p') && inherits( p, 'jobjRef') && .jclass(p) == 'java.awt.Point' " ; 
					System.out.println( "  test if the object was pushed correctly " ) ;
					boolean ok = ((REXPLogical)eng.parseAndEval( cmd )).isTRUE()[0] ;
					if( !ok ){
						throw new TestException( "could not push java object to R" ) ;
					}
					System.out.println( "  R> " + cmd + "  :  ok " ) ;
				
					eng.parseAndEval( ".jcall( p, 'V', 'move', 20L, 20L )" ) ; 
					System.out.println("  manipulate the object " ) ;
					if( p.x != 20 || p.y != 20 ){
						throw new TestException( "not modified the java object with R" ) ; 
					}
					System.out.println("  R> .jcall( p, 'V', 'move', 20L, 20L )   -> p.x == 20 ,p.y == 20 : ok " ) ;
					
					/* bug #126, use of .jfield with guess of the return class using reflection */
					System.out.print("  using .jfield with reflection (bug #126)" ) ;
					eng.parseAndEval( ".jfield( p, , 'x')" ) ; /* used to crash the jvm; (not really - the code in the bgu just forgot to init rJava)  */
					System.out.println(" : ok " ) ;
				
					System.out.println("PASSED");
				}
			}
			
			
			/* setEncoding is Rserve's extension - with JRI you have to use UTF-8 locale so this test will fail unless run in UTF-8
			{ // string encoding test (will work with Rserve 0.5-3 and higher only)
				System.out.println("* Test string encoding support ...");
				String t = "ひらがな"; // hiragana (literally, in hiragana ;))
				eng.setStringEncoding("utf8"); 
				// -- Just in case the console is not UTF-8 don't display it
				//System.out.println("  unicode text: "+t);
				eng.assign("s", t);
				REXP x = eng.parseAndEval("nchar(s)");
				System.out.println("  nchar = " + x);
				if (x == null || !x.isInteger() || x.asInteger() != 4)
					throw new TestException("UTF-8 encoding string length test failed");
				// we cannot really test any other encoding ..
				System.out.println("PASSED");
			} */
			
			eng.close(); // close the engine connection
			
			System.out.println("Done.");
			
		} catch (REXPMismatchException me) {
			// some type is different from what you (the programmer) expected
			System.err.println("Type mismatch: "+me);
			me.printStackTrace();
			System.exit(1);
		} catch (REngineException ee) {
			// something went wring in the engine
			System.err.println("REngine exception: "+ee);
			ee.printStackTrace();
			System.exit(1);
		} catch (ClassNotFoundException cnfe) {
			// class not found is thrown by engineForClass
			System.err.println("Cannot find JRIEngine class - please fix your class path!\n"+cnfe);
			System.exit(1);
		} catch (Exception e) {
			// some other exception ...
			System.err.println("Exception: "+e);
			e.printStackTrace();
			System.exit(1);
		}
	}
}
