#!./lib/runner.groovy
// Generates server-side metadata for dependencycheck-launch auto-installation

import hudson.util.VersionNumber
import net.sf.json.*

def listFromGithub(int page) {
    def url = ("https://api.github.com/repos/dependency-check/dependencycheck/releases?per_page=50&page=" + page).toURL()
    def releases = JSONArray.fromObject(url.text)

    releases.collect {
        release ->
            def version = release["tag_name"].substring(1)
            ["id": version,
             "name": "dependency-check ${version}".toString(),
             "url": "https://github.com/dependency-check/DependencyCheck/releases/download/v${version}/dependency-check-${version}-release.zip".toString()]
    }
}


def listAll() {
    Map versions = new HashMap()
    for (int page = 0;; page++) {
        def releases = listFromGithub(page)
        releases.each {version -> versions.put(version["id"], version)}
        if (releases.isEmpty()) {
            break; // do {} until {}
        }
    }

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
