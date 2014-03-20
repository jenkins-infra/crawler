#!./lib/runner.groovy
// Generates server-side metadata for SBuild auto-installation

import net.sf.json.*
import groovy.util.ConfigSlurper

def url = "http://sbuild.org/download/sbuild-jenkins-plugin/sbuild-releases.properties".toURL()
def text = url.text
def json = []

text.split("\n").each { line ->
        def pair = line.split("=", 2)
        def version = pair[0]
        def dist = pair[1]
	json << ["id":version, "name":"SBuild ${version}".toString(), "url":dist];
}

def result = JSONObject.fromObject([list:json])

lib.DataWriter.write("org.sbuild.jenkins.plugin.SBuildInstaller", result);
