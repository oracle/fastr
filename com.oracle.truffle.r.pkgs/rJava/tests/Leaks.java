public class Leaks {
    public int[] i;
    public String[] s;

    public Leaks(int[] i, String[] s) {
	this.i = i;
	this.s = s;
    }

    public String[] getS() { return s; }
    public int[] getI() { return i; }
    
    public String[] replaceS(String[] s) {
	String[] a = this.s;
	this.s = s;
	return a;
    }

    public int[] replaceI(int[] i) {
	int[] a = this.i;
	this.i = i;
	return a;
    }

    public static String[] passS(String[] s) { return s; }
    public static Object[] passSO(String[] s) { return (Object[])s; }
    public static int[] passI(int[] i) { return i; }


    public static void runGC() {
	Runtime r = Runtime.getRuntime();
	r.runFinalization();
	r.gc();
    }

    public static String reportMem() {
	Runtime r = Runtime.getRuntime();
	r.gc();
	return "used: "+((r.totalMemory()-r.freeMemory())/1024L)+"kB (free: "+r.freeMemory()+" of "+r.totalMemory()+")";
    }
}
