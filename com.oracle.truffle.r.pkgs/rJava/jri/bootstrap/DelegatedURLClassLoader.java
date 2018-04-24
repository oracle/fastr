/* An extension of URLClassLoader that implements DelegatedClassLoader */

import java.net.URL;
import java.net.URLClassLoader;

public class DelegatedURLClassLoader extends URLClassLoader implements DelegatedClassLoader {
    public DelegatedURLClassLoader() {
	super(new URL[]{});
    }
    public DelegatedURLClassLoader(URL[] urls) {
	super(urls);
    }
    public String delegatedFindLibrary(String name) {
	return super.findLibrary(name);
    }
    public Class delegatedFindClass(String name) throws ClassNotFoundException {
	return super.findClass(name);
    }
    public URL delegatedFindResource(String name) {
	return super.findResource(name);
    }
}
