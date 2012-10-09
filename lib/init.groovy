// code to load all the necessary dependencies
// placed in a separate file to load this lazily after we set the necessary system property to work around Maven resolution
package lib;
import java.util.logging.*;

@GrabResolver(name="repo.jenkins-ci.org",root='http://repo.jenkins-ci.org/public/')
@GrabExclude('nekohtml:nekohtml')
@Grapes([
    @Grab("net.sourceforge.nekohtml:nekohtml:1.9.13"),
    @Grab("org.jenkins-ci:htmlunit:2.6-jenkins-4"),
    @Grab("org.jenkins-ci:update-center2:1.20")
])
class init {
    static {
        println "done"
        which org.apache.xerces.parsers.AbstractSAXParser.class
        which com.gargoylesoftware.htmlunit.html.HTMLParser.class
        which org.cyberneko.html.HTMLConfiguration.class

        Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
    }

    static void which(Class c) {
//        println c.classLoader.getResource(c.name.replace(".","/")+".class")
    }
}
