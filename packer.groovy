#!./lib/runner.groovy
// Generates server-side metadata for Packer auto-installation
import com.gargoylesoftware.htmlunit.html.*;
import com.gargoylesoftware.htmlunit.WebClient

import com.gargoylesoftware.htmlunit.BrowserVersion

import net.sf.json.*

def url = "https://api.bintray.com/v1/packages/mitchellh/packer/packer/files".toURL()
def files = JSONArray.fromObject(url.text)

def json = [];

for (JSONObject file : files) {
    def m = (file.get("path") =~ /.*(\d+.\d+.\d+)_(.*)_(.*).zip/)
    if (m) {
        def verId = "${m[0][1]}-${m[0][2]}-${m[0][3]}".toString()
        json << ["id": verId, "name": "Packer ${m[0][1]} ${m[0][2]} (${m[0][3]})".toString(), "url": "https://dl.bintray.com/mitchellh/packer/" + file.get("path")];
    }
}

lib.DataWriter.write("biz.neustar.jenkins.plugins.packer.PackerInstaller",JSONObject.fromObject([list:json]));
