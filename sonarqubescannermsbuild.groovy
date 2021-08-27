#!./lib/runner.groovy
// Generates server-side metadata for SonarScanner for MSBuild
import net.sf.json.*

def url = "https://api.github.com/repos/SonarSource/sonar-scanner-msbuild/releases".toURL()
def releases = JSONArray.fromObject(url.text)

def json = []

def maxNumberOfReleases = 2
  
for (i = 0; i < maxNumberOfReleases - 1; i++) {
  JSONObject release = releases[i]
  def tagName = release.get("tag_name")
  if (!release.get("draft") && !release.get("prerelease") && !tagName.toLowerCase().contains("vsts")) {
    
      json << ["id": tagName,
             "name": "SonarScanner for .NET ${tagName} - .NET Fwk 4.6".toString(),
             "url": "https://github.com/SonarSource/sonar-scanner-msbuild/releases/download/${tagName}/sonar-scanner-msbuild-${tagName}-net46.zip".toString()];

      json << ["id": "${tagName}-net5".toString(),
             "name": "SonarScanner for .NET ${tagName} - .NET 5.0".toString(),
             "url": "https://github.com/SonarSource/sonar-scanner-msbuild/releases/download/${tagName}/sonar-scanner-msbuild-${tagName}-net5.0.zip".toString()];

      json << ["id": "${tagName}-netcore".toString(),
             "name": "SonarScanner for .NET ${tagName} - .NET Core 2.0".toString(),
             "url": "https://github.com/SonarSource/sonar-scanner-msbuild/releases/download/${tagName}/sonar-scanner-msbuild-${tagName}-netcoreapp2.0.zip".toString()];

      json << ["id": "${tagName}-netcore3".toString(),
             "name": "SonarScanner for .NET ${tagName} - .NET Core 3.0".toString(),
             "url": "https://github.com/SonarSource/sonar-scanner-msbuild/releases/download/${tagName}/sonar-scanner-msbuild-${tagName}-netcoreapp3.0.zip".toString()];
  }
}

lib.DataWriter.write("hudson.plugins.sonar.MsBuildSonarQubeRunnerInstaller",JSONObject.fromObject([list:json]));
