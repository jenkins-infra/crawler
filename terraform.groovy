#!./lib/runner.groovy


import net.sf.json.JSONArray;
import net.sf.json.JSONObject;


def downloadUrl = "https://dl.bintray.com/mitchellh/terraform/"

def url = "https://api.bintray.com/v1/packages/mitchellh/terraform/terraform/files".toURL()

def files = JSONArray.fromObject(url.text)

def json = [];

for (JSONObject file : files) {
    def match = (file.get("path") =~ /.*(\d+.\d+.\d+)_(.*)_(.*).zip/)
    if (match) {
        def versionID = "${match[0][1]}-${match[0][2]}-${match[0][3]}".toString()
        json << [
                "id": versionID,
                "name": "Terraform ${match[0][1]} ${match[0][2]} (${match[0][3]})".toString(),
                "url": downloadUrl + file.get("path")
        ];
    }
}

json = json.reverse()

lib.DataWriter.write("org.jenkinsci.plugins.terraform.TerraformInstaller", JSONObject.fromObject([list:json]));
