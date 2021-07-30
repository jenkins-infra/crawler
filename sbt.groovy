#!./lib/runner.groovy
// Generates server-side metadata for sbt-launch auto-installation
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.xml.XmlPage
import hudson.util.VersionNumber
import net.sf.json.*

def listFromMaven() {
    String baseUrl = 'https://repo1.maven.org/maven2/org/scala-sbt/sbt'
    URL metaUrl = new URL("$baseUrl/maven-metadata.xml")

    WebClient wc = new WebClient()
    XmlPage meta = wc.getPage(metaUrl)

    List<String> versions = meta.getByXPath("//metadata/versioning/versions/version")
            .collect() { DomElement e -> e.getTextContent() }
            .findAll() { e -> !e.contains('RC') }
            .reverse()

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

def listAll() {
    List versions = new ArrayList()
    versions.addAll(listFromMaven())

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
