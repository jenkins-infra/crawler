#!./lib/runner.groovy
// Generates server-side metadata for Packer auto-installation
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.WebClient

import com.gargoylesoftware.htmlunit.BrowserVersion

import net.sf.json.*

BrowserVersion browserVer = BrowserVersion.getDefault()
def wc = new WebClient(browserVer)
def baseUrl = 'http://dl.bintray.com/mitchellh/packer'
HtmlPage p = wc.getPage(baseUrl);

def json = [];

p.selectNodes("//a[@href]").reverse().collect { HtmlAnchor e ->
    def href = e.getHrefAttribute().replaceFirst(/^:/) {''}
    def url = baseUrl + "/" + href
    //println url
    def m = (url =~ /.*(\d+.\d+.\d+)_(.*)_(.*).zip/)
    if (m) {
        def verId = "${m[0][1]}-${m[0][2]}-${m[0][3]}".toString()
        json << ["id": verId, "name": "Packer ${m[0][1]} ${m[0][2]} (${m[0][3]})".toString(), "url": url];
    }
}

lib.DataWriter.write("biz.neustar.jenkins.plugins.packer.PackerInstaller",JSONObject.fromObject([list:json]));
