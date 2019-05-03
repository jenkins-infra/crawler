#!./lib/runner.groovy
import com.gargoylesoftware.htmlunit.UnexpectedPage
import com.gargoylesoftware.htmlunit.WebClient
import net.sf.json.JSONArray
import net.sf.json.JSONObject

public class ListAdoptOpenJDK {
    private final WebClient wc;

    public ListAdoptOpenJDK() {
        wc = new WebClient();
    }

    public void main() throws Exception {
        lib.DataWriter.write("io.jenkins.plugins.adoptopenjdk.AdoptOpenJDKInstaller", build());
    }

    private JSONObject build() throws IOException {
        return new JSONObject()
                .element("version", 2)
                .element("data", new JSONArray()
                    .element(family("OpenJDK 8 - HotSpot", parse("hotspot", "https://api.adoptopenjdk.net/v2/info/releases/openjdk8?openjdk_impl=hotspot")))
                    .element(family("OpenJDK 8 - OpenJ9", parse("openj9", "https://api.adoptopenjdk.net/v2/info/releases/openjdk8?openjdk_impl=openj9")))

                    .element(family("OpenJDK 9 - HotSpot", parse("hotspot", "https://api.adoptopenjdk.net/v2/info/releases/openjdk9?openjdk_impl=hotspot")))
                    .element(family("OpenJDK 9 - OpenJ9", parse("openj9", "https://api.adoptopenjdk.net/v2/info/releases/openjdk9?openjdk_impl=openj9")))

                    .element(family("OpenJDK 10 - HotSpot", parse("hotspot", "https://api.adoptopenjdk.net/v2/info/releases/openjdk10?openjdk_impl=hotspot")))
                    .element(family("OpenJDK 10 - OpenJ9", parse("openj9", "https://api.adoptopenjdk.net/v2/info/releases/openjdk10?openjdk_impl=openj9")))

                    .element(family("OpenJDK 11 - HotSpot", parse("hotspot", "https://api.adoptopenjdk.net/v2/info/releases/openjdk11?openjdk_impl=hotspot")))
                    .element(family("OpenJDK 11 - OpenJ9", parse("openj9", "https://api.adoptopenjdk.net/v2/info/releases/openjdk11?openjdk_impl=openj9")))

                    .element(family("OpenJDK 12 - HotSpot", parse("hotspot", "https://api.adoptopenjdk.net/v2/info/releases/openjdk12?openjdk_impl=hotspot")))
                    .element(family("OpenJDK 12 - OpenJ9", parse("openj9", "https://api.adoptopenjdk.net/v2/info/releases/openjdk12?openjdk_impl=openj9")))
        );
    }

    private JSONObject family(String name, JSONArray data) {
        JSONObject o = new JSONObject();
        o.put("name", name);
        o.put("releases", data);
        return o;
    }

    private JSONArray parse(String openjdk_impl, String url) throws IOException {
        UnexpectedPage p = wc.getPage(url);
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