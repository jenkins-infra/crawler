#!/usr/bin/env groovy

// Generates server-side metadata for MongoDB auto-installation
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.5.1' )
import groovyx.net.http.*
@GrabResolver(name="m.g.o-public",root='http://maven.glassfish.org/content/group/public/')
@Grab(group="org.jvnet.hudson",module="htmlunit",version="2.2-hudson-9")
import com.gargoylesoftware.htmlunit.html.*;
@Grab(group="org.kohsuke.stapler",module="json-lib",version="2.1",classifier="jdk15")
import net.sf.json.*
import com.gargoylesoftware.htmlunit.WebClient
import java.util.logging.Level;

def wc = new WebClient()
def json = [];

// disable logging
java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF); 

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

JSONObject envelope = JSONObject.fromObject([list:json])
println envelope.toString(2)
key = "com.g2one.hudson.grails.GrailsInstaller"
File d = new File("target")
d.mkdirs()
new File(d,"${key}.json").write("downloadService.post('${key}',${envelope.toString(2)})")
