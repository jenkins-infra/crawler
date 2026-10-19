#!./lib/runner.groovy
// Generates server-side metadata for SonarScanner CLI
import net.sf.json.JSONObject
import hudson.util.VersionNumber

def listFromSonarSourceBinaries() {
    def baseUrl = "https://binaries.sonarsource.com/"
    def prefix = "Distribution/sonar-scanner-cli/"
    // Only keep the platform-independent distributions (no JRE embedded)
    def pattern = ~/^${prefix}sonar-scanner(?:-cli)?-([0-9][0-9\.]*)\.zip$/
    def result = []
    def marker = ""
    def truncated = true
    while (truncated) {
        def listUrl = baseUrl + "s3api?prefix=" + URLEncoder.encode(prefix, "UTF-8") + "&delimiter=%2F&marker=" + URLEncoder.encode(marker, "UTF-8")
        def xml = new XmlSlurper().parseText(new URL(listUrl).text)
        xml.Contents.each {
            def key = it.Key.text()
            def m = pattern.matcher(key)
            if (m.matches()) {
                def ver = m.group(1)
                result << ["id": ver, "name": "SonarQube Scanner " + ver, "url": baseUrl + key]
            }
            marker = key
        }
        truncated = xml.IsTruncated.text() == "true"
        if (xml.NextMarker.text()) {
            marker = xml.NextMarker.text()
        }
    }
    return result
}

def listAll() {
    return listFromSonarSourceBinaries().sort { o1, o2 -> new VersionNumber(o2.id).compareTo(new VersionNumber(o1.id)) }
}

def store(key,o) {
    JSONObject envelope = JSONObject.fromObject(["list": o]);
    lib.DataWriter.write(key,envelope);
}

store("hudson.plugins.sonar.SonarRunnerInstaller", listAll())
