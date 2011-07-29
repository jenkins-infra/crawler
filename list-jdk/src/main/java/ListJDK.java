import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sourceforge.htmlunit.corejs.javascript.IdScriptableObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Set;

/**
 * Couldn't get the Groovy version working because of org/w3c/dom/UserDataHandler.class in Jaxen
 * that makes Groovy classloader unhappy.
 *
 * @author Kohsuke Kawaguchi
 */
public class ListJDK {
    private final WebClient wc;

    public ListJDK() {
        wc = new WebClient();
        wc.setCssEnabled(false);
    }

    public static void main(String[] args) throws Exception {
        OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream("target/hudson.tools.JDKInstaller.json"), "UTF-8");
        w.write("downloadService.post('hudson.tools.JDKInstaller',");
        w.write(new ListJDK().build().toString());
        w.write(")");
        w.close();
    }

    private JSONObject build() throws IOException {
        return new JSONObject()
            .element("version", 2)
            .element("data",new JSONArray()
                .element(family("JDK 7", parse("http://www.oracle.com/technetwork/java/javase/downloads/java-se-jdk-7-download-432154.html")))
                .element(family("JDK 6", parse("http://www.oracle.com/technetwork/java/javasebusiness/downloads/java-archive-downloads-javase6-419409.html")))
                .element(family("JDK 5", parse("http://www.oracle.com/technetwork/java/javasebusiness/downloads/java-archive-downloads-javase5-419410.html")))
                .element(family("JDK 1.4", parse("http://www.oracle.com/technetwork/java/javasebusiness/downloads/java-archive-downloads-javase14-419411.html"))));
    }

    /**
     * Builds a data structure for a major release.
     */
    private JSONObject family(String name, JSONObject data) {
        JSONObject o = new JSONObject();
        o.put("name",name);
        JSONArray releases = new JSONArray();
        for (String n : (Set<String>)data.keySet()) {
            if (n.contains("jdk"))
                releases.add(release(n,data.getJSONObject(n)));
        }
        o.put("releases",releases);
        return o;
    }

    private JSONObject release(String name, JSONObject src) {
        JSONObject in = src.getJSONObject("files");
        JSONArray files = new JSONArray();
        for (String n : (Set<String>)in.keySet()) {
            if (n.contains("rpm"))   continue;   // we don't use RPM bundle
            if (n.contains("tar.Z"))   continue;   // we don't use RPM bundle
            if (n.contains("-iftw.exe"))   continue;   // online installers
            files.add(file(n, in.getJSONObject(n)));
        }
        return src.element("name",name).element("files",files);
    }

    private JSONObject file(String name, JSONObject src) {
        return src.element("name",name).element("size",(Object)null);
    }

    private JSONObject parse(String url) throws IOException {
        HtmlPage p = getPage(url);

        return (JSONObject)toJSON(p.executeJavaScript("downloads").getJavaScriptResult());
    }

    private Object toJSON(Object o) {
        // the code in the page instantiates array but uses it as an object,
        // so we convert them all into JSONObject
        if (o instanceof IdScriptableObject) {
            IdScriptableObject na = (IdScriptableObject) o;
            JSONObject ja = new JSONObject();
            for (Object key : na.getIds()) {
                ja.put(key.toString(), toJSON(na.get(key)));
            }
            return ja;
        }
        // primitives
        return o;
    }

    private HtmlPage getPage(String url) throws IOException {
//        System.out.println("Fetching url} ...")
        long start = System.currentTimeMillis();
        HtmlPage p = wc.getPage(url);
        long end = System.currentTimeMillis();
//        println("done (took ${end - start} msec)")
        return p;
    }
}
