#!./lib/runner.groovy
// Generates server-side metadata for Leiningen auto-installation
import com.gargoylesoftware.htmlunit.html.*;

import net.sf.json.*
import com.gargoylesoftware.htmlunit.WebClient

def wc = new WebClient()
def baseUrl = 'https://github.com/technomancy/leiningen/releases'
HtmlPage p = wc.getPage(baseUrl);

def json = [];

p.selectNodes("//a[@href]").reverse().collect { HtmlAnchor e ->
    def url = baseUrl + e.getHrefAttribute()
    println url
    def m = (url =~ /leiningen-(.*)-standalone.jar$/)
    if (m) {
        json << ["id":m[0][1], "name": "Leiningen ${m[0][1]}".toString(), "url":url];
    }
}

lib.DataWriter.write("org.jenkins-ci.plugins.leiningen.LeinInstaller",JSONObject.fromObject([list:json]));
