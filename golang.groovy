#!./lib/runner.groovy
import com.gargoylesoftware.htmlunit.html.HtmlAnchor
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.WebClient

import net.sf.json.*

// Fetch a single-page list of all available downloads (not just the current release).
// We can't just scrape the downloads Atom feed, as it only contains the current release
def baseUrl = "https://code.google.com/p/go/downloads/list?can=1&sort=-uploaded&num=10000"

// Build a map of Go versions -> platform archives
def releases = [:]

HtmlPage p = new WebClient().getPage(baseUrl)
p.selectNodes("//a[starts-with(@href, '//go.googlecode.com/files/')]").each { HtmlAnchor e ->

    def url = e.getHrefAttribute()

    // We only want release archives; ignore source packages and beta/RC versions
    if (url =~ /\.src\.tar\.gz$/ || url =~ /(beta|rc)\d+\./) {
        return
    }

    // Extract the version info from archive filename (ignore .msi or .pkg installers), e.g.:
    //  go1.2.1.darwin-amd64-osx10.8.tar.gz
    //  go.go1.windows-386.zip
    def parts = (url =~ /go(?:\.go)?(\d(?:\.\d)*)\.(?:([^-]+)-([^-]+)(?:-(.+))?)\.(?:tar\.gz|zip)/)
    if (!parts) {
        return
    }

    // Gather the info for this archive
    def variant = [:]
    variant.url = "https:" + url
    variant.os = parts[0][2]
    variant.arch = parts[0][3]
    if (parts[0][4] && parts[0][4].startsWith("osx")) {
        variant.osxversion = parts[0][4].substring("osx".length())
    }

    // Add it to the list of variants for this version of Go
    def version = parts[0][1]
    if (!releases[version]) {
        releases[version] = []
    }
    releases[version] << variant
}

// Build the JSON structure: a list of release objects, each with its platform-specific variants
def json = [releases: releases.collect { key, value -> ["id": key, "name": "Go ${key}".toString(), "variants": value] }]

// Write the JSON update file
lib.DataWriter.write("org.jenkinsci.plugins.golang.GolangInstaller", JSONObject.fromObject(json))
