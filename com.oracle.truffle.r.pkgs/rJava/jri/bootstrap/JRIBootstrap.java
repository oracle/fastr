import java.io.File;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.StringTokenizer;

public class JRIBootstrap {
	//--- global constants ---
	public static final int HKLM = 0; // HKEY_LOCAL_MACHINE
	public static final int HKCU = 1; // HKEY_CURRENT_USER

	//--- native methods ---
	public static native String getenv(String var);
	public static native void setenv(String var, String val);

	public static native String regvalue(int root, String key, String value);
	public static native String[] regsubkeys(int root, String key);

	public static native String expand(String val);

	public static native boolean hasreg();

	public static native String arch();

	//--- helper methods ---	
	static void fail(String msg) {
		System.err.println("ERROR: "+msg);
		System.exit(1);
	}

        public static String findInPath(String path, String fn, boolean mustBeFile) {
                StringTokenizer st = new StringTokenizer(path, File.pathSeparator);
                while (st.hasMoreTokens()) {
                        String dirname=st.nextToken();
                        try {
                                File f = new File(dirname+File.separator+fn);
				System.out.println(" * "+f+" ("+f.exists()+", "+f.isFile()+")");
                                if (f.exists() && (!mustBeFile || f.isFile())) return f.getPath();
                        } catch (Exception fex) {}
                }
                return null;
        }

    // set ONLY after findR was run
    public static boolean isWin32 = false;
    public static boolean isMac = false;

    static String findR(boolean findAllSettings) {
	String ip = null;
	try {
	    if (hasreg()) {
		isWin32 = true;
		int rroot = HKLM;
		System.out.println("has registry, trying to find R");
		ip = regvalue(HKLM, "SOFTWARE\\R-core\\R","InstallPath");
		if (ip == null)
		    ip = regvalue(rroot=HKCU, "SOFTWARE\\R-core\\R","InstallPath");
		if (ip == null) {
		    System.out.println(" - InstallPath not present (possibly uninstalled R)");
		    String[] vers = regsubkeys(rroot=HKLM, "SOFTWARE\\R-core\\R");
		    if (vers == null)
			vers = regsubkeys(rroot=HKCU, "SOFTWARE\\R-core\\R");
		    if (vers!=null) {
			String lvn = ""; // FIXME: we compare versions lexicographically which may fail if we really reach R 2.10
			int i = 0; while (i<vers.length) {
			    if (vers[i] != null && lvn.compareTo(vers[i]) < 0)
				lvn = vers[i];
			    i++;
			}
			if (!lvn.equals(""))
			    ip = regvalue(rroot, "SOFTWARE\\R-core\\R\\"+lvn, "InstallPath");
		    }
		}
		if (ip == null) {
		    ip = getenv("R_HOME");
		    if (ip==null || ip.length()<1) ip = getenv("RHOME");
		    if (ip==null || ip.length()<1) ip=null;
		}
		if (ip != null) rs_home = ip;
		return ip;
	    }
	    isMac = System.getProperty("os.name").startsWith("Mac");
	    File f = null;
	    ip = getenv("R_HOME");
	    if (ip == null || ip.length()<1)
		ip = getenv("RHOME");
	    if (ip == null || ip.length()<1) {
		if (isMac) {
		    f=new File("/Library/Frameworks/R.framework/Resources/bin/R");
		    if (!f.exists())
			f=new File(getenv("HOME")+"/Library/Frameworks/R.framework/Resources/bin/R");
		    if (!f.exists())
			f=null;
		}
		if (f==null) {
		    String fn = findInPath(getenv("PATH"), "R", true);
		    if (fn == null)
			fn = findInPath("/usr/bin:/usr/local/bin:/sw/bin:/opt/bin:/usr/lib/R/bin:/usr/local/lib/R/bin", "R", true);
		    if (fn != null) f = new File(fn);
		}
		if (!findAllSettings) {
		    String s = f.getAbsolutePath();
		    if (s.length()>6) ip = s.substring(0, s.length()-6);
		}
	    }
	    if (findAllSettings) {
		if (f==null && ip!=null) f=new File(u2w(ip+"/bin/R"));
		if (f!=null) ip = getRSettings(f.getAbsolutePath());
	    }

	} catch (Exception e) {
	}
	return ip;
    }

    public static String rs_home = "";
    public static String rs_arch = "";
    public static String rs_docdir = "";
    public static String rs_incdir = "";
    public static String rs_sharedir = "";
    public static String rs_ldp = "";
    public static String rs_dyldp = "";
    public static String rs_unzip = "";
    public static String rs_latex = "";
    public static String rs_paper = "";
    public static String rs_print = "";
    public static String rs_libs = "";

    public static void setREnv() {
	if (rs_home!=null && rs_home.length()>0) setenv("R_HOME", rs_home);
	if (rs_arch!=null && rs_arch.length()>0) setenv("R_ARCH", rs_arch);
	if (rs_docdir!=null && rs_docdir.length()>0) setenv("R_DOC_DIR", rs_docdir);
	if (rs_incdir!=null && rs_incdir.length()>0) setenv("R_INCLUDE_DIR", rs_incdir);
	if (rs_sharedir!=null && rs_sharedir.length()>0) setenv("R_SHARE_DIR", rs_sharedir);
	if (rs_ldp!=null && rs_ldp.length()>0) setenv("LD_LIBRARY_PATH", rs_ldp);
	if (rs_dyldp!=null && rs_dyldp.length()>0) setenv("DYLD_LIBRARY_PATH", rs_dyldp);
	if (rs_libs!=null && rs_libs.length()>0) setenv("R_LIBS", rs_libs);
    }

    public static int execR(String cmd) {
	try {
	    String binR = u2w(rs_home+"/bin/R");
	    if (isWin32) {
		binR+=".exe";
		File fin = File.createTempFile("rboot",".R");
		File fout = File.createTempFile("rboot",".tmp");
		PrintStream p = new PrintStream(new FileOutputStream(fin));
		p.println(cmd);
		p.close();
		Process rp = Runtime.getRuntime().exec(new String[] {
			binR,"CMD","BATCH","--no-restore","--no-save","--slave",fin.getAbsolutePath(),
			fout.getAbsolutePath()});
		int i = rp.waitFor();
		if (!fin.delete()) fin.deleteOnExit();
		if (!fout.delete()) fout.deleteOnExit();
		return i;
	    } else {
		Process rp = Runtime.getRuntime().exec(new String[] {
			"/bin/sh","-c","echo \""+cmd+"\" |"+binR+" --no-restore --no-save --slave >/dev/null 2>&1" });
		return rp.waitFor();
	    }
	} catch (Exception e) {
	    lastError = e.toString();
	    return -1;
	}
    }

    public static String getRSettings(String binR) {
	try {
	    File fin = File.createTempFile("rboot",".R");
	    File fout = File.createTempFile("rboot",".tmp");
	    PrintStream p = new PrintStream(new FileOutputStream(fin));
	    p.println("cat(unlist(lapply(c('R_HOME','R_ARCH','R_DOC_DIR','R_INCLUDE_DIR','R_SHARE_DIR','LD_LIBRARY_PATH','DYLD_LIBRARY_PATH','R_UNZIPCMD','R_LATEXCMD','R_PAPERSIZE','R_PRINTCMD'),Sys.getenv)),sep='\n')");
	    p.println("cat(paste(.libPaths(),collapse=.Platform$path.sep),'\n',sep='')");
	    p.close();
	    Process rp = Runtime.getRuntime().exec(new String[] {
		    "/bin/sh","-c",binR+" --no-restore --no-save --slave < \""+fin.getAbsolutePath()+"\" > \""+fout.getAbsolutePath()+"\"" });
	    int i = rp.waitFor();
	    System.out.println("getRSettings, i="+i);
	    BufferedReader r = new BufferedReader(new FileReader(fout));
	    rs_home = r.readLine();
	    rs_arch = r.readLine();
	    rs_docdir = r.readLine();
	    rs_incdir = r.readLine();
	    rs_sharedir = r.readLine();
	    rs_ldp = r.readLine();
	    rs_dyldp = r.readLine();
	    rs_unzip = r.readLine();
	    rs_latex = r.readLine();
	    rs_paper = r.readLine();
	    rs_print = r.readLine();
	    rs_libs = r.readLine();
	    r.close();
	    if (!fin.delete()) fin.deleteOnExit();
	    //if (!fout.delete()) fout.deleteOnExit();
	    System.out.println(" - retrieved R settings, home: "+rs_home+" (arch="+rs_arch+", libs="+rs_libs+")");
	} catch (Exception e) {
	    System.err.println("Failed to get R settings: "+e);
	}
	return rs_home;
    }

    public static String u2w(String fn) {
	return (java.io.File.separatorChar != '/')?fn.replace('/',java.io.File.separatorChar):fn;
    }

    public static Object bootRJavaLoader = null;

    public static Object getBootRJavaLoader() {
	System.out.println("JRIBootstrap.bootRJavaLoader="+bootRJavaLoader);
	return bootRJavaLoader;
    }

    static void addClassPath(String s) {
	if (bootRJavaLoader==null) return;
	try {
	    Method m = bootRJavaLoader.getClass().getMethod("addClassPath", new Class[] { String.class });
	    m.invoke(bootRJavaLoader, new Object[] { s });
	} catch (Exception e) {
	    System.err.println("FAILED: JRIBootstrap.addClassPath");
	}
    }

    static String lastError = "";

    static String findPackage(String name) {
	String pd = null;
	if (rs_libs!=null && rs_libs.length()>0)
	    pd = findInPath(rs_libs, name, false);
	if (pd == null) {
	    pd = u2w(rs_home+"/library/"+name);
	    if (!(new File(pd)).exists()) pd = null;
	}
	return pd;	
    }

    static Object createRJavaLoader(String rhome, String[] cp, boolean addJRI) {
	String rJavaRoot = null;
	if (rs_libs!=null && rs_libs.length()>0)
	    rJavaRoot = findInPath(rs_libs, "rJava", false);
	if (rJavaRoot == null)
	    rJavaRoot = u2w(rhome+"/library/rJava");

	if (!(new File(rJavaRoot)).exists()) {
	    lastError="Unable to find rJava";
	    return null;
	}

	File f = new File(u2w(rJavaRoot+"/java/boot"));
	if (!f.exists()) {
	    // try harder ...
	    lastError = "rJava too old";
	    return null;
	}
	String rJavaHome = u2w(rJavaRoot);
	File lf = null;
	if (rs_arch!=null && rs_arch.length()>0) lf = new File(u2w(rJavaRoot+"/libs"+rs_arch));
	if (lf == null || !lf.exists()) lf = new File(u2w(rJavaRoot+"/libs/"+arch()));
	if (!lf.exists()) lf = new File(u2w(rJavaRoot+"/libs"));
	String rJavaLibs = lf.toString();
	JRIClassLoader mcl = JRIClassLoader.getMainLoader();
	mcl.addClassPath(f.toString()); // add rJava boot to primary CP
	try {
	    // force the use of the MCL even if the system loader could find it
	    Class rjlclass = mcl.findAndLinkClass("RJavaClassLoader");
	    Constructor c = rjlclass.getConstructor(new Class[] { String.class, String.class });
	    Object rjcl = c.newInstance(new Object[] { rJavaHome, rJavaLibs });
	    System.out.println("RJavaClassLoader: "+rjcl);
	    if (addJRI) {
		if (cp==null || cp.length==0)
		    cp = new String[] { u2w(rJavaRoot+"/jri/JRI.jar") };
		else {
		    String[] ncp = new String[cp.length+1];
		    System.arraycopy(cp, 0, ncp, 1, cp.length);
		    ncp[0] = u2w(rJavaRoot+"/jri/JRI.jar");
		    cp = ncp;
		}
	    }
	    if (cp==null || cp.length==0)
		cp = new String[] { u2w(rJavaRoot+"/java/boot") };
	    else {
		String[] ncp = new String[cp.length+1];
		System.arraycopy(cp, 0, ncp, 1, cp.length);
		ncp[0] = u2w(rJavaRoot+"/java/boot");
		cp = ncp;
	    }
	    if (cp != null) {
		System.out.println(" - adding class paths");
		Method m = rjlclass.getMethod("addClassPath", new Class[] { String[].class });
		m.invoke(rjcl, new Object[] { cp });
	    }
	    return rjcl;
	} catch (Exception rtx) {
	    System.err.println("ERROR: Unable to create new RJavaClassLoader in JRIBootstrap! ("+rtx+")");
	    rtx.printStackTrace();
	    System.exit(2);
	}
	return null;
    }
	
	//--- main bootstrap method ---
	public static void bootstrap(String[] args) {
		System.out.println("JRIBootstrap("+args+")");
		try {
			System.loadLibrary("boot");
		} catch (Exception e) {
			fail("Unable to load boot library!");
		}
		
		// just testing from now on
		String rhome = findR(true);
		if (rhome == null) fail("Unable to find R!");
		if (isWin32) {
		    String path = getenv("PATH");
		    if (path==null || path.length()<1) path=rhome+"\\bin";
		    else path=rhome+"\\bin;"+path;
		    setenv("PATH",path);
		}
		setREnv();

		System.out.println("PATH="+getenv("PATH")+"\nR_LIBS="+getenv("R_LIBS"));

		if (!isMac && !isWin32) {
		    String stage = System.getProperty("stage");
		    if (stage==null || stage.length()<1) {
			File jl = new File(u2w(System.getProperty("java.home")+"/bin/java"));
			if (jl.exists()) {
			    try {
				System.out.println(jl.toString()+" -cp "+System.getProperty("java.class.path")+" -Xmx512m -Dstage=2 Boot");
				Process p = Runtime.getRuntime().exec(new String[] {
				    jl.toString(), "-cp", System.getProperty("java.class.path"),
				    "-Xmx512m", "-Dstage=2", "Boot" });
				System.out.println("Started stage 2 ("+p+"), waiting for it to finish...");
				System.exit(p.waitFor());
			    } catch (Exception re) {
			    }
			}
		    }
		}

		String needPkg = null;
		String rj = findPackage("rJava");
		if (rj == null) {
		    System.err.println("**ERROR: rJava is not installed");
		    if (needPkg==null) needPkg="'rJava'"; else needPkg+=",'rJava'";
		}
		String ipl = findPackage("iplots");
		if (ipl == null) {
		    System.err.println("**ERROR: iplots is not installed");
		    if (needPkg==null) needPkg="'iplots'"; else needPkg+=",'iplots'";
		}
		String jgr = findPackage("JGR");
		if (jgr == null) {
		    System.err.println("**ERROR: JGR is not installed");
		    if (needPkg==null) needPkg="'JGR'"; else needPkg+=",'JGR'";
		}
		if (needPkg != null) {
		    if (!isWin32 && !isMac) {
			System.err.println("*** Please run the following in R as root to install missing packages:\n install.packages(c("+needPkg+"),,'http://www.rforge.net/')");
			System.exit(4);
		    }
		    if (execR("install.packages(c("+needPkg+"),,c('http://www.rforge.net/','http://cran.r-project.org'))")!=0) {
			System.err.println("*** ERROR: failed to install necessary packages");
			System.exit(4);
		    }
		    rj = findPackage("rJava");
		    ipl = findPackage("iplots");
		    jgr = findPackage("JGR");
		    if (rj==null || ipl==null || jgr==null) {
			System.err.println("*** ERROR: failed to find installed packages");
			System.exit(5);
		    }
		}

		Object o = bootRJavaLoader = createRJavaLoader(rhome, new String[] { "main" }, true);

		addClassPath(u2w(jgr+"/cont/JGR.jar"));
		addClassPath(u2w(ipl+"/cont/iplots.jar"));
		String mainClass = "org.rosuda.JGR.JGR";

		try {
		    Method m = o.getClass().getMethod("bootClass", new Class[] { String.class, String.class, String[].class });
		    m.invoke(o, new Object[] { mainClass, "main", args });
		} catch(Exception ie) {		    
		    System.out.println("cannot boot the final class: "+ie);
		    ie.printStackTrace();
		}
	}

	public static void main(String[] args) {
		System.err.println("*** WARNING: JRIBootstrap.main should NOT be called directly, it is intended for debugging use ONLY. Use Boot wrapper instead.");
		// just for testing
		bootstrap(args);
	}
}
