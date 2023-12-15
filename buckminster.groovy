#!./lib/runner.groovy
// Generates server-side metadata for Buckminster auto-installation
import org.htmlunit.html.*;

import net.sf.json.*
import org.htmlunit.WebClient

def url = "https://raw.github.com/jenkinsci/buckminster-plugin/master/updates/buckminster.json".toURL()
def text = url.text

//println text

lib.DataWriter.write("hudson.plugins.buckminster.BuckminsterInstallation.BuckminsterInstaller",JSONObject.fromObject(text));
