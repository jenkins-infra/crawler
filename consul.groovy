#!./lib/runner.groovy
// Generates server-side metadata for Consul auto-installation
import com.gargoylesoftware.htmlunit.html.*;
import net.sf.json.*
import com.gargoylesoftware.htmlunit.WebClient

def baseUrl = 'https://releases.hashicorp.com'
def json = []
def wc = new WebClient()
HtmlPage p = wc.getPage("${baseUrl}/consul/")
p.getByXPath("//a[@href]").grep { it.hrefAttribute =~ /\/consul\/.*/ }.each {
    wc.getPage("${baseUrl}${it.hrefAttribute}").getByXPath("//a[@href]").each {
        def m = (it.textContent =~ /consul_.*(\d+.\d+.\d+)_(.*)_(.*).zip/)
        if (m) {
            def verId = "${m[0][1]}-${m[0][2]}-${m[0][3]}".toString()
            json << ["id": verId, "name": "Consul ${m[0][1]} ${m[0][2]} (${m[0][3]})".toString(), "url": "${it.hrefAttribute}".toString()];
        }
    }
}

lib.DataWriter.write("com.inneractive.jenkins.plugins.consul.ConsulInstaller",JSONObject.fromObject([list:json]));
