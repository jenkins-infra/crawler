#!./lib/runner.groovy
import com.gargoylesoftware.htmlunit.html.HtmlAnchor
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.WebClient

import net.sf.json.*

// Fetch the list of downloads from the Rust website
def downloadsUrl = "https://static.rust-lang.org/dist/index.html"

// Gather a list of URLs
def urls = []

// Fetch the page and gather the links
WebClient webClient = new WebClient()
webClient.setJavaScriptEnabled(false);
HtmlPage page = webClient.getPage(downloadsUrl)
page.selectNodes("//td/a").each { HtmlAnchor e ->
	urls << e.getHrefAttribute()
}

// Build a map of Rust versions -> platform archives
def releases = [:]
urls.each { url ->
    // We only want release archives; ignore source packages and beta/RC versions
    if (url =~ /\.src\.tar\.gz$/ || url =~ /.sha256|.asc$/ || url =~ /(beta|rc)\d+\./ || url =~ /bootstrap/) {
        return
    }

    // Extract the version info from archive filename (ignore .msi or .pkg installers), e.g.:
    //  go1.2.1.darwin-amd64-osx10.8.tar.gz
    //  go.go1.windows-386.zip
    //  go1.10.linux-amd64.tar.gz
    def parts = (url =~ /rust(?:\.rust)?-(\d(?:\.\d+)*)\.(?:([^-]+)-([^-]+)(?:-(.+))?)\.(?:tar\.gz|zip)/)
    if (!parts) {
        return
    }

    // Gather the info for this archive
    def variant = [:]
    variant.url = "https://static.rust-lang.org" + url
    variant.os = parts[0][4]
    variant.arch = parts[0][3]
    if (parts[0][4] && parts[0][4].startsWith("unknown-")) {
        variant.os = parts[0][4].substring("unknown-".length())
    }

    // Add it to the list of variants for this version of Rust
    def version = parts[0][1]
    if (!releases[version]) {
        releases[version] = []
    }
    releases[version] << variant
}

// Build the JSON structure: a list of release objects, each with its platform-specific variants
def json = [releases: releases.collect { key, value -> ["id": key, "name": "Rust ${key}".toString(), "variants": value] }]

// Write the JSON update file
lib.DataWriter.write("org.jenkinsci.plugins.rustlang.RustlangInstaller", JSONObject.fromObject(json))
