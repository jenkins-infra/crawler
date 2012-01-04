#!/usr/bin/env groovy
// Generates index for scriptler scripts
@GrabResolver(name="m.g.o-public",root='http://maven.glassfish.org/content/group/public/')
@Grab(group="org.kohsuke.stapler",module="json-lib",version="2.1",classifier="jdk15")
import net.sf.json.*

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
        def metadata = JSONObject.fromObject(m[0][1]);
        metadata['script'] = f.name
        json << metadata
        
    }
}

JSONObject envelope = JSONObject.fromObject([list:json]);
println envelope.toString(2)

key = "org.jenkins-ci.plugins.scriptler.CentralScriptJsonCatalog";
File d = new File("target")
d.mkdirs()
new File(d,"${key}.json").write("downloadService.post('${key}',${envelope.toString(2)})");
