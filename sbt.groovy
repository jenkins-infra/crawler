#!./lib/runner.groovy
// Generates server-side metadata for sbt-launch auto-installation
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.WebClient
import hudson.util.VersionNumber
import net.sf.json.*

def listFromBintray() {
    def url = "https://bintray.com/api/v1/packages/sbt/native-packages/sbt".toURL()
	def bintray = JSONObject.fromObject(url.text)

	bintray["versions"].collect {
        version -> ["id": version,
                    "name": "sbt ${version}".toString(),
                    "url": "https://dl.bintray.com/sbt/native-packages/sbt/${version}/sbt-${version}.zip".toString()];
    }
}

def listAll() {
    return listFromBintray()
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
