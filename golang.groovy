#!./lib/runner.groovy
import com.gargoylesoftware.htmlunit.html.HtmlAnchor
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.WebClient

import net.sf.json.*

// Fetch the list of downloads from the Go website
def downloadsUrl = "http://golang.org/dl/"

// Gather a list of URLs
def urls = []

// Disable JS, as we don't care about it
WebClient webClient = new WebClient()
webClient.setCssErrorHandler(new com.gargoylesoftware.htmlunit.SilentCssErrorHandler());
webClient.getOptions().setJavaScriptEnabled(false);
webClient.getOptions().setThrowExceptionOnScriptError(false);
webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

// Fetch the page and gather the links
HtmlPage page = webClient.getPage(downloadsUrl)
page.getByXPath("//td/a").each { HtmlAnchor e ->
	urls << e.getHrefAttribute()
}

// Build a map of Go versions -> platform archives
def releases = [:]
urls.each { url ->
    // We only want release archives; ignore source packages and beta/RC versions
    if (url =~ /\.src\.tar\.gz$/ || url =~ /(beta|rc)\d+\./ || url =~ /bootstrap/) {
        return
    }

    // Extract the version info from archive filename (ignore .msi or .pkg installers), e.g.:
    //  go1.2.1.darwin-amd64-osx10.8.tar.gz
    //  go.go1.windows-386.zip
    //  go1.10.linux-amd64.tar.gz
    def parts = (url =~ /go(?:\.go)?(\d(?:\.\d+)*)\.(?:([^-]+)-([^-]+)(?:-(.+))?)\.(?:tar\.gz|zip)/)
    if (!parts) {
        return
    }

    // Gather the info for this archive
    def variant = [:]
    variant.url = url
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
