#!./lib/runner.groovy
// Generates server-side metadata for MongoDB auto-installation
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.5.1' )
import groovyx.net.http.*
import com.gargoylesoftware.htmlunit.html.*;
import net.sf.json.*
import com.gargoylesoftware.htmlunit.WebClient

def wc = new WebClient()
def json = [];

def http = new HttpURLClient(url:'http://grails.org', followRedirects: false)
HtmlPage p = wc.getPage('http://grails.org/download/archive/Grails')
def xpath = "//tr/td[1]/strong[contains(text(), 'Binary Z')]/../../td[2]/select[@name='mirror']/option[1]"
p.selectNodes(xpath).collect { HtmlOption opt ->
    def res = http.request(path:'/download/file', query:[ mirror:opt.getValueAttribute() ])
    if (res.status == 302) {
        def url = res.headers.Location
        def m = url =~/\/grails(?:-bin)?-(.*)\.zip/
        if (m) {
            json << [id:m[0][1], name:"Grails ${m[0][1]}".toString(), url:url]
        }
    }
    sleep 500
}

lib.DataWriter.write("com.g2one.hudson.grails.GrailsInstaller",JSONObject.fromObject([list:json]));
