#!./lib/runner.groovy
import org.htmlunit.Page
import org.htmlunit.WebClient
import org.htmlunit.WebResponse
import net.sf.json.JSONArray
import net.sf.json.JSONObject

class ListAdoptOpenJDK {
    private final WebClient wc;

    private final String API_URL = "https://api.adoptium.net/v3";

    ListAdoptOpenJDK() {
        wc = new WebClient();
        wc.addRequestHeader("accept", "application/json")
        wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
        wc.getOptions().setPrintContentOnFailingStatusCode(false);
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

        return new JSONObject()
                .element("version", 2)
                .element("data", result);
    }

    private JSONArray getReleases(JSONArray available_releases, String jvm_impl) {
        JSONArray result = new JSONArray()
        available_releases.each { feature_version ->
            JSONObject r = getRelease(feature_version as String, jvm_impl)
            result.add(r)
        }
        return result
    }

    private JSONObject getRelease(String feature_version, String jvm_impl){
        String openjdk_impl = jvm_impl.toLowerCase()
        JSONObject r = new JSONObject()
        r.put("name", "OpenJDK " + feature_version + " - " + jvm_impl)
        Map<String, JSONObject> releasesMap = new LinkedHashMap<>();
        int page = 0
        boolean keepGoing = true
        while (keepGoing) {
            Page a = wc.getPage(API_URL + "/assets/feature_releases/" + feature_version + "/ga?vendor=eclipse&project=jdk&image_type=jdk&sort_method=DEFAULT&sort_order=DESC&page_size=20&page=" + (page++) + "&jvm_impl=" + openjdk_impl)
            WebResponse ar = a.getWebResponse()
            if (ar.getStatusCode() == 200) {
                JSONArray assets = JSONArray.fromObject(ar.getContentAsString())
                assets.forEach { JSONObject asset -> releasesMap.merge(asset.release_name as String, asset, { v1, v2 ->
                    if (v1.version_data?.adopt_build_number > v2.version_data?.adopt_build_number){
                        v1.binaries.addAll(v2.binaries);
                        return v1;
                    } else {
                        v2.binaries.addAll(v1.binaries);
                        return v2;
                    }
                })}
            } else {
                keepGoing = false
            }
        }

        def releases = releasesMap.values().collect {
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
        }
        r.put("releases", releases)
        return r
    }
}

new ListAdoptOpenJDK().main();
