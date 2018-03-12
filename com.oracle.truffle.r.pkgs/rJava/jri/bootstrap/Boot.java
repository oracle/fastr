import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.util.StringTokenizer;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.lang.reflect.Method;

public class Boot {
    public static String bootFile = null;

	public static String findInPath(String path, String fn) {
		StringTokenizer st = new StringTokenizer(path, File.pathSeparator);
		while (st.hasMoreTokens()) {
			String dirname=st.nextToken();
			try {
				File f = new File(dirname+File.separator+fn);
				if (f.isFile()) return f.getPath();
			} catch (Exception fex) {}
		}
		return null;
	}

	public static String findNativeLibrary(String basename, boolean internalFirst) {
		String libName = "lib"+basename;
		String ext = ".so";
		String os = System.getProperty("os.name");
		if (os.startsWith("Win")) {
			os = "Win";
			ext= ".dll";
			libName=basename;
		}
		if (os.startsWith("Mac")) {
			os = "Mac";
			ext= ".jnilib";
		}
		String fullName = libName+ext;

		// first, try the system path unless instructed otherwise
		if (!internalFirst) {
			try {
				String r = findInPath("."+File.pathSeparator+System.getProperty("java.library.path"),
									  fullName);
				if (r != null) return r;
			} catch (Exception ex1) {}
		}

		// second, try to locate in on the class path (in the JAR file or in one of the directories)
		String cp = System.getProperty("java.class.path");
		StringTokenizer st = new StringTokenizer(cp, File.pathSeparator);
		while (st.hasMoreTokens()) {
			String dirname=st.nextToken();
			try {
				File f = new File(dirname);
				if (f.isFile()) {
					// look in a JAR file and extract it if necessary
					ZipFile jf = new ZipFile(f);
					ZipEntry ze = jf.getEntry(fullName);
					if (ze != null) { // found it inside a JAR file						
						try {
						    bootFile = f.toString();
							File tf = File.createTempFile(basename,ext);
							System.out.println("Boot.findNativeLibrary: found in a JAR ("+jf+"), extracting into "+tf);
							InputStream zis = jf.getInputStream(ze);
							FileOutputStream fos = new FileOutputStream(tf);
							byte b[] = new byte[65536];
							while (zis.available()>0) {
								int n = zis.read(b);
								if (n>0) fos.write(b, 0, n);
							}
							zis.close();
							fos.close();
							tf.deleteOnExit();
							return tf.getPath();
						} catch (Exception foo) {
						}
					}
				} else if (f.isDirectory()) {
					File ff = new File(dirname+File.separator+fullName);
					if (ff.isFile()) return ff.getPath();
				}
			} catch(Exception ex2) {}
		}

		// third, try the system path if we didn't look there before
		if (internalFirst) {
			try {
				String r = findInPath("."+File.pathSeparator+System.getProperty("java.library.path"),
									  fullName);
				if (r != null) return r;
			} catch (Exception ex3) {}
		}
		return null;
	}

	public static void main(String[] args) {
		// 1) instantiate master class loader
		JRIClassLoader mcl = JRIClassLoader.getMainLoader();

		// 2) locate boot JNI library
		String nl = findNativeLibrary("boot", false);
		
		if (nl == null) {
			System.err.println("ERROR: Unable to locate native bootstrap library.");
			System.exit(1);
		}
		
		// register boot library with MCL
		mcl.registerLibrary("boot", new File(nl));

		// add path necessary for loading JRIBootstrap to MCL
		String cp = System.getProperty("java.class.path");
		StringTokenizer st = new StringTokenizer(cp, File.pathSeparator);
		while (st.hasMoreTokens()) {
		    String p = st.nextToken();
		    mcl.addClassPath(p);
		    // we assume that the first file on the CP is us (FIXME: verify this!)
		    if (bootFile==null && (new File(p)).isFile()) bootFile=p;
		}
		
		// call static bootstrap method
		try {
			// force the use of the MCL even if the system loader could find it
			Class stage2class = mcl.findAndLinkClass("JRIBootstrap");
			Method m = stage2class.getMethod("bootstrap", new Class[] { String[].class });
			m.invoke(null, new Object[] { args });
		} catch (Exception rtx) {
			System.err.println("ERROR: Unable to invoke bootstrap method in JRIBootstrap! ("+rtx+")");
			rtx.printStackTrace();
			System.exit(2);
		}
	}
}
