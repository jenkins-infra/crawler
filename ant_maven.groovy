#!./lib/runner.groovy
// Generates server-side metadata for Ant & Maven
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlAnchor
import com.gargoylesoftware.htmlunit.html.HtmlPage
import java.util.regex.Pattern
import net.sf.json.JSONObject
import hudson.util.VersionNumber

// order version 
def sortingversion = {
    Object o1, Object o2 ->
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

// build list for binaries 
// ant
// old maven layout for version <3.0.4
def listUp(url,pattern) {
    wc = new WebClient()
    wc.javaScriptEnabled = false;
    wc.cssEnabled = false;
    HtmlPage p = wc.getPage(url);
    pattern=Pattern.compile(pattern);

    return p.getAnchors().collect { HtmlAnchor a ->
        m = pattern.matcher(a.hrefAttribute)
        if(m.find()) {
            ver=m.group(1)
            url = p.getFullyQualifiedUrl(a.hrefAttribute);
            return ["id":ver, "name":ver, "url":url.toExternalForm()]
        }
        return null;
    }.findAll { it!=null };    
}

// build list for binaries 
// use for new maven layout starting 3.0.4
def altListUp(url,pattern) {
    wc = new WebClient();
    wc.javaScriptEnabled = false;
    wc.cssEnabled = false;

    HtmlPage p = wc.getPage(url);
    initialurl = url;
    versionpattern = Pattern.compile("[0-9]([0-9.]+)");
    pattern=Pattern.compile(pattern);
    
    return p.getAnchors().collect { HtmlAnchor a ->
        m = versionpattern.matcher(a.hrefAttribute)
        if(m.find() ) {
            l=a.hrefAttribute.length()-2;
            ver=a.hrefAttribute.getAt(0..l);
            url = p.getFullyQualifiedUrl(a.hrefAttribute+"binaries/");
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

def store(key,o) {
    JSONObject envelope = JSONObject.fromObject(["list": o]);
    lib.DataWriter.write(key,envelope);
}

store("hudson.tasks.Ant.AntInstaller",  listUp("http://archive.apache.org/dist/ant/binaries/",  "ant-([0-9.]+)-bin.zip\$").sort ( sortingversion  ))
store("hudson.tasks.Maven.MavenInstaller", listUp("http://archive.apache.org/dist/maven/binaries/","maven-([0-9.]+)(-bin)?.zip\$").plus(altListUp("http://archive.apache.org/dist/maven/maven-3/","maven-([0-9.]+)(-bin)?.zip\$")).unique(false) {
        a ,b -> a.id <=> b.id
}.sort(sortingversion))
