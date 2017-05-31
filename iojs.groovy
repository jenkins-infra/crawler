#!./lib/runner.groovy

import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.WebClient

import net.sf.json.*

def wc = new WebClient()
def baseUrl = 'http://iojs.org/dist'
HtmlPage p = wc.getPage(baseUrl);

def json = [];

p.selectNodes("//a[@href]").reverse().collect { HtmlAnchor e ->
    def url = baseUrl + "/" + e.getHrefAttribute()
    String versionRegex = "v(\\d+(?:\\.\\d+)*)/";
    def m = (url =~ versionRegex)
    if (m) {
        json << ["id": m[0][1], "name": "io.js ${m[0][1]}".toString(), "url": url];
    }
}

lib.DataWriter.write("hudson.plugins.iojs.tools.IojsInstaller",JSONObject.fromObject([releases:json]));
