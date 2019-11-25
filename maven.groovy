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
    wc.setCssErrorHandler(new com.gargoylesoftware.htmlunit.SilentCssErrorHandler());
    wc.getOptions().setJavaScriptEnabled(false);
    wc.getOptions().setThrowExceptionOnScriptError(false);
    wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
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

def listFromUrl(url, pattern) {
    wc = new WebClient();
    wc.setCssErrorHandler(new com.gargoylesoftware.htmlunit.SilentCssErrorHandler());
    wc.getOptions().setJavaScriptEnabled(false);
    wc.getOptions().setThrowExceptionOnScriptError(false);
    wc.getOptions().setThrowExceptionOnFailingStatusCode(false);

    HtmlPage p = wc.getPage(url);
    initialurl = url;
    versionpattern = Pattern.compile("[0-9]([0-9.]+)");
    pattern=Pattern.compile(pattern);

    return p.getAnchors().collect { HtmlAnchor a ->
        m = versionpattern.matcher(a.hrefAttribute)
        if(m.find() ) {
            l=a.hrefAttribute.length()-2;
            ver=a.hrefAttribute.getAt(0..l);
            url = p.getFullyQualifiedUrl(a.hrefAttribute);
            HtmlPage pb = wc.getPage(url);
            return pb.getAnchors().collect { HtmlAnchor a1 ->
              n = pattern.matcher(a1.hrefAttribute)
              if (n.find()) {
                  urld = pb.getFullyQualifiedUrl(a1.hrefAttribute);
              return ["id":ver, "name":ver, "url":urld.toExternalForm()]
              }
              return null;
          }
        }
        return null;
    }.flatten().findAll { it!=null };
}

// Archives are coming from Apache
def listFromOldURL() {
    return listFromURL("https://archive.apache.org/dist/maven/binaries/")
}

// Recent releases are coming from Maven central
// Discussed with the Maven team here
// http://mail-archives.apache.org/mod_mbox/maven-dev/201505.mbox/%3cCAFNCU--iq2nYb3wnO715CbcXN+S8umRTyRnfk4_JSZ2qCR+1fg@mail.gmail.com%3e
def listFromNewUrl() {
    return listFromUrl("https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/", "maven-([0-9.]+)(-bin)?.zip\$")
}

def listAll() {
    return (listFromNewUrl() + listFromOldURL())
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
