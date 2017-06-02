#!./lib/runner.groovy
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlAnchor
import com.gargoylesoftware.htmlunit.html.HtmlPage
import hudson.util.VersionNumber
import net.sf.json.JSONObject

import java.util.regex.Pattern
// Generates server-side metadata for Flyway command-line
def webclient (){
  def wc = new WebClient()
  wc.javaScriptEnabled = false;
  wc.cssEnabled = false;
  return wc
}

def listFromMavenRepo() {
  def json = []
  def url = "https://repo1.maven.org/maven2/org/flywaydb/flyway-commandline/";

  def wc=webclient();
  HtmlPage p = wc.getPage(url);
  def pattern = Pattern.compile("^([0-9]+.*)/\$");

  p.getAnchors().collect { HtmlAnchor a ->
    m = pattern.matcher(a.hrefAttribute)
    if (m.find()) {
      ver = m.group(1)
      json.addAll( os_versions(url + ver, ver, wc))
    }
  }
  return json
}

def os_versions(url, version, wc) {
  json=[]
  HtmlPage page= wc.getPage(url)
  def pattern = Pattern.compile("flyway-commandline-" + ver + "(\\.tar\\.gz|-([a-z]+)-x64(\\.zip|\\.tar.gz))\$")

  page.getAnchors().collect { HtmlAnchor a ->
    m = pattern.matcher(a.hrefAttribute)
    if (m.find()) {
      installer_url=page.getFullyQualifiedUrl(m.group(0))

      def parsed_platform = m.group(2)
      platform = parsed_platform ? " (" + parsed_platform + ")": " (without JRE)"
      id = version + ( parsed_platform ? "_" + parsed_platform : "nojre")

      json << ["id": id, "name": ver  + platform, "url": installer_url.toExternalForm()]
    }
  }
  return json
}

def flyway_distributions = listFromMavenRepo()

flyway_distributions.sort { o1, o2 ->
  try {
    def v1 = new VersionNumber(o1.id)
    try {
      new VersionNumber(o2.id).compareTo(v1)
    } catch (IllegalArgumentException _2) {
      -1
    }
  } catch (IllegalArgumentException _1) {
    try {
      new VersionNumber(o2.id)
      1
    } catch (IllegalArgumentException _2) {
      o2.id.compareTo(o1.id)
    }
  }
}

lib.DataWriter.write("sp.sd.flywayrunner.installation.FlywayInstaller", JSONObject.fromObject([list: flyway_distributions]));


