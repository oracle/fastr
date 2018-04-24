// RConsoleOutputStream, part of Java/R Interface
//
// (C)Copyright 2007 Simon Urbanek
//
// For licensing terms see LICENSE in the root if the JRI distribution

package org.rosuda.JRI;

import java.io.OutputStream;
import java.io.IOException;

/** RConsoleOutputStream provides an OutputStream which causes its output to be written to the R console. It is a pseudo-stream as there is no real descriptor connected to the R console and thusly it is legal to have multiple console streams open. The synchonization happens at the RNI level.<p>Note that stdout/stderr are not connected to the R console by default, so one way of using this stream is to re-route Java output to R console:<pre>
System.setOut(new PrintStream(new RConsoleOutputStream(engine, 0)));
System.setErr(new PrintStream(new RConsoleOutputStream(engine, 1)));
</pre>

@since JRI 0.4-0
*/
public class RConsoleOutputStream extends OutputStream {
	Rengine eng;
	int oType;
	boolean isOpen;
	
	/** opens a new output stream to R console
		@param eng R engine
		@param oType output type (0=regular, 1=error/warning) */
	public RConsoleOutputStream(Rengine eng, int oType) {
		this.eng = eng;
		this.oType = oType;
		isOpen = true;
	}
	
	public void write(byte[] b, int off, int len) throws IOException {
		if (!isOpen) throw new IOException("cannot write to a closed stream");
		if (eng == null) throw new IOException("missing R engine");
		String s = new String(b, off, len);
		eng.rniPrint(s, oType);
	}
	
	public void write(byte[] b) throws IOException { write(b, 0, b.length); }
	public void write(int b) throws IOException { write(new byte[] { (byte)(b&255) }); }
	public void close() throws IOException { isOpen=false; eng=null; }
}
