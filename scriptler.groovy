#!./lib/runner.groovy
// Generates index for scriptler scripts
import net.sf.json.*

def json = [];

def dir = new File("./jenkins-scripts");

if (dir.isDirectory() && new File(dir, ".git").isDirectory()) {
    "git pull --rebase origin master".execute([], dir).waitFor()
} else {
     dir.mkdirs()
    "git clone https://github.com/jenkinsci/jenkins-scripts -b master".execute().waitFor()
}

def scriptlerDir = new File(dir, "scriptler")

scriptlerDir.eachFileMatch(~/.+\.groovy/) { File f ->
    if(f.name.equals('testMetaFormat.groovy')) {
        return
    }
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

lib.DataWriter.write("org.jenkinsci.plugins.scriptler.CentralScriptJsonCatalog",JSONObject.fromObject([list:json]));
