#!/usr/bin/env groovy
// Generates server-side metadata for Gradle auto-installation
@GrabResolver(name="repo.jenkins-ci.org",root='http://repo.jenkins-ci.org/public/')
@Grab(group="org.jvnet.hudson",module="htmlunit",version="2.2-hudson-9")
@Grab(group="org.jenkins-ci",module="update-center2",version="1.20")
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.WebClient

import net.sf.json.*
import org.jvnet.hudson.update_center.Signer

def wc = new WebClient()
def baseUrl = 'http://services.gradle.org'
HtmlPage p = wc.getPage(baseUrl + '/distributions');

def json = [];

p.selectNodes("//a[@href]").reverse().collect { HtmlAnchor e ->
    def url = baseUrl + e.getHrefAttribute()
    println url
    def m = (url =~ /gradle-(.*)-bin.zip$/)
    if (m) {
        json << ["id":m[0][1], "name": "Gradle ${m[0][1]}".toString(), "url":url];
    }
}

JSONObject envelope = JSONObject.fromObject([list:json]);
new Signer().configureFromEnvironment().sign(envelope);
println envelope.toString(2)

key = "hudson.plugins.gradle.GradleInstaller";
File d = new File("target")
d.mkdirs()
new File(d,"${key}.json").write("downloadService.post('${key}',${envelope.toString(2)})");
