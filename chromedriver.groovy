#!./lib/runner.groovy
// Generates server-side metadata for chromedriver auto-installation
import net.sf.json.*
import groovy.util.XmlSlurper

def baseUrl = 'https://chromedriver.storage.googleapis.com/'
def json = []
def map = [:].withDefault {[:].withDefault {[:]}}

def xml = new XmlSlurper().parseText(new URL(baseUrl).text)
xml.Contents.each {
    def key = it.Key.text()
    def m = key =~ /(.*)\/chromedriver_(.*?)(\d+)\.zip/
    if (m) {
        def (_, version, os, arch) = m[0]
        map[os][arch][version] = key
        json << ["id":"${os}${arch}_${version}".toString(), "url":baseUrl + key];
    }
}

map.each { os, arch ->
    if (arch.size() == 1) {
        def latest = arch.find().value.max { a, b ->
            List verA = a.key.tokenize('.')
            List verB = b.key.tokenize('.')
            def commonIndices = Math.min(verA.size(), verB.size())
            for (int i = 0; i < commonIndices; ++i) {
                def numA = verA[i].toInteger()
                def numB = verB[i].toInteger()
                if (numA != numB) {
                    return numA <=> numB
                }
            }
            verA.size() <=> verB.size() }
        json << ["id":os, "url":baseUrl + latest.value];
    } else {
        arch.each { k, v ->
            def latest = v.max { a, b ->
                List verA = a.key.tokenize('.')
                List verB = b.key.tokenize('.')
                def commonIndices = Math.min(verA.size(), verB.size())
                for (int i = 0; i < commonIndices; ++i) {
                    def numA = verA[i].toInteger()
                    def numB = verB[i].toInteger()
                    if (numA != numB) {
                        return numA <=> numB
                    }
                }
                verA.size() <=> verB.size() }
            json << ["id":os + k, "url":baseUrl + latest.value];
        }
    }
}

lib.DataWriter.write("org.jenkins-ci.plugins.chromedriver.ChromeDriver",JSONObject.fromObject([list:json]));
