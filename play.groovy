#!./lib/runner.groovy
// Generates server-side metadata for Play auto-installation
import org.htmlunit.html.*;

import net.sf.json.*
import org.htmlunit.WebClient

def wc = new WebClient()
wc.setCssErrorHandler(new org.htmlunit.SilentCssErrorHandler());
wc.getOptions().setJavaScriptEnabled(false);
wc.getOptions().setThrowExceptionOnScriptError(false);
wc.getOptions().setThrowExceptionOnFailingStatusCode(false);

def baseUrl = 'https://www.playframework.com/releases'
HtmlPage p = wc.getPage(baseUrl);

def json = [];

p.getByXPath("//a[@href]").reverse().collect { HtmlAnchor e ->
    def url = e.getHrefAttribute()
    def m = (url =~ /play-(.*).zip$/)
    if (m) {
        json << ["id":m[0][1], "name": "Play ${m[0][1]}".toString(), "url":url];
    }
}

lib.DataWriter.write("hudson.plugins.play.PlayInstaller",JSONObject.fromObject([list:json]));
