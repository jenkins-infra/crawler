#!/usr/bin/env groovy
// Generates index for scriptler scripts
@GrabResolver(name="repo.jenkins-ci.org",root='http://repo.jenkins-ci.org/public/')
@Grab(group="org.jvnet.hudson",module="htmlunit",version="2.2-hudson-9")
@Grab(group="org.jenkins-ci",module="update-center2",version="1.20")
import net.sf.json.*
import org.jvnet.hudson.update_center.Signer

def json = [];

def dir = new File("./jenkins-scripts");

if (dir.isDirectory() && new File(dir, ".git").isDirectory()) {
    "git pull --rebase origin master".execute([], dir).waitFor()
} else {
     dir.mkdirs()
    "git clone git://github.com/jenkinsci/jenkins-scripts -b master".execute().waitFor()
}

def scriptlerDir = new File(dir, "scriptler")

scriptlerDir.eachFileMatch(~/.+\.groovy/) { File f ->
    def m = (f.text =~ /(?ms)BEGIN META(.+?)END META/)
    if (m) {
        try {
            def metadata = JSONObject.fromObject(m[0][1]);
            metadata['script'] = f.name
            json << metadata
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}

JSONObject envelope = JSONObject.fromObject([list:json]);
new Signer().configureFromEnvironment().sign(envelope);
println envelope.toString(2)

key = "org.jenkinsci.plugins.scriptler.CentralScriptJsonCatalog";
File d = new File("target")
d.mkdirs()
new File(d,"${key}.json").write("downloadService.post('${key}',${envelope.toString(2)})");
