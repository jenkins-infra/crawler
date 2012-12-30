#!./lib/runner.groovy
// generate metadata for recipes
import net.sf.json.*
import java.util.zip.*

def branch = "inbound";

// def zip = new File("recipes.zip");
def zip = File.createTempFile("recipes","zip");
zip.deleteOnExit()
zip.withOutputStream { o ->
    new URL("https://github.com/jenkinsci/submitted-recipes/archive/${branch}.zip").withInputStream { i ->
        o << i;
    }
}

def json = [];

def z = new ZipFile(zip)
z.entries().each { e ->
    if (e.name.endsWith(".jrcp")) {
        def xml = new XmlSlurper().parse(z.getInputStream(e));
        def o = [:];
        ["id","version","displayName","description","author"].each { p ->
            o[p] = xml[p].toString();
        }
        o.timestamp = e.time
        json << o;
    }
}

lib.DataWriter.write("org.jenkinsci.plugins.recipe.RecipeCatalog",JSONObject.fromObject([list:json]));
