#!/usr/bin/env groovy
// Generates server-side metadata for Gradle auto-installation
@GrabResolver(name="m.g.o-public",root='http://maven.glassfish.org/content/group/public/')
@Grab(group="org.jvnet.hudson",module="htmlunit",version="2.2-hudson-9")
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.WebClient

@Grab(group="org.kohsuke.stapler",module="json-lib",version="2.1",classifier="jdk15")
import net.sf.json.*

def wc = new WebClient()
wc.javaScriptEnabled = wc.cssEnabled = false;
def baseUrl = 'http://code.google.com/p/chromedriver/downloads/list'
HtmlPage p = wc.getPage(baseUrl);

def json = [];

p.selectNodes("//a[@href]").each { HtmlAnchor e ->
    def href = e.getHrefAttribute()
    def textContent = e.getTextContent().trim()
    if (!href.startsWith("detail?") || !textContent.endsWith(".zip"))   return;

    def os = textContent.split("_")[1];

    println textContent;
    if (os!=null) {
        json << ["id":os, "url":"http://chromedriver.googlecode.com/files/"+textContent];
    }
}

JSONObject envelope = JSONObject.fromObject([list:json]);
println envelope.toString(2)

key = "org.jenkins-ci.plugins.chromedriver.ChromeDriver";
File d = new File("target")
d.mkdirs()
new File(d,"${key}.json").write("downloadService.post('${key}',${envelope.toString(2)})");
