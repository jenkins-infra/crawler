#!./lib/runner.groovy
// Generates server-side metadata for Oracle JDK
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.BrowserVersion
import net.sf.json.JSONObject
import org.kohsuke.args4j.CmdLineException
import java.security.GeneralSecurityException
import net.sf.json.JSONArray
import java.util.regex.Pattern
import java.util.regex.Matcher
import com.gargoylesoftware.htmlunit.html.HtmlPage
import net.sourceforge.htmlunit.corejs.javascript.IdScriptableObject

public class ListJDK {
    private final WebClient wc;

    public ListJDK() {
        wc = new WebClient(BrowserVersion.FIREFOX_3);
        wc.setCssEnabled(false);
        wc.setThrowExceptionOnScriptError(false);
        wc.setThrowExceptionOnFailingAjax(false);
    }

    public void main() throws Exception {
        lib.DataWriter.write("hudson.tools.JDKInstaller",build());
    }

    private JSONObject build() throws IOException, CmdLineException, GeneralSecurityException {
        return new JSONObject()
                .element("version", 2)
                .element("data", new JSONArray()
                        .element(family("JDK 12", combine(
                            parse("https://www.oracle.com/technetwork/java/javase/downloads/java-archive-javase12-5440181.html"),
                            parse("https://www.oracle.com/technetwork/java/javase/downloads/jdk12-downloads-5295953.html"))))
                        .element(family("JDK 11", combine(
                            parse("https://www.oracle.com/technetwork/java/javase/downloads/java-archive-javase11-5116896.html"),
                            parse("https://www.oracle.com/technetwork/java/javase/downloads/jdk11-downloads-5066655.html"))))
                        .element(family("JDK 10", combine(
                            parse("https://www.oracle.com/technetwork/java/javase/downloads/java-archive-javase10-4425482.html"))))
                        .element(family("JDK 9", combine(
                            parse("http://www.oracle.com/technetwork/java/javase/downloads/java-archive-javase9-3934878.html"))))
                        .element(family("JDK 8", combine(
                            parse("http://www.oracle.com/technetwork/java/javase/downloads/java-archive-javase8-2177648.html"),
                            parse("http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html"))))
                        .element(family("JDK 7", combine(
                            parse("http://www.oracle.com/technetwork/java/javase/downloads/java-archive-downloads-javase7-521261.html"))))
                        .element(family("JDK 6", combine(
                            parse("http://www.oracle.com/technetwork/java/javasebusiness/downloads/java-archive-downloads-javase6-419409.html"))))
                        .element(family("JDK 5", combine(
                            parse("http://www.oracle.com/technetwork/java/javasebusiness/downloads/java-archive-downloads-javase5-419410.html"))))
                        .element(family("JDK 1.4", combine(
                            parse("http://www.oracle.com/technetwork/java/javasebusiness/downloads/java-archive-downloads-javase14-419411.html")))));
    }

    private static final Pattern NUMBER = Pattern.compile("\\d+");

    /**
     * Builds a data structure for a major release.
     */
    private JSONObject family(String name, JSONObject data) {
        JSONObject o = new JSONObject();
        o.put("name",name);
        JSONArray releases = new JSONArray();
        List<String> releaseNames = new ArrayList<String>((Set<String>) data.keySet());
        Collections.sort(releaseNames,new Comparator<String>() {
            public int compare(String o1, String o2) {
                return -versionNormalize(o1).compareTo(versionNormalize(o2)); // descending order
            }

            /**
             * We want to compare numbers among string tokens as numbers. To do it simply, we force the width of any string token to be 4 and then compare.
             */
            private String versionNormalize(String s) {
                Matcher m = NUMBER.matcher(s);
                StringBuffer sb = new StringBuffer();
                while (m.find()) {
                    m.appendReplacement(sb, String.format("%4s",m.group()));
                }
                m.appendTail(sb);
                return sb.toString();
            }
        });
        for (String n : releaseNames) {
            if (n.contains("demo")) continue;   // we don't care about demo & sample bundles
            if (n.contains("jdk") || n.contains("j2sdk"))
                releases.add(release(n,data.getJSONObject(n)));
        }
        o.put("releases",releases);
        return o;
    }

    private JSONObject release(String name, JSONObject src) {
        JSONObject input = src.getJSONObject("files");
        JSONArray files = new JSONArray();
        for (String n : (Set<String>)input.keySet()) {
            if (n.contains("rpm"))   continue;   // we don't use RPM bundle
            if (n.contains("tar.Z"))   continue;   // we don't use RPM bundle
            if (n.contains("-iftw.exe"))   continue;   // online installers
            files.add(file(n, input.getJSONObject(n)));
        }
        return src.element("name",massageName(name)).element("files", files);
    }

    /**
     * JDK release names are inconsistent, so make it look uniform
     */
    private String massageName(String name) {
        name = name.trim();
        name = name.replace("(TM)", "");
        name = name.replace(" Update ","u");
        return name;
    }

    private JSONObject file(String name, JSONObject src) {
        return src.element("name",name).element("size",(Object)null);
    }

    private JSONObject parse(String url) throws IOException {
        HtmlPage p = getPage(url);

        return (JSONObject)toJSON(p.executeJavaScript("downloads").getJavaScriptResult());
    }

    private JSONObject combine(JSONObject... args) {
        JSONObject o = new JSONObject();
        args.each { a -> o.putAll(a); }
        return o;
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

new ListJDK().main();
