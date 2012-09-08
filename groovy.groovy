#!/usr/bin/env groovy

// Generates server-side metadata for Groovy auto-installation
@GrabResolver(name="repo.jenkins-ci.org",root='http://repo.jenkins-ci.org/public/')
@Grab(group="org.jvnet.hudson",module="htmlunit",version="2.2-hudson-9")
@Grab(group="org.jenkins-ci",module="update-center2",version="1.20")
import com.gargoylesoftware.htmlunit.html.*;

import net.sf.json.*
import com.gargoylesoftware.htmlunit.WebClient
import org.jvnet.hudson.update_center.Signer

def wc = new WebClient()
def baseUrl = 'http://dist.groovy.codehaus.org/distributions/'
HtmlPage p = wc.getPage(baseUrl);

def json = [];

p.selectNodes("//a[@href]").reverse().collect { HtmlAnchor e ->
    def url = baseUrl + e.getHrefAttribute()
    println url
    def m = (url =~ /groovy-binary-(\d.\d.\d).zip$/)
    if (m) {
        json << ["id":m[0][1], "name": "Groovy ${m[0][1]}".toString(), "url":url];
    }
}

JSONObject envelope = JSONObject.fromObject([list:json]);
new Signer().configureFromEnvironment().sign(envelope);
println envelope.toString(2)

key = "hudson.plugins.groovy.GroovyInstaller";
File d = new File("target")
d.mkdirs()
new File(d,"${key}.json").write("downloadService.post('${key}',${envelope.toString(2)})");
