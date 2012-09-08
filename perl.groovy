#!/usr/bin/env groovy
// Generates server-side metadata for Perl auto-installation
@GrabResolver(name="repo.jenkins-ci.org",root='http://repo.jenkins-ci.org/public/')
@Grab(group="org.jvnet.hudson",module="htmlunit",version="2.2-hudson-9")
@Grab(group="org.jenkins-ci",module="update-center2",version="1.20")
import com.gargoylesoftware.htmlunit.html.*;

import net.sf.json.*
import com.gargoylesoftware.htmlunit.WebClient
import org.jvnet.hudson.update_center.Signer

def wc = new WebClient()
def baseUrl = 'http://www.cpan.org/src/5.0/'
//HtmlPage p = wc.getPage('file:///home/gavinm/workspace/backend-crawler/index.html');
HtmlPage p = wc.getPage('http://search.cpan.org/dist/perl/');

def json = [];

//p.getElementsByName("url").reverse().collect { HtmlOption e ->
def select = p.getElementsByName("url").get(0);
select.getChildElements().each { e ->
    def ver = (e.getText() =~ /^perl-([^ ]+).*--/)
    if (ver) {
        def url = baseUrl + "perl-" + ver[0][1] + ".tar.gz"; 
//        println url 
        json << ["id":ver[0][1], "name": "Perl ${ver[0][1]}".toString(), "url":url];
    }
}

//json = json.sort{a,b -> new VersionNumber(a.id).compareTo(new VersionNumber(b.id)) }
json = json.sort{a,b -> a.id.compareTo(b.id) }
JSONObject envelope = JSONObject.fromObject([list:json]);
new Signer().configureFromEnvironment().sign(envelope);
println envelope.toString(2)

key = "org.jenkinsci.plugins.perlinstaller.PerlInstaller";
File d = new File("target")
d.mkdirs()
new File(d,"${key}.json").write("downloadService.post('${key}',${envelope.toString(2)})");
