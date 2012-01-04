#!/usr/bin/env groovy
// Generates index for scriptler scripts
@GrabResolver(name="m.g.o-public",root='http://maven.glassfish.org/content/group/public/')
@Grab(group="org.kohsuke.stapler",module="json-lib",version="2.1",classifier="jdk15")
import net.sf.json.*

def json = [];

def dir = new File("./scriptler");

// TODO: update or clone from git

dir.eachFileMatch(~/.+\.groovy/) { File f ->
    def m = (f.text =~ /(?ms)BEGIN META(.+?)END META/)
    if (m) {
        def metadata = JSONObject.fromObject(m[0][1]);
        json << metadata
    }
}

JSONObject envelope = JSONObject.fromObject([list:json]);
println envelope.toString(2)

key = "org.jenkins-ci.plugins.scriptler.CentralScripter";
File d = new File("target")
d.mkdirs()
new File(d,"${key}.json").write("downloadService.post('${key}',${envelope.toString(2)})");
