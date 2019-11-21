#!./lib/runner.groovy
// Generates server-side metadata for Scala auto-installation
import java.net.URI
import net.sf.json.*
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.*

def wc = new WebClient()

//Unfortunately we cannot get an index page from http://www.scala-lang.org/downloads/distrib/files/ so instead
//we need to extract content from the HTML downloads page
def baseUrl = 'http://www.scala-lang.org/files/archive/'
def pathAndVersionRegex = /^(scala-\d+\.\d+(?:\.\d+)?(?:(?:-RC\d)|(?:\.final))?.tgz)$/
// def pathAndVersionRegex = /^\/downloads\/distrib\/files\/(scala-\d+\.\d+(?:\.\d+)?(?:(?:-RC\d)|(?:\.final))?.tgz)$/

HtmlPage p = wc.getPage(baseUrl)

def json = [];

p
  .getByXPath("//a[@href]")
  .grep({
    it.getHrefAttribute() ==~ pathAndVersionRegex 
  })
  .collect { HtmlAnchor e ->
    def file = e.getHrefAttribute()
    def url = new URI(baseUrl).resolve(new URI(file)).toString()
    //println url
	json << ["id": file, "name": file.replaceFirst("scala-", "Scala ").replaceFirst(".tgz", ""), "url": url]
  }

lib.DataWriter.write("hudson.plugins.scala.ScalaInstaller", JSONObject.fromObject([list: json]))
