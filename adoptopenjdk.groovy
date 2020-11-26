#!./lib/runner.groovy
import com.gargoylesoftware.htmlunit.Page
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.WebResponse
import net.sf.json.JSONArray
import net.sf.json.JSONObject

class ListAdoptOpenJDK {
    private final WebClient wc;

    private final String API_URL = "https://api.adoptopenjdk.net/v3";

    ListAdoptOpenJDK() {
        wc = new WebClient();
        wc.addRequestHeader("accept", "application/json")
        wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
    }

    void main() throws Exception {
        lib.DataWriter.write("io.jenkins.plugins.adoptopenjdk.AdoptOpenJDKInstaller", build());
    }

    private JSONObject build() throws IOException {
        Page p = wc.getPage(API_URL + "/info/available_releases")
        WebResponse response = p.getWebResponse();

        if (response.statusCode != 200) {
            throw new Exception("Could not load available_releases")
        }

        JSONArray result = new JSONArray()
        JSONObject data = JSONObject.fromObject(response.getContentAsString())

        result.addAll(getReleases(data.available_releases as JSONArray, "HotSpot"))
        result.addAll(getReleases(data.available_releases as JSONArray, "OpenJ9"))

        return new JSONObject()
                .element("version", 2)
                .element("data", result);
    }

    private JSONArray getReleases(JSONArray available_releases, String jvm_impl) {
        JSONArray result = new JSONArray()
        String openjdk_impl = jvm_impl.toLowerCase()
        available_releases.each { feature_version ->
            JSONObject r = new JSONObject()
            r.put("name", "OpenJDK " + feature_version + " - " + jvm_impl)
            JSONArray releases = new JSONArray()
            int page = 0
            boolean keepGoing = true
            while (keepGoing) {
                Page a = wc.getPage(API_URL + "/assets/feature_releases/" + feature_version + "/ga?vendor=adoptopenjdk&project=jdk&image_type=jdk&sort_method=DEFAULT&sort_order=DESC&page_size=20&page=" + (page++) + "&jvm_impl=" + openjdk_impl)
                WebResponse ar = a.getWebResponse()
                if (ar.getStatusCode() == 200) {
                    JSONArray assets = JSONArray.fromObject(ar.getContentAsString())
                    releases.addAll(assets.collect {
                        [
                                release_name: it.release_name,
                                openjdk_impl: openjdk_impl,
                                binaries    : it.binaries.collect {
                                    [
                                            architecture: it.architecture,
                                            os          : it.os,
                                            openjdk_impl: it.jvm_impl,
                                            binary_link : it.package.link
                                    ]
                                }
                        ]
                    })
                } else {
                    keepGoing = false
                }
            }
            r.put("releases", releases)
            result.add(r)
        }
        return result
    }
}

new ListAdoptOpenJDK().main();