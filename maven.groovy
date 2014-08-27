#!./lib/runner.groovy
// Generates server-side metadata for Maven
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlAnchor
import com.gargoylesoftware.htmlunit.html.HtmlPage
import java.util.regex.Pattern
import net.sf.json.JSONObject
import hudson.util.VersionNumber

def getHtmlPage(url) {
    def wc = new WebClient()
    wc.javaScriptEnabled = false
    wc.cssEnabled = false
    return wc.getPage(url)
}

def listFromURL(url) {
    def HtmlPage p = getHtmlPage(url)
    def pattern = Pattern.compile("maven-([0-9\\.]+)(-bin)?.zip\$")

    return p.getAnchors().collect { HtmlAnchor a ->
        def m = pattern.matcher(a.hrefAttribute)
        if (m.find()) {
            def ver = m.group(1)
            def fqUrl = p.getFullyQualifiedUrl(a.hrefAttribute).toExternalForm()
            return ["id": ver, "name": ver, "url": fqUrl]
        }
        return null
    }.findAll { it != null }
}

def listFromUrl(url, regex) {
    def HtmlPage p = getHtmlPage(url)
    def pattern = Pattern.compile(regex)

    return p.getAnchors().collect { HtmlAnchor a ->
        def m = pattern.matcher(a.hrefAttribute)
        if (m.matches()) {
            return p.getFullyQualifiedUrl(a.hrefAttribute)
        }
        return null
    }.findAll { it != null }
}

def listFromOldURL() {
    return listFromURL("http://archive.apache.org/dist/maven/binaries/")
}

def listFromNewUrl() {
    return listFromUrl("http://archive.apache.org/dist/maven/", "maven-([0-9])/\$")
            .collect { url -> listFromUrl(url, "([0-9\\.])+/\$") }.flatten()
            .collect { url -> listFromUrl(url, "^binaries/\$") }.flatten()
            .collect { url -> listFromURL(url) }.flatten()
}

def listAll() {
    return (listFromOldURL() + listFromNewUrl())
            .unique { o1, o2 -> o1.id <=> o2.id }
            .sort { o1, o2 ->
                try {
                    def v1 = new VersionNumber(o1.id)
                    try {
                        return new VersionNumber(o2.id).compareTo(v1)
                    } catch (IllegalArgumentException _2) {
                        return -1
                    }
                } catch (IllegalArgumentException _1) {
                    try {
                        new VersionNumber(o2.id)
                        return 1
                    } catch (IllegalArgumentException _2) {
                        return o2.id.compareTo(o1.id)
                    }
                }
            }
}

def store(key, o) {
    JSONObject envelope = JSONObject.fromObject(["list": o])
    lib.DataWriter.write(key, envelope)
}

store("hudson.tasks.Maven.MavenInstaller", listAll())
