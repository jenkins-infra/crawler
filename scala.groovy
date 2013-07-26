#!./lib/runner.groovy
// Generates server-side metadata for Scala auto-installation
import java.net.URI
import net.sf.json.*
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.*

def wc = new WebClient()

//Unfortunately we cannot get an index page from http://www.scala-lang.org/downloads/distrib/files/ so instead
//we need to extract content from the HTML downloads page
def baseUrl = 'http://www.scala-lang.org/downloads/'
def pathAndVersionRegex = /^\/downloads\/distrib\/files\/(scala-\d+\.\d+(?:\.\d+)?(?:(?:-RC\d)|(?:\.final))?.tgz)$/

HtmlPage p = wc.getPage(baseUrl)

def json = [];

p
  .selectNodes("//a[@href]")
  .grep({
    it.getHrefAttribute() ==~ pathAndVersionRegex 
  })
  .reverse()
  .collect { HtmlAnchor e ->
    def downloadUri = e.getHrefAttribute()
    def m = (downloadUri =~ pathAndVersionRegex)
    println(m)
    if (m) {
      def url = new URI(baseUrl).resolve(new URI(downloadUri)).toString()
      //println url
	  json << ["id": m[0][1], "name": "${m[0][1]}".replaceFirst("scala-", "Scala ").replaceFirst(".tgz", ""), "url": url]
	}
  }

lib.DataWriter.write("hudson.plugins.scala.ScalaInstaller", JSONObject.fromObject([list: json]))
