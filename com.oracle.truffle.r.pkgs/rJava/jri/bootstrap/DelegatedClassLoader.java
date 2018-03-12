import java.net.URL;

public interface DelegatedClassLoader {
    public String delegatedFindLibrary(String name);
    public Class delegatedFindClass(String name) throws ClassNotFoundException;
    public URL delegatedFindResource(String name);
}
