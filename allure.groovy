#!./lib/runner.groovy
// Generates server-side metadata for Allure
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.DomElement
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.xml.XmlPage
import net.sf.json.JSONObject

def getList() {
    List versions = new ArrayList()
    versions.addAll(getCentralVersions())
    versions.addAll(getBintrayVersions())
    versions.addAll(getSonatypeVersions())
    return versions
}
def getCentralVersions() {
    String baseUrl = 'https://repo.maven.apache.org/maven2/io/qameta/allure/allure-commandline'
    URL metaUrl = new URL("$baseUrl/maven-metadata.xml")

    WebClient wc = new WebClient()
    XmlPage meta = wc.getPage(metaUrl)

    List<String> versions = meta.getByXPath("//metadata/versioning/versions/version")
            .collect() { DomElement e -> e.getTextContent() }
            .findAll() { e -> !e.contains('BETA') }
            .reverse()

    return versions.collect() { version ->
        return ["id"  : version,
                "name": version,
                "url" : String.format('%s/%s/allure-commandline-%s.zip', baseUrl, version, version)
        ]
    }
}

def getBintrayVersions() {
    String baseUrl = 'https://dl.bintray.com/qameta/generic/io/qameta/allure/allure'
    WebClient wc = new WebClient()
    HtmlPage meta = wc.getPage(baseUrl)

    List<String> versions = meta.getByXPath("//body/pre")
            .collect() { DomElement e -> e.getTextContent().replace("/", "") }
            .findAll() { e -> !e.contains('BETA') }
            .reverse()

    return versions.collect() { version ->
        return ["id"  : version,
                "name": version,
                "url" : String.format('%s/%s/allure-%s.zip', baseUrl, version, version)
        ]
    }
}

def getSonatypeVersions() {
    String baseUrl = 'https://oss.sonatype.org/content/repositories/releases/ru/yandex/qatools/allure/allure-commandline'
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
                "url" : getSonatypeArtifactUrl(baseUrl, version)
        ]
    }
}

def getSonatypeArtifactUrl(String baseUrl, String version) {
    def artifactName = getSonatypeArtifactName(version);
    return String.format('%s/%s/%s', baseUrl, version, artifactName);
}

def getSonatypeArtifactName(String version) {
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