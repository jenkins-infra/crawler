#!./lib/runner.groovy
// Generates server-side metadata for SonarScanner for MSBuild
import net.sf.json.*

def url = "https://api.github.com/repos/SonarSource/sonar-scanner-msbuild/releases".toURL()
def releases = JSONArray.fromObject(url.text)

def json = []

for (JSONObject release : releases) {
  def tagName = release.get("tag_name")
  if (!release.get("draft") && !release.get("prerelease") && !tagName.toLowerCase().contains("vsts")) {

    if (tagName.startsWith("1") || tagName.equals("2.0") || tagName.equals("2.1")) {
       json << ["id": tagName,
              "name": "SonarScanner for MSBuild ${tagName}".toString(),
              "url": "https://github.com/SonarSource/sonar-scanner-msbuild/releases/download/${tagName}/MSBuild.SonarQube.Runner-${tagName}.zip".toString()];

    } else if (tagName.startsWith("2.") || tagName.startsWith("3.") || tagName.startsWith("4.0.")) {
      json << ["id": tagName,
              "name": "SonarScanner for MSBuild ${tagName}".toString(),
              "url": "https://github.com/SonarSource/sonar-scanner-msbuild/releases/download/${tagName}/sonar-scanner-msbuild-${tagName}.zip".toString()];

    } else {
      json << ["id": tagName,
             "name": "SonarScanner for MSBuild ${tagName} - .NET Fwk 4.6".toString(),
             "url": "https://github.com/SonarSource/sonar-scanner-msbuild/releases/download/${tagName}/sonar-scanner-msbuild-${tagName}-net46.zip".toString()];
      
      json << ["id": tagName,
             "name": "SonarScanner for MSBuild ${tagName} - .NET Fwk 5.0".toString(),
             "url": "https://github.com/SonarSource/sonar-scanner-msbuild/releases/download/${tagName}/sonar-scanner-msbuild-${tagName}-net5.0.zip".toString()];

      json << ["id": "${tagName}-netcore".toString(),
             "name": "SonarScanner for MSBuild ${tagName} - .NET Core 2.0".toString(),
             "url": "https://github.com/SonarSource/sonar-scanner-msbuild/releases/download/${tagName}/sonar-scanner-msbuild-${tagName}-netcoreapp2.0.zip".toString()];
    }
  }
}

lib.DataWriter.write("hudson.plugins.sonar.MsBuildSonarQubeRunnerInstaller",JSONObject.fromObject([list:json]));
