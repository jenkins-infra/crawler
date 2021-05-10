#!./lib/runner.groovy
// Generates server-side metadata for dependencycheck-launch auto-installation

import hudson.util.VersionNumber
import net.sf.json.*

def listFromBintray() {
    def url = "https://bintray.com/api/v1/packages/jeremy-long/owasp/dependency-check".toURL()
    def bintray = JSONObject.fromObject(url.text)

    bintray["versions"].collect {
        version -> ["id": version,
                    "name": "dependency-check ${version}".toString(),
                    "url": "https://dl.bintray.com/jeremy-long/owasp/dependency-check-${version}-release.zip".toString()];
    }
}

def listFromGithub() {
    def url = "https://api.github.com/repos/jeremylong/dependencycheck/releases".toURL()
    def releases = JSONArray.fromObject(url.text)

    releases.collect {
        release ->
            def version = release["tag_name"].substring(1)
            ["id": version,
             "name": "dependency-check ${version}".toString(),
             "url": "https://github.com/jeremylong/DependencyCheck/releases/download/v${version}/dependency-check-${version}-release.zip".toString()]
    }
}


def listAll() {
    Map versions = new HashMap()
    listFromBintray().each {
        version -> versions.put(version["id"], version) }
    listFromGithub().each {version -> versions.put(version["id"], version)}

    return versions.values()
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

lib.DataWriter.write("org.jenkinsci.plugins.DependencyCheck.tools.DependencyCheckInstaller",JSONObject.fromObject([list:listAll()]));
