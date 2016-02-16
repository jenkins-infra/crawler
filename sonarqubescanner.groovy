#!./lib/runner.groovy
// Generates server-side metadata for Sonar Runner
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlAnchor
import com.gargoylesoftware.htmlunit.html.HtmlPage
import java.util.regex.Pattern
import net.sf.json.JSONObject
import hudson.util.VersionNumber

def listFromOldCodehausURL() {
    def url = "http://repo1.maven.org/maven2/org/codehaus/sonar-plugins/sonar-runner/";
    def wc = new WebClient()
    wc.javaScriptEnabled = false;
    wc.cssEnabled = false;
    HtmlPage p = wc.getPage(url);
    def pattern=Pattern.compile("^([0-9][0-9\\.]+)/\$");

    return p.getAnchors().collect { HtmlAnchor a ->
        m = pattern.matcher(a.hrefAttribute)
        println(a.hrefAttribute)
        if(m.find()) {
            ver=m.group(1)
            url = p.getFullyQualifiedUrl(a.hrefAttribute + "sonar-runner-" + ver + ".zip");
            return ["id":ver, "name": "SonarQube Scanner " + ver, "url":url.toExternalForm()]
        }
        return null;
    }
}

def listFromNewCodehausUrl() {
    def url = "http://repo1.maven.org/maven2/org/codehaus/sonar/runner/sonar-runner-dist/";
    def wc = new WebClient()
    wc.javaScriptEnabled = false;
    wc.cssEnabled = false;
    HtmlPage p = wc.getPage(url);
    def pattern = Pattern.compile("^([0-9][0-9\\.]+)/");

    return p.getAnchors().collect { HtmlAnchor a ->
        m = pattern.matcher(a.hrefAttribute)
        println(a.hrefAttribute)
        if(m.find()) {
            ver=m.group(1)
            url = p.getFullyQualifiedUrl(a.hrefAttribute + "sonar-runner-dist-" + ver + ".zip");
            return ["id":ver, "name": "SonarQube Scanner " + ver, "url":url.toExternalForm()]
        }
        return null;
    }
}

def listFromNewSonarSourceUrl() {
    def url = "http://repo1.maven.org/maven2/org/sonarsource/scanner/cli/sonar-scanner-cli/";
    def wc = new WebClient()
    wc.javaScriptEnabled = false;
    wc.cssEnabled = false;
    HtmlPage p = wc.getPage(url);
    def pattern = Pattern.compile("^([0-9][0-9\\.]+)/");

    return p.getAnchors().collect { HtmlAnchor a ->
        m = pattern.matcher(a.hrefAttribute)
        println(a.hrefAttribute)
        if(m.find()) {
            ver=m.group(1)
            url = p.getFullyQualifiedUrl(a.hrefAttribute + "sonar-scanner-cli-" + ver + ".zip");
            return ["id":ver, "name": "SonarQube Scanner " + ver, "url":url.toExternalForm()]
        }
        return null;
    }
}

def listAll() {
  return (listFromOldCodehausURL() + listFromNewCodehausUrl() + listFromNewSonarSourceUrl())
    .findAll { it!=null }.sort { o1,o2 ->
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
}

def store(key,o) {
    JSONObject envelope = JSONObject.fromObject(["list": o]);
    lib.DataWriter.write(key,envelope);
}

store("hudson.plugins.sonar.SonarRunnerInstaller", listAll())

