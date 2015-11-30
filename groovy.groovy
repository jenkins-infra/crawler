#!./lib/runner.groovy
// Generates server-side metadata for Groovy auto-installation
import com.gargoylesoftware.htmlunit.html.*;

import net.sf.json.*
import com.gargoylesoftware.htmlunit.WebClient

def wc = new WebClient()
def baseUrl = 'https://dl.bintray.com/groovy/maven/'
HtmlPage p = wc.getPage(baseUrl);

def json = [];

p.selectNodes("//a[@href]").reverse().collect { HtmlAnchor e ->
    def url = baseUrl + e.getHrefAttribute()[1..-1]
    println url
    def m = (url =~ /groovy-binary-(\d.\d.\d).zip$/)
    if (m) {
        json << ["id":m[0][1], "name": "Groovy ${m[0][1]}".toString(), "url":url];
    }
}

lib.DataWriter.write("hudson.plugins.groovy.GroovyInstaller",JSONObject.fromObject([list:json.sort { it.id }.reverse()]));
