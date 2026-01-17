#!./lib/runner.groovy
// Generates server-side metadata for Allure
import org.htmlunit.html.HtmlAnchor;
import org.htmlunit.WebClient
import hudson.util.VersionNumber
import org.htmlunit.html.HtmlPage
import java.util.regex.Pattern
import net.sf.json.JSONObject

def getList() {
    List versions = new ArrayList()
    versions.addAll(getCentralVersions())
    versions.addAll(getSonatypeVersions())
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

def getCentralVersions() {
    String baseUrl = 'https://repo1.maven.org/maven2/io/qameta/allure/allure-commandline/'
    URL metaUrl = new URL(baseUrl)

    WebClient wc = new WebClient()
    wc.setCssErrorHandler(new org.htmlunit.SilentCssErrorHandler());
    wc.getOptions().setThrowExceptionOnScriptError(false);
    wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
    HtmlPage p = wc.getPage(metaUrl);

    def versions = []
    def pattern = Pattern.compile("^([0-9]+.*)/\$");

    p.getAnchors().each {
        HtmlAnchor a ->
        def m = pattern.matcher(a.hrefAttribute)
        if (m.find()) {
            def ver = m.group(1)
            if (!ver.contains("BETA")) {
                versions.add(ver)
            }
        }
    }

    return versions.collect() { version ->
        return ["id"  : version,
                "name": version,
                "url" : String.format('%s%s/allure-commandline-%s.zip', baseUrl, version, version)
        ]
    }
}

def getSonatypeVersions() {
    String baseUrl = 'https://oss.sonatype.org/content/repositories/releases/ru/yandex/qatools/allure/allure-commandline/'
    URL metaUrl = new URL(baseUrl)

    WebClient wc = new WebClient()
    wc.setCssErrorHandler(new org.htmlunit.SilentCssErrorHandler());
    wc.getOptions().setThrowExceptionOnScriptError(false);
    wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
    HtmlPage p = wc.getPage(metaUrl);

    def versions = []
    def pattern = Pattern.compile("^([0-9]+.*)/\$");

    p.getAnchors().each {
        HtmlAnchor a ->
        def m = pattern.matcher(a.hrefAttribute)
        if (m.find()) {
            def ver = m.group(1)
            if (!ver.contains("RC")) {
                versions.add(ver)
            }
        }
    }

    return versions.collect() { version ->
        return ["id"  : version,
                "name": version,
                "url" : getSonatypeArtifactUrl(baseUrl, version)
        ]
    }
}

def getSonatypeArtifactUrl(String baseUrl, String version) {
    def artifactName = getSonatypeArtifactName(version);
    return String.format('%s%s/%s', baseUrl, version, artifactName);
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
