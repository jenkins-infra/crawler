#!./lib/runner.groovy
// Generates server-side metadata for Packer auto-installation
import org.htmlunit.html.*;
import net.sf.json.*
import org.htmlunit.WebClient

def baseUrl = 'https://releases.hashicorp.com'
def json = []
def wc = new WebClient()
HtmlPage p = wc.getPage("${baseUrl}/packer/")
p.getByXPath("//a[@href]").grep { it.hrefAttribute =~ /\/packer\/.*/ }.each {
    wc.getPage("${baseUrl}${it.hrefAttribute}").getByXPath("//a[@href]").each {
        def m = (it.textContent =~ /packer_.*(\d+.\d+.\d+)_(.*)_(.*).zip/)
        if (m) {
            def verId = "${m[0][1]}-${m[0][2]}-${m[0][3]}".toString()
            json << ["id": verId, "name": "Packer ${m[0][1]} ${m[0][2]} (${m[0][3]})".toString(), "url": "${it.hrefAttribute}".toString()];
        }
    }
}

lib.DataWriter.write("biz.neustar.jenkins.plugins.packer.PackerInstaller",JSONObject.fromObject([list:json]));
