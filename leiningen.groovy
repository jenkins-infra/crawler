#!./lib/runner.groovy
// Generates server-side metadata for Leiningen auto-installation
import com.gargoylesoftware.htmlunit.html.*;

import net.sf.json.*
import com.gargoylesoftware.htmlunit.WebClient

def wc = new WebClient()
wc.setCssErrorHandler(new com.gargoylesoftware.htmlunit.SilentCssErrorHandler());
wc.getOptions().setJavaScriptEnabled(false);
wc.getOptions().setThrowExceptionOnScriptError(false);
wc.getOptions().setThrowExceptionOnFailingStatusCode(false);

def baseUrl = 'https://github.com'
HtmlPage p = wc.getPage(baseUrl + '/technomancy/leiningen/releases');
//  curl 'https://api.github.com/repos/technomancy/leiningen/releases' | jq '.[].assets[].name,.[].assets[].browser_download_url'


def json = [];

p.getByXPath("//a[@href]").reverse().collect { HtmlAnchor e ->
    def url = baseUrl + e.getHrefAttribute()
    def m = (url =~ /leiningen-(.*)-standalone.jar$/)
    if (m) {
        json << ["id":m[0][1], "name": "Leiningen ${m[0][1]}".toString(), "url":url];
    }
}

lib.DataWriter.write("org.jenkins-ci.plugins.leiningen.LeinInstaller",JSONObject.fromObject([list:json]));
