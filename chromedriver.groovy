#!./lib/runner.groovy
// Generates server-side metadata for chromedriver auto-installation
import net.sf.json.JSONObject
import groovy.json.JsonSlurper

def url = 'https://googlechromelabs.github.io/chrome-for-testing/latest-versions-per-milestone-with-downloads.json'
def jsonString = new URL(url).text
def jsonData = new JsonSlurper().parseText(jsonString)

def jsonList = []

// Extract information only for the "Stable" channel
jsonData.milestones.each { milestone ->
    def milestoneData = milestone.value

    def chromeDriverData = milestoneData.downloads.chromedriver
    chromeDriverData.each { entry ->
        def id = "${entry.platform}_${milestoneData.version}".toString()
        def entry_url = entry.url
        jsonList << ["id": id, "url": entry_url]
    }
}

lib.DataWriter.write("org.jenkins-ci.plugins.chromedriver.ChromeDriver",JSONObject.fromObject([list:jsonList]))
