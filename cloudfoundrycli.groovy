import groovy.json.JsonSlurper
import net.sf.json.JSONObject

/*
 * There may be an API on https://cli.run.pivotal.io/ to discover the available versions,
 * for the moment, discover it on https://github.com/cloudfoundry/cli/releases
 */
URL releasesUrl = new URL("https://api.github.com/repos/cloudfoundry/cli/releases")
def releases = new JsonSlurper().parseText(releasesUrl.text)
/*
 * There may be an API on https://cli.run.pivotal.io/ to discover the available platforms,
 * for the moment we hard code them
 */
def platforms = new JsonSlurper().parseText('[ \
   {"os": "linux",      "arch" : "386",     "id": "linux32-binary"   }, \
   {"os": "linux",      "arch" : "x86-64",  "id": "linux64-binary"   }, \
   {"os": "darwin",     "arch" : "x86-64",  "id": "macosx64-binary"  }, \
   {"os": "windows",    "arch" : "386",     "id": "windows32-exe"    }, \
   {"os": "windows",    "arch" : "x86-64",  "id": "windows64-exe"    } \
]')

def json = []

releases.each { release ->
    String version = release.tag_name.substring(1) // remove "v" prefix e.g. "v6.6.0"

    def variants = []
    String[] splittedVersion = version.split("\\.")
    try {
        int majorVersion = Integer.parseInt(splittedVersion[0])
        int minorVersion = Integer.parseInt(splittedVersion[1])
        if (majorVersion >= 6) { // don't list versions older than 6.0.0

            platforms.each { platform ->
                String url = "https://cli.run.pivotal.io/stable?release=$platform.id&version=$version&source=jenkins"
                // call toString() to workaround "net.sf.json.JSONException: There is a cycle in the hierarchy!"
                variants << [
                        "os": "$platform.os".toString(),
                        "arch": "$platform.arch".toString(),
                        "url": url.toString()];
            }

            json << [
                    "name": "CloudFoundry CLI $version".toString(),
                    "id": "$version".toString(),
                    "variants": variants
            ]
        } else {
            // print "skip old version $version"
        }


    } catch (Exception e) {
        println "skip version $version " + e
        e.printStackTrace()
    }
}

lib.DataWriter.write("com.cloudbees.plugins.cloudfoundry.cli.CloudFoundryCliInstaller", JSONObject.fromObject([releases: json]));
