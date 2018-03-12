public class Types {
    public static int sri() { return si; }
    public int ri() { return i; }
    public static byte srb() { return sb; }
    public byte rb() { return b; }
    public static boolean srz() { return sz; }
    public boolean rz() { return z; }
    public static char src() { return sc; }
    public char rc() { return c; }
    public static short srs() { return ss; }
    public short rs() { return s; }
    public static long srj() { return sj; }
    public long rj() { return j; }
    public static float srf() { return sf; }
    public float rf() { return f; }
    public static double srd() { return sd; }
    public double rd() { return d; }
    public static String srS() { return sS; }
    public String rS() { return S; }
    
    public void zbcsijfdS(boolean _z, byte _b, char _c,
			 short _s, int _i, long _j,
			 float _f, double _d, String _S) {
	z = _z; b = _b; c = _c;
	s = _s; i = _i; j = _j;
	f = _f; d = _d; S = _S;
    }
    public static void szbcsijfdS(boolean z, byte b, char c,
				 short s, int i, long j,
				 float f, double d, String S) {
	sz = z; sb = b; sc = c;
	ss = s; si = i; sj = j;
	sf = f; sd = d; sS = S;
    }

    public boolean z = true;
    public byte b = 123;
    public char c = 'c';
    public short s = 1234;
    public int i = 1234;
    public long j = 1234567890;
    public double d = 1234.567;
    public float f = 1234.567f;
    public String S = "ok";
    public static boolean sz = true;
    public static byte sb = 123;
    public static char sc = 'c';
    public static short ss = 1234;
    public static int si = 1234;
    public static long sj = 1234567890;
    public static double sd = 1234.567;
    public static float sf = 1234.567f;
    public static String sS = "ok";
}

