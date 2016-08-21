#!./lib/runner.groovy
// Generates server-side metadata for MongoDB auto-installation
import com.gargoylesoftware.htmlunit.html.*;
import net.sf.json.*
import com.gargoylesoftware.htmlunit.WebClient

def wc = new WebClient()
def json = [];
[osx: ['i386', 'x86_64'],
    linux: ['i686', 'x86_64'],
    win32: ['i386', 'x86_64'],
    sunos5: ['i86pc', 'x86_64']
].each { osname, archs -> archs.each { arch ->
    HtmlPage p = wc.getPage("http://dl.mongodb.org/dl/$osname/$arch")
    p.selectNodes("//a[@href]").reverse().collect { HtmlAnchor e ->
        def m = e.getHrefAttribute() =~ /^.*mongodb-$osname-$arch-(.*?)\.(tgz|zip)$/
        if (m) {
            String version = "${osname}-${arch}-${m[0][1]}"
            json << [id:version, name:version, url:m[0][0].replace('http://', 'https://')]
        }
    }
}}

lib.DataWriter.write("org.jenkinsci.plugins.mongodb.MongoDBInstaller",JSONObject.fromObject([list:json]));
