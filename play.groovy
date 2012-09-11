#!./lib/runner.groovy
// Generates server-side metadata for Play auto-installation
import com.gargoylesoftware.htmlunit.html.*;

import net.sf.json.*
import com.gargoylesoftware.htmlunit.WebClient

def wc = new WebClient()
def baseUrl = 'http://download.playframework.org/releases/'
HtmlPage p = wc.getPage(baseUrl);

def json = [];

p.selectNodes("//a[@href]").reverse().collect { HtmlAnchor e ->
    def url = baseUrl + e.getHrefAttribute()
    println url
    def m = (url =~ /play-(.*).zip$/)
    if (m) {
        json << ["id":m[0][1], "name": "Play ${m[0][1]}".toString(), "url":url];
    }
}

lib.DataWriter.write("hudson.plugins.play.PlayInstaller",JSONObject.fromObject([list:json]));
