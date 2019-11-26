#!./lib/runner.groovy
// Generates server-side metadata for Gradle auto-installation
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.WebClient

import net.sf.json.*

def wc = new WebClient()
def baseUrl = 'https://nodejs.org/dist'
HtmlPage p = wc.getPage(baseUrl);

def json = [];

p.getByXPath("//a[@href]").reverse().collect { HtmlAnchor e ->
    def url = baseUrl + "/" + e.getHrefAttribute()
    println url
    String versionRegex = "v(\\d+(?:\\.\\d+)*)/";
    def m = (url =~ versionRegex)
    if (m) {
        json << ["id": m[0][1], "name": "NodeJS ${m[0][1]}".toString(), "url": url];
    }
}

lib.DataWriter.write("hudson.plugins.nodejs.tools.NodeJSInstaller",JSONObject.fromObject([list:json]));
