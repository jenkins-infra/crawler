#!./lib/runner.groovy
// Generates server-side metadata for chromedriver auto-installation
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def url = 'https://googlechromelabs.github.io/chrome-for-testing/last-known-good-versions-with-downloads.json'
def jsonString = new URL(url).text
def jsonData = new JsonSlurper().parseText(jsonString)

def jsonList = []

// Extract information only for the "Stable" channel
def stableData = jsonData.channels.Stable
def chromeDriverData = stableData.downloads.chromedriver
chromeDriverData.each { entry ->
    def id = "${entry.platform}_${stableData.version}"
    def entry_url = entry.url
    jsonList << ["id": id, "url": entry_url]
}

lib.DataWriter.write("org.jenkins-ci.plugins.chromedriver.ChromeDriver",JSONObject.fromObject([list:json]));
