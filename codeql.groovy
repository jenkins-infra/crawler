#!./lib/runner.groovy
// Generates server-side metadata for Gradle auto-installation
import com.gargoylesoftware.htmlunit.Page
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.WebResponse

import net.sf.json.*

def wc = new WebClient();
wc.addRequestHeader("accept", "application/json")
wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
wc.getOptions().setPrintContentOnFailingStatusCode(false);

def API_URL = "https://api.github.com";
Page p = wc.getPage(API_URL + "/repos/github/codeql-action/releases")

WebResponse response = p.getWebResponse();

if (response.statusCode != 200) {
    throw new Exception("Could not load available_releases")
}

JSONArray result = new JSONArray()
JSONArray data = JSONArray.fromObject(response.getContentAsString())


data.each { release ->
    JSONObject r = new JSONObject()
    def tagname = release.tag_name
    if(!(tagname =~ /codeql-bundle-(.*)/)) {
        return
    }
    def bundleDate = (tagname =~ /codeql-bundle-(.*)/)[0][1]

    def bundleversion = ( release.body =~ /.*v([0-9]+\.[0-9]+\..*)/)[0][1]

    r.put("id", bundleversion )
    r.put("name", bundleversion)
    r.put("url", "https://github.com/github/codeql-action/releases/download/" + tagname + '/')
    result.add(r)
}

JSONObject resultObj = new JSONObject()
                .element("list", result);

lib.DataWriter.write("io.jenkins.plugins.codeql.CodeQLInstaller", resultObj);
