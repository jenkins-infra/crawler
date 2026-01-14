#!./lib/runner.groovy
// Generates server-side metadata for sbt-launch auto-installation
import org.htmlunit.html.*;
import org.htmlunit.WebClient
import org.htmlunit.html.HtmlAnchor
import org.htmlunit.html.HtmlPage
import org.htmlunit.xml.XmlPage
import hudson.util.VersionNumber
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern
import net.sf.json.*

def listFromMavenRepo() {
    def versions = []
    def url = "https://repo1.maven.org/maven2/org/scala-sbt/sbt/";

    def wc = new WebClient();
    wc.setCssErrorHandler(new org.htmlunit.SilentCssErrorHandler());
    wc.getOptions().setThrowExceptionOnScriptError(false);
    wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
    HtmlPage p = wc.getPage(url);
    def pattern = Pattern.compile("^([0-9]+.*)/\$");

    p.getAnchors().collect {
        HtmlAnchor a ->
        m = pattern.matcher(a.hrefAttribute)
        if (m.find()) {
            ver = m.group(1)
            if (!ver.contains("RC") && urlReturns200(getGithubArtifactUrl(ver))) {
                versions.addAll(ver)
            }
        }
    }
    return versions.collect() { version ->
        return ["id"  : version,
                "name": version,
                "url" : getGithubArtifactUrl(version)
        ]
    }
}

def getGithubArtifactUrl(String version) {
    return String.format("https://github.com/sbt/sbt/releases/download/v%s/sbt-%s.zip", version, version)
}

boolean urlReturns200(String urlString, int timeoutMillis = 10000) {
    HttpURLConnection connection = null
    try {
        URL url = new URL(urlString)
        connection = (HttpURLConnection) url.openConnection()
        connection.setRequestMethod("GET")
        connection.setConnectTimeout(timeoutMillis)
        connection.setReadTimeout(timeoutMillis)
        connection.setInstanceFollowRedirects(true)

        int responseCode = connection.getResponseCode()
        return responseCode == 200
    } catch (Exception e) {
        return false
    } finally {
        if (connection != null) {
            connection.disconnect()
        }
    }
}

def listAll() {
    List versions = new ArrayList()
    versions.addAll(listFromMavenRepo())

    return versions
            .findAll { it != null }
            .sort { o1,o2 ->
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

lib.DataWriter.write("org.jvnet.hudson.plugins.SbtPluginBuilder.SbtInstaller",JSONObject.fromObject([list:listAll()]));
