#!./lib/runner.groovy
import com.gargoylesoftware.htmlunit.UnexpectedPage
import com.gargoylesoftware.htmlunit.WebClient
import net.sf.json.JSONArray
import net.sf.json.JSONObject

public class ListAdoptOpenJDK {
    private final WebClient wc;

    private final String API_CONFIG = "https://adoptopenjdk.net/dist/json/config.json";
    private final String API_URL = "https://api.adoptopenjdk.net/v2";

    public ListAdoptOpenJDK() {
        wc = new WebClient();
    }

    public void main() throws Exception {
        lib.DataWriter.write("io.jenkins.plugins.adoptopenjdk.AdoptOpenJDKInstaller", build());
    }

    private JSONObject build() throws IOException {
        UnexpectedPage p = wc.getPage(API_CONFIG);
        JSONObject data = JSONObject.fromObject(p.getWebResponse().getContentAsString());
        JSONArray variants = data.getJSONArray("variants");
        JSONArray result = new JSONArray();
        for (int i=0; i<variants.size(); i++) {
            JSONObject v = variants.getJSONObject(i);
            String familyName = v.getString("label") + " - " + v.getString("jvm");
            def searchableName = v.getString("searchableName").split("-");
            String jvm = searchableName[0];
            String openjdk_impl = searchableName[1];
            result.add(family(familyName, parse(jvm, openjdk_impl)));
        }

        return new JSONObject()
                .element("version", 2)
                .element("data", result);
    }

    private static JSONObject family(String name, JSONArray data) {
        JSONObject o = new JSONObject();
        o.put("name", name);
        o.put("releases", data);
        return o;
    }

    private JSONArray parse(String jvm, String openjdk_impl) throws IOException {
        UnexpectedPage p = wc.getPage(API_URL + "/info/releases/" + jvm + "?openjdk_impl=" + openjdk_impl);
        JSONArray data = JSONArray.fromObject(p.getWebResponse().getContentAsString());
        JSONArray result = new JSONArray();
        for (int i=0; i<data.size(); i++) {
            JSONObject r = new JSONObject();

            JSONObject release = data.getJSONObject(i);
            r.put("release_name", release.getString("release_name"));
            r.put("openjdk_impl", openjdk_impl);

            JSONArray binaries = release.getJSONArray("binaries");
            JSONArray b = new JSONArray();
            for (int j=0; j<binaries.size(); j++) {
                JSONObject binary = binaries.getJSONObject(j);
                if ("jdk".equals(binary.getString("binary_type"))) {
                    b.add(binary);
                }
            }
            r.put("binaries", b);

            result.add(r);
        }
        return result;
    }
}

new ListAdoptOpenJDK().main();