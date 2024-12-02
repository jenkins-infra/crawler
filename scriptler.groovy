#!./lib/runner.groovy
// Generates index for scriptler scripts
import net.sf.json.*

def repository = 'https://github.com/jenkinsci/jenkins-scripts'
def branch = 'main'

def json = [];

def dir = new File("./jenkins-scripts");

void git(File workingDir, String... commands) {
    commands.each { cmd ->
        def fullCommand = "git -C $workingDir.canonicalPath $cmd"
        print "Executing \"${fullCommand}\"..."
        fullCommand.execute().waitFor()
        println ' done'
    }
}

if (dir.isDirectory() && new File(dir, ".git").isDirectory()) {
    git dir,
        "fetch origin $branch",
        "checkout origin/$branch"
} else {
    dir.mkdirs()
    git dir, "clone $repository . -b $branch"
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
