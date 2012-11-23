#!./lib/runner.groovy
// Generates server-side metadata for MongoDB auto-installation
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.5.1' )
import groovyx.net.http.*
import com.gargoylesoftware.htmlunit.html.*;
import net.sf.json.*
import com.gargoylesoftware.htmlunit.WebClient

def wc = new WebClient()
def json = [];

def http = new HttpURLClient(url:'http://www.grails.org', followRedirects: false)
HtmlPage p = wc.getPage('http://www.grails.org/download')
def xpath = "//section[@id='downloadArchives']//table[@class='table']//tr/td[4]/a"
p.selectNodes(xpath).collect { HtmlAnchor a ->
    def url = a.getHrefAttribute()
    println url
    def m = url =~/\/grails(?:-bin)?-(.*)\.zip/
    if (m) {
        json << [id:m[0][1], name:"Grails ${m[0][1]}".toString(), url:url]
    }
}

lib.DataWriter.write("com.g2one.hudson.grails.GrailsInstaller",JSONObject.fromObject([list:json]));
