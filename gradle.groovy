#!./lib/runner.groovy
// Generates server-side metadata for Gradle auto-installation
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.WebClient

import net.sf.json.*

def wc = new WebClient()
wc.setThrowExceptionOnScriptError(false)

def baseUrl = 'https://services.gradle.org'
HtmlPage p = wc.getPage(baseUrl + '/distributions');

def json = [];

p.selectNodes("//a[@href]").collect { HtmlAnchor e ->
    def url = baseUrl + e.getHrefAttribute()
    println url
    def m = (url =~ /gradle-(.*)-bin.zip$/)
    if (m) {
        json << ["id":m[0][1], "name": "Gradle ${m[0][1]}".toString(), "url":url];
    }
}

lib.DataWriter.write("hudson.plugins.gradle.GradleInstaller",JSONObject.fromObject([list:json]));
