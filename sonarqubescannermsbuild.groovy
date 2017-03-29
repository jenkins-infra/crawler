#!./lib/runner.groovy
// Generates server-side metadata for MSBuild SonarQube Runner
import net.sf.json.*

def url = "https://api.github.com/repos/SonarSource/sonar-scanner-msbuild/releases".toURL()
def releases = JSONArray.fromObject(url.text)

def json = []

for (JSONObject release : releases) {
  def tagName = release.get("tag_name")
  if (!release.get("draft") && !release.get("prerelease") && !tagName.toLowerCase().contains("vsts")) {
    def fileName
    if (tagName.startsWith("1") || tagName.equals("2.0") || tagName.equals("2.1")) {
      fileName = "MSBuild.SonarQube.Runner-${tagName}.zip"
    } else {
      fileName = "sonar-scanner-msbuild-${tagName}.zip"
    }
    json << ["id": tagName,
             "name": "SonarQube Scanner for MSBuild ${tagName}".toString(), 
             "url": "https://github.com/SonarSource/sonar-scanner-msbuild/releases/download/${tagName}/${fileName}".toString()];
  }
}

lib.DataWriter.write("hudson.plugins.sonar.MsBuildSonarQubeRunnerInstaller",JSONObject.fromObject([list:json]));

