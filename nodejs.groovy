#!./lib/runner.groovy
// Generates server-side metadata for Gradle auto-installation
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.WebClient

import net.sf.json.*

def wc = new WebClient()
def baseUrl = 'http://nodejs.org/dist'
HtmlPage p = wc.getPage(baseUrl);

class Version implements Comparable<Version> {
  private String url
  private Integer major;
  private Integer minor;
  private Integer patch;
  Version(String url, Integer major, Integer minor, Integer patch){
    this.url = url; this.major = major; this.minor = minor; this.patch = patch;
  }
  public int compareTo(Version v){
    int cmp = major.compareTo(v.major);
    if(cmp == 0){
      cmp = minor.compareTo(v.minor);
      if(cmp == 0){
        return patch.compareTo(v.patch);
      }
    }
    return cmp;
  }
  public String toString(){
    return this.major+"."+this.minor+"."+this.patch;
  }
}

def json = [];
def versions = new TreeSet();

p.selectNodes("//a[@href]").reverse().collect { HtmlAnchor e ->
    def url = baseUrl + "/" + e.getHrefAttribute()
    println url
    String versionRegex = "v(\\d+(?:\\.\\d+)*)/";
    def m = (url =~ versionRegex)
    if (m) {
        def chunkedVersions = m[0][1].split("\\.");
        versions << new Version(url, chunkedVersions[0].toInteger(), chunkedVersions[1].toInteger(), chunkedVersions[2].toInteger());
    }
}

(versions as List).reverse().collect { Version v ->
  json << ["id": v.toString(), "name": "NodeJS ${v}".toString(), "url": v.url];
}

lib.DataWriter.write("hudson.plugins.nodejs.tools.NodeJSInstaller",JSONObject.fromObject([list:json]));
