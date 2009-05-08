import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlAnchor
import com.gargoylesoftware.htmlunit.html.HtmlPage
import java.util.regex.Pattern
import net.sf.json.JSONObject

def listUp(url,pattern) {
    wc = new WebClient()
    wc.setJavaScriptEnabled(false);
    wc.setCssEnabled(false);
    HtmlPage p = wc.getPage(url);
    pattern=Pattern.compile(pattern);

    return p.getAnchors().collect { HtmlAnchor a ->
        m = pattern.matcher(a.hrefAttribute)
        if(m.find()) {
            ver=m.group(1)
            url = p.getFullyQualifiedUrl(a.hrefAttribute);
            return ["version":ver, "url":url.toExternalForm()]
        }
        return null;
    }.findAll { it!=null }
}

def store(key,jsonp,o) {
    JSONObject envelope = JSONObject.fromObject(["${key}": o]);
    println envelope.toString(2)

    if(project!=null) {
        // if we run from GMaven during a build, put that out in a file as well, with the JSONP support
        File d = new File(project.basedir, "target")
        d.mkdirs()
        new File(d,"${key}.json").write("${jsonp}(${envelope.toString()})");
    }
}


store("ant","listOfAnts",    listUp("http://archive.apache.org/dist/ant/binaries/","ant-(.+)-bin.zip\$"))
store("maven","listOfMavens",listUp("http://archive.apache.org/dist/maven/binaries/","maven-([0-9.]+)(-bin)?.zip\$"))
