#!./lib/runner.groovy
import com.gargoylesoftware.htmlunit.html.*;
import net.sf.json.*
import com.gargoylesoftware.htmlunit.WebClient

def baseUrl = 'https://releases.hashicorp.com'
def json = []
def wc = new WebClient()
HtmlPage p = wc.getPage("${baseUrl}/terraform/")
p.getByXPath("//a[@href]").grep { it.hrefAttribute =~ /\/terraform\/.*/ }.each {
    wc.getPage("${baseUrl}${it.hrefAttribute}").getByXPath("//a[@href]").each {
        def m = (it.textContent =~ /terraform.*(\d+.\d+.\d+)_(.*)_(.*).zip/)
        if (m) {
            def verId = "${m[0][1]}-${m[0][2]}-${m[0][3]}".toString()
            json << ["id": verId, "name": "Terraform ${m[0][1]} ${m[0][2]} (${m[0][3]})".toString(), "url": "${baseUrl}${it.hrefAttribute}".toString()];
        }
    }
}

lib.DataWriter.write("org.jenkinsci.plugins.terraform.TerraformInstaller", JSONObject.fromObject([list:json]));
