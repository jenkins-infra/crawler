#!./lib/runner.groovy
// Generates server-side metadata for Allure
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.DomElement
import com.gargoylesoftware.htmlunit.xml.XmlPage
import net.sf.json.JSONObject


def getList() {
    def baseUrl = 'https://oss.sonatype.org/content/repositories/releases/ru/yandex/qatools/allure/allure-commandline';
    List versions = getVersions(baseUrl)

    return versions.collect() { version ->
        return ["id"  : version,
                "name": version,
                "url" : getUrl(baseUrl, version)
        ]
    }
}

def getVersions(String baseUrl) {
    URL metaUrl = new URL("$baseUrl/maven-metadata.xml");

    WebClient wc = new WebClient()
    XmlPage meta = wc.getPage(metaUrl)

    List versions = meta.getByXPath("//metadata/versioning/versions/version")
    return versions
            .collect() { DomElement e -> e.getTextContent() }
            .findAll() { e -> !e.contains('RC') }
            .reverse()
}

def getUrl(String baseUrl, String version) {
    def artifactName = getArtifactName(version);
    return String.format('%s/%s/%s', baseUrl, version, artifactName);
}

def getArtifactName(String version) {
    switch (version) {
        case '1.4.17': return String.format('allure-commandline-%s.zip', version)
        default: return String.format('allure-commandline-%s-standalone.zip', version)
    }
}

def store(key, o) {
    JSONObject envelope = JSONObject.fromObject(["list": o])
    lib.DataWriter.write(key, envelope)
}

store("ru.yandex.qatools.allure.jenkins.tools.AllureCommandlineInstaller", getList())