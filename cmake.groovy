#!/usr/bin/env groovy
// Generates server-side metadata for CMake auto-installation
import com.gargoylesoftware.htmlunit.html.*;
import net.sf.json.*
import com.gargoylesoftware.htmlunit.WebClient

def baseUrl = 'http://www.cmake.org/files/'

def wc = new WebClient()
wc.setJavaScriptEnabled(false);
def releases = [:]

// Gather a list of top dirs
def dirs = []
wc.getPage(baseUrl).selectNodes("//td/a").reverse().each { HtmlAnchor e ->
    dirs << e.getHrefAttribute()
}
dirs.each { dir ->
    // We only want download dir of v 2.6 (for RHEL7 !) and above
    if (!(dir =~ /^v[2]\.[6-9].*/ || dir =~ /^v[3-9]\.\d+.*/)) {
        return
    }
    // gather archive files
    def files= []
    // get version urls
    wc.getPage(baseUrl+ dir).selectNodes("//td/a").reverse().each { HtmlAnchor e ->
        files << e.getHrefAttribute()
    }

    // Build a map of Cmake versions -> platform archives
    files.each { file ->
        // We only want release archives; ignore source packages, installers and beta/RC versions
        if (file =~ /\.src\.tar\.bz2$/ || file =~ /(alpha|beta|rc)\d+\./) {
            return
        }
        if (! (file =~ /\.(zip|tar\.gz)$/)) {
            return
        }

        // Extract the version info from archive filename
        //  cmake-2.6.4-Darwin-x86_64.tar.gz
        //  cmake-2.6.4-win32-i386.zip
        def parts = (file =~ /^cmake-(\d+\.\d+\.\d+)-(HP-UX|[^-]+)-([^-]+)\.(?:tar\.gz|zip)$/)
        if (!parts) {
            return
        }

        // Gather the info for this archive
        def variant = [:]
        variant.url = baseUrl+ dir+ file;
        variant.os = parts[0][2]
        variant.arch = parts[0][3]

        // Add it to the list of variants for this version of CMake
        def version = parts[0][1]
        if (!releases[version]) {
            releases[version] = []
        }
        releases[version] << variant
    }
}

// Build the JSON structure: a list of release objects, each with its platform-specific variants
def json = [list: releases.collect { key, value ->
        [ "id": key, "name": "${key}".toString(),
            "variants": value] }]
// Write the JSON update file
lib.DataWriter.write("hudson.plugins.cmake.CmakeInstaller",JSONObject.fromObject(json));
