#!./lib/runner.groovy
// Generates server-side metadata for sbt-launch auto-installation
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.WebClient
import hudson.util.VersionNumber
import net.sf.json.*

def listFromOldURL() {
    def wc = new WebClient()
    def baseUrl = 'http://scalasbt.artifactoryonline.com/scalasbt/sbt-native-packages/org/scala-sbt/sbt/'
    HtmlPage p = wc.getPage(baseUrl);

    return p.selectNodes("//a[@href]").reverse().collect { HtmlAnchor e ->
        def url = baseUrl + e.getHrefAttribute()
        println url
        def m = (url =~ /([0-9]{1,}\.[0-9]{2}\.[0-9]{1}(-.*)?)\/$/)
        if (m) {
            println m[0][1]
            return ["id": m[0][1], "name": "sbt ${m[0][1]}".toString(), "url": url + "sbt.zip"];
        }
    }
}

def listFromNewUrl() {
    def url = "https://api.bintray.com/v1/packages/sbt/native-packages/sbt".toURL()
	def bintray = JSONObject.fromObject(url.text)

	bintray["versions"].collect {
        version -> ["id": version,
                    "name": "sbt ${version}".toString(),
                    "url": "https://dl.bintray.com/sbt/native-packages/sbt/${version}/sbt-${version}.zip".toString()];
    }
}

def listAll() {
    def listFromNewUrl = listFromNewUrl()
    def idsFromNewUrl = listFromNewUrl.collect { it -> it.id }

    def listFromOldURL = listFromOldURL()
            .findAll { it != null }
            .collect { it -> !idsFromNewUrl.contains(it.id) ? it : null }

    return (listFromNewUrl + listFromOldURL)
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
