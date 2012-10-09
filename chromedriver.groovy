#!./lib/runner.groovy
// Generates server-side metadata for chromedriver auto-installation
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.WebClient

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

lib.DataWriter.write("org.jenkins-ci.plugins.chromedriver.ChromeDriver",JSONObject.fromObject([list:json]));
