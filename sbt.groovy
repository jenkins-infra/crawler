#!./lib/runner.groovy
// Generates server-side metadata for sbt-launch auto-installation
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.WebClient

import net.sf.json.*

def wc = new WebClient()
def baseUrl = 'http://repo.typesafe.com/typesafe/ivy-releases/org.scala-sbt/sbt-launch/'
HtmlPage p = wc.getPage(baseUrl);

def json = [];

p.selectNodes("//a[@href]").reverse().collect { HtmlAnchor e ->
    def url = baseUrl + e.getHrefAttribute()
    println url
    def m = (url =~ /([0-9]{1,}\.[0-9]{2}\.[0-9]{1}(-.*)?)\/$/)
    if (m) {
        println m[0][1]
        json << ["id":m[0][1], "name": "sbt-launch.jar ${m[0][1]}".toString(), "url":url + "sbt-launch.jar"];
    }
}

lib.DataWriter.write("org.jvnet.hudson.plugins.SbtPluginBuilder.SbtInstaller",JSONObject.fromObject([list:json]));
