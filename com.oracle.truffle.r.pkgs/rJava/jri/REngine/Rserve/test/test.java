import org.rosuda.REngine.*;
import org.rosuda.REngine.Rserve.*;

class TestException extends Exception {
    public TestException(String msg) { super(msg); }
}

public class test {
    public static void main(String[] args) {
	try {
	    RConnection c = new RConnection();
	    // REngine is the backend-agnostic API -- using eng instead of c makes sure that we don't use Rserve extensions inadvertently
	    REngine eng = (REngine) c;

	    System.out.println(">>" + c.eval("R.version$version.string").asString() + "<<");

		{
			System.out.println("* Test string and list retrieval");
			RList l = c.eval("{d=data.frame(\"huhu\",c(11:20)); lapply(d,as.character)}").asList();
			int cols = l.size();
			int rows = l.at(0).length();
			String[][] s = new String[cols][];
			for (int i=0; i<cols; i++) s[i]=l.at(i).asStrings();
			System.out.println("PASSED");
		}
		
	    {
		System.out.println("* Test NA/NaN support in double vectors...");
		double R_NA = REXPDouble.NA;
		double x[] = { 1.0, 0.5, R_NA, Double.NaN, 3.5 };
		c.assign("x",x);
		String nas = c.eval("paste(capture.output(print(x)),collapse='\\n')").asString();
		System.out.println(nas);
		if (!nas.equals("[1] 1.0 0.5  NA NaN 3.5"))
		    throw new TestException("NA/NaN assign+retrieve test failed");
		REXP rx = c.eval("c(2.2, NA_real_, NaN)");
		if (rx == null || !rx.isNumeric() || rx.isInteger())
		    throw new TestException("NA/NaN pull test failed (invalid result)");
		boolean nal[] = rx.isNA();
		if (nal.length != 3 || nal[0] || !nal[1] || nal[2])
		    throw new TestException("NS/NAN pull test: NA pull failed");
		x = rx.asDoubles();
		if (x.length != 3 || !Double.isNaN(x[2]) || REXPDouble.isNA(x[2]) || !REXPDouble.isNA(x[1]))
		    throw new TestException("NS/NAN pull test: NA/NaN pull failed");		    
		System.out.println("PASSED");
	    }
		
	    {
			System.out.println("* Test assigning of lists and vectors ...");
			RList l = new RList();
			l.put("a",new REXPInteger(new int[] { 0,1,2,3}));
			l.put("b",new REXPDouble(new double[] { 0.5,1.2,2.3,3.0}));
			System.out.println("  assign x=pairlist");
			c.assign("x", new REXPList(l));
			System.out.println("  assign y=vector");
			c.assign("y", new REXPGenericVector(l));
			System.out.println("  assign z=data.frame");
			c.assign("z", REXP.createDataFrame(l));
			System.out.println("  pull all three back to Java");
			REXP x = c.parseAndEval("x");
			System.out.println("  x = "+x);
			x = c.eval("y");
			System.out.println("  y = "+x);
			x = c.eval("z");
			System.out.println("  z = "+x);
			System.out.println("PASSED");
	    }
		{
			System.out.println("* Test support for logicals ... ");
			System.out.println("  assign b={true,false,true}");
			c.assign("b", new REXPLogical(new boolean[] { true, false, true }));
			REXP x = c.parseAndEval("b");
			System.out.println("  " + ((x != null) ? x.toDebugString() : "NULL"));
			if (!x.isLogical() || x.length() != 3)
				throw new TestException("boolean array assign+retrieve test failed");
			boolean q[] = ((REXPLogical)x).isTRUE();
			if (q[0] != true || q[1] != false || q[2] != true)
				throw new TestException("boolean array assign+retrieve test failed (value mismatch)");
			System.out.println("  get c(TRUE,FLASE,NA)");
			x = c.parseAndEval("c(TRUE,FALSE,NA)");
			System.out.println("  " + ((x != null) ? x.toDebugString() : "NULL"));
			if (!x.isLogical() || x.length() != 3)
				throw new TestException("boolean array NA test failed");
			boolean q1[] = ((REXPLogical)x).isTRUE();
			boolean q2[] = ((REXPLogical)x).isFALSE();
			boolean q3[] = ((REXPLogical)x).isNA();
			if (q1[0] != true || q1[1] != false || q1[2] != false ||
				q2[0] != false || q2[1] != true || q2[2] != false ||
				q3[0] != false || q3[1] != false || q3[2] != true)
				throw new TestException("boolean array NA test failed (value mismatch)");
		}

		{ // regression: object bit was not set for Java-side generated objects before 0.5-3
			System.out.println("* Testing functionality of assembled S3 objects ...");
			// we have already assigned the data.frame in previous test, so we jsut re-use it
			REXP x = c.parseAndEval("z[2,2]");
			System.out.println("  z[2,2] = " + x);
			if (x == null || x.length() != 1 || x.asDouble() != 1.2)
				throw new TestException("S3 object bit regression test failed");
			System.out.println("PASSED");
		}
		
		{ // this test does a pull and push of a data frame. It will fail when the S3 test above failed.
			System.out.println("* Testing pass-though capability for data.frames ...");
			REXP df = c.parseAndEval("{data(iris); iris}");
			c.assign("df", df);
			REXP x = c.eval("identical(df, iris)");
			System.out.println("  identical(df, iris) = "+x);
			if (x == null || !x.isLogical() || x.length() != 1 || !((REXPLogical)x).isTrue()[0])
				throw new TestException("Pass-through test for a data.frame failed");
			System.out.println("PASSED");
		}
		
            { // factors
                System.out.println("* Test support of factors");
                REXP f = c.parseAndEval("factor(paste('F',as.integer(runif(20)*5),sep=''))");
				System.out.println("  f="+f);
                System.out.println("  isFactor: "+f.isFactor()+", asFactor: "+f.asFactor());
                if (!f.isFactor() || f.asFactor() == null) throw new TestException("factor test failed");
                System.out.println("  singe-level factor used to degenerate:");
                f = c.parseAndEval("factor('foo')");
                System.out.println("  isFactor: "+f.isFactor()+", asFactor: "+f.asFactor());
                if (!f.isFactor() || f.asFactor() == null) throw new TestException("single factor test failed (not a factor)");
				if (!f.asFactor().at(0).equals("foo")) throw new TestException("single factor test failed (wrong value)");
                System.out.println("  test factors with null elements contents:");
				c.assign("f", new REXPFactor(new RFactor(new String[] { "foo", "bar", "foo", "foo", null, "bar" })));
				f = c.parseAndEval("f");
                if (!f.isFactor() || f.asFactor() == null) throw new TestException("factor assign-eval test failed (not a factor)");
				System.out.println("  f = "+f.asFactor());
				f = c.parseAndEval("as.factor(c(1,'a','b',1,'b'))");
				System.out.println("  f = "+f);
                if (!f.isFactor() || f.asFactor() == null) throw new TestException("factor test failed (not a factor)");
				System.out.println("PASSED");
            }


	    {
			System.out.println("* Lowess test");
			double x[] = c.eval("rnorm(100)").asDoubles();
			double y[] = c.eval("rnorm(100)").asDoubles();
			c.assign("x", x);
			c.assign("y", y);
			RList l = c.parseAndEval("lowess(x,y)").asList();
			System.out.println("  "+l);
			x = l.at("x").asDoubles();
			y = l.at("y").asDoubles();
			System.out.println("PASSED");
		}

	    {
			// multi-line expressions
			System.out.println("* Test multi-line expressions");
			if (c.eval("{ a=1:10\nb=11:20\nmean(b-a) }\n").asInteger()!=10)
				throw new TestException("multi-line test failed.");
			System.out.println("PASSED");
	    }
		{
		    System.out.println("* Matrix tests\n  matrix: create a matrix");
		    int m = 100, n = 100;
		    double[] mat=new double[m * n];
		    { int i=0; while (i < m * n) mat[i++] = i / 100; }
		    System.out.println("  matrix: assign/retrieve a matrix");
		    c.assign("m", mat);
		    c.voidEval("m<-matrix(m,"+m+","+n+")");
		    System.out.println("  matrix: use createDoubleMatrix");
		    double[][] A = new double[m][n];
		    for (int i = 0; i < m; i++) for (int j = 0; j < n; j++) A[i][j] = mat[i + j * m];
		    c.assign("m2", REXP.createDoubleMatrix(A));
		    if (c.eval("identical(m ,m2)").asInteger() != 1)
			throw new TestException("matrix test failed: createDoubleMatrix result is not the same as direct assign");
		    System.out.println("matrix: cross-product");
		    double[][] mr = c.parseAndEval("crossprod(m,m)").asDoubleMatrix();
		    System.out.println("PASSED");
		}
		
		{
			System.out.println("* Test serialization and raw vectors");
			byte[] b = c.eval("serialize(ls, NULL, ascii=FALSE)").asBytes();
			System.out.println("  serialized ls is "+b.length+" bytes long");
			c.assign("r", new REXPRaw(b));
			String[] s = c.eval("unserialize(r)()").asStrings();
			System.out.println("  we have "+s.length+" items in the workspace");
			System.out.println("PASSED");
		}
		
		{ /* NAs in character vectors are mapped to null references in String[] and vice versa. Only Rserve 0.6-2 and later support NAa in character vectors. */
		  /* repression test: assigning empty string '' has failed in previous versions of RserveEngine */
			System.out.println("* Test handling of NAs in character vectors ('foo', '', NA, 'NA')");
			System.out.print("  push String[] with NAs: ");
			eng.assign("s", new String[] { "foo", "", null, "NA" });
			int nas[] = eng.parseAndEval("is.na(s)").asIntegers();
			for (int i = 0; i < nas.length; i++) System.out.print(nas[i] + " ");
			if (nas.length != 4 || nas[0] != REXPLogical.FALSE || nas[1] != REXPLogical.FALSE || nas[2] != REXPLogical.TRUE || nas[3] != REXPLogical.FALSE)
				throw new TestException("assigning null Strings as NAs and '' has failed");
			System.out.println(" - OK");
			System.out.print("  pull String[] with NAs: ");
			String s[] = eng.parseAndEval("c('foo', '', NA, 'NA')").asStrings();
			for (int i = 0; i < s.length; i++) System.out.print("'" + s[i] + "' ");
			if (s.length != 4 || s[0] == null || s[1] == null || !s[1].equals("") || s[2] != null || s[3] == null)
				throw new TestException("pulling Strings containin NAs and '' has failed");
			System.out.println(" - OK");
			System.out.print("  compare pushed and constructed strings: ");
			if (eng.parseAndEval("identical(s, c('foo', '', NA, 'NA'))").asInteger() != REXPLogical.TRUE)
				throw new TestException("comparing Strings with NAs and '' has failed");
			System.out.println(" - OK");
			System.out.print("  check isNA() for REXPString:");
			boolean na[] = eng.parseAndEval("s").isNA();
			for (int i = 0; i < na.length; i++) System.out.print(" " + na[i]);
			if (na.length != 4 || na[0] || na[1] || !na[2] || na[3])
				throw new TestException("isNA() test failed");
			System.out.println(" - OK");
			System.out.println("PASSED");
		}
		
		{ // string encoding test (will work with Rserve 0.5-3 and higher only)
			System.out.println("* Test string encoding support ...");
			String t = "ひらがな"; // hiragana (literally, in hiragana ;))
			c.setStringEncoding("utf8");
			// -- Just in case the console is not UTF-8 don't display it
			//System.out.println("  unicode text: "+t);
			c.assign("s", t);
			REXP x = c.parseAndEval("nchar(s)");
			System.out.println("  nchar = " + x);
			if (x == null || !x.isInteger() || x.asInteger() != 4)
				throw new TestException("UTF-8 encoding string length test failed");
			// we cannot really test any other encoding ..
			System.out.println("PASSED");
		}

		{ // test QAP evals
			System.out.println("* Test eval without parse (direct calls) ...");
			System.out.println("  call 1L + 2 as a language construct");
			REXP x = c.eval(REXP.asCall("+",
						    new REXPInteger(1),
						    new REXPDouble(2)), null, true);
			if (x == null || !x.isNumeric() || x.asDouble() != 3)
				throw new TestException("evaluating 1L + 2 as a call failed");
			System.out.println("  call a compound statement and exported symbol");
			x = c.eval(REXP.asCall("{", new REXP[] {
						// X <- local vector 1,2,3,4
						REXP.asCall("<-", new REXPSymbol("X"), new REXPInteger(new int[] { 1, 2, 3, 4 })),
						// base::length(X)  # convoluted for the sake of testing exported symbols
						REXP.asCall(REXP.asCall("::", new REXPSymbol("base"), new REXPSymbol("length")),
							    new REXPSymbol("X"))
					}), null, true);
			if (x == null || !x.isInteger() || x.asInteger() != 4)
				throw new TestException("calling a compound statement failed");
			// since we did an assignment we mauy as well test the get() method
			System.out.println("  get from global env");
			x = c.get("X", null, true);
			if (x == null || !x.isInteger() || x.length() != 4)
				throw new TestException("retrieving value created by assignment expression call failed");
			System.out.println("PASSED");
		}
		
		{ // test control commands (works only when enabled and in Rserve 0.6-0 and higher only) - must be the last test since it closes the connection and shuts down the server
			System.out.println("* Test control commands (this will fail if control commands are disabled) ...");
			System.out.println("  server eval");
			String key = "rn" + Math.random(); // generate a random number to prevent contamination from previous tests
			c.serverEval("xXx<-'" + key + "'");
			c.close();
			c = new RConnection();
			REXP x = c.eval("xXx");
			if (x == null || !x.isString() || x.length() != 1 || !x.asString().equals(key))
				throw new TestException("control eval test failed - assignment was not persistent");
			c.serverEval("rm(xXx)"); // remove the test variable to not pollute the global workspace
			System.out.println("  server shutdown");
			c.serverShutdown();
			c.close();
			System.out.println("PASSED");
		}
	    
		} catch (RserveException rse) {
	    System.out.println(rse);
	} catch (REXPMismatchException mme) {
	    System.out.println(mme);
	    mme.printStackTrace();
        } catch(TestException te) {
            System.err.println("** Test failed: "+te.getMessage());
            te.printStackTrace();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
