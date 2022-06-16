#!./lib/runner.groovy
// Generates server-side metadata for Groovy auto-installation
import com.gargoylesoftware.htmlunit.html.*;

import net.sf.json.*
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.BrowserVersion

final String applicationName = "APPNAME";
final String applicationVersion = "APPVERSION";
final String userAgent = "USERAGENT";

final BrowserVersion browser =
        new BrowserVersion.BrowserVersionBuilder(BrowserVersion.BEST_SUPPORTED)
            .setApplicationName(applicationName)
            .setApplicationVersion(applicationVersion)
            .setUserAgent(userAgent)
            .build();

def wc = new WebClient(browser);
def baseUrl = 'https://groovy.jfrog.io/artifactory/dist-release-local/groovy-zips/'
HtmlPage p = wc.getPage(baseUrl);

def json = [];

p.getByXPath("//a[@href]").reverse().collect { HtmlAnchor e ->
    def url = baseUrl + e.getHrefAttribute()
    println url
    def m = (url =~ /groovy-binary-(\d.\d.\d).zip$/)
    if (m) {
        json << ["id":m[0][1], "name": "Groovy ${m[0][1]}".toString(), "url":url];
    }
}

lib.DataWriter.write("hudson.plugins.groovy.GroovyInstaller",JSONObject.fromObject([list:json.sort { it.id }.reverse()]));
