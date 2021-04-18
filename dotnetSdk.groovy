#!./lib/runner.groovy
// Generates data files used by the dotnet-sdk-plugin, both for SDK installation and for auto-completion.

import com.gargoylesoftware.htmlunit.Page
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler
import com.gargoylesoftware.htmlunit.WebClient
import net.sf.json.JSONArray
import net.sf.json.JSONException
import net.sf.json.JSONObject
import org.apache.commons.lang3.StringUtils

//region Utilities

static WebClient createWebClient() {
    final WebClient wc = new WebClient()
    wc.setCssErrorHandler(new SilentCssErrorHandler())
    wc.options.javaScriptEnabled = false
    wc.options.throwExceptionOnFailingStatusCode = false
    wc.options.throwExceptionOnScriptError = false
    return wc
}

void createDownloadable(String className, JSONObject object) {
    lib.DataWriter.write('io.jenkins.plugins.dotnet.data.' + className, object)
}

static JSONObject fetchJson(String url) {
    //System.out.printf('fetching JSON from %s%n', url)
    final WebClient wc = createWebClient()
    final Page p = wc.getPage(url)
    return JSONObject.fromObject(p.webResponse.contentAsString)
}


//endregion

Throwable failure = null

private Throwable addFailure(Throwable failure, Throwable t) {
    if (failure == null) {
        failure = new IOException("Failed to create one or more output files.")
    }
    failure.addSuppressed(t)
    return failure
}

//region File 1: Framework Monikers

private void createFrameworkMonikers() {
    // https://docs.microsoft.com/en-us/dotnet/standard/frameworks is referenced by the .NET CLI docs as the reference
    // but does not actually have a complete list. Nor does there seem to be another source. So for now, this just takes
    // a file from the plugin repository as-is.
    final JSONObject tfmList = fetchJson('https://raw.githubusercontent.com/jenkinsci/dotnet-sdk-plugin/master/downloadables/tfm-list.json')
    createDownloadable('Framework', tfmList)
}

try {
    createFrameworkMonikers()
}
catch (Throwable t) {
    failure = addFailure(failure, t)
}

//endregion

//region File 2: RID Catalog

private void createRidCatalog() {
    final JSONObject ridCatalog = fetchJson('https://raw.githubusercontent.com/dotnet/runtime/master/src/libraries/Microsoft.NETCore.Platforms/src/runtime.json')
    final String[] rids = ridCatalog.getJSONObject('runtimes').keySet().toArray()
    // TODO: Maybe sort this list so that names with fewer parts sort before those with more.
    // TODO: Specifically, sort 'tizen-4.0.0' and 'tizen-5.0.0' before 'tizen-4.0.0-x64' and 'tizen-5.0.0-x64'
    final JSONObject data = JSONObject.fromObject([ 'ridCatalog': rids ])
    createDownloadable('Runtime', data)
}

try {
    createRidCatalog()
}
catch (Throwable t) {
    failure = addFailure(failure, t)
}

//endregion

//region File 3: SDK Downloads

private String getSdkInfo(JSONObject s) {
    final StringBuilder info = new StringBuilder()
    def value = s.get('vs-support')
    if (value instanceof String && !value.isEmpty()) {
        info.append(value)
    }
    else {
        value = s.get('vs-version')
        if (value instanceof String && !value.isEmpty()) {
            info.append('Visual Studio ').append(value)
        }
    }
    value = s.get('csharp-version')
    if (value instanceof String && !value.isEmpty()) {
        if (info.size() > 0) {
            info.append(', ')
        }
        info.append('C# ').append(value)
    }
    value = s.get('fsharp-version')
    if (value instanceof String && !value.isEmpty()) {
        if (info.size() > 0) {
            info.append(', ')
        }
        info.append('F# ').append(value)
    }
    value = s.get('vb-version')
    if (value instanceof String && !value.isEmpty()) {
        if (info.size() > 0) {
            info.append(', ')
        }
        info.append('VB ').append(value)
    }
    if (info.size() == 0) {
        return null
    }
    return info.toString()
}

private def getSdk(sdks, JSONObject s) {
    def name = s.get('version-display')
    if (name instanceof JSONObject && name.isNullObject()) {
        name = null
    }
    if (name == null) { // some SDKs have version-display as null; fall back on the raw version
        name = s.get('version')
        if (name instanceof JSONObject && name.isNullObject()) {
            name = null
        }
    }
    if (name == null || !(name instanceof String)) {
        throw new JSONException('SDK has neither a version-display nor a version property set.')
    }
    def sdk = [:]
    sdk['name'] = name
    if (sdks.containsKey(name)) {
        return sdk // Assumption: SDK of same name has same contents
    }
    def info = getSdkInfo(s)
    if (info != null) {
        sdk['info'] = info
    }
    def packages = []
    def urls = []
    def rids = [:]
    for (JSONObject p : s.getJSONArray('files')) {
        final String fileName = p.getString('name')
        if (fileName == null || (!fileName.endsWith('.zip') && !fileName.endsWith('.tar.gz'))) {
            continue
        }
        def pkg = [:]
        final String rid = p.getString('rid')
        if (rids.containsKey(rid)) {
            // Avoid listing two files for the same rid; prefer the .tar.gz because it's slightly smaller.
            if (fileName.endsWith('.tar.gz')) {
                packages.remove(rids[rid])
            }
            else {
                continue
            }
        }
        pkg['rid'] = rid
        rids[rid] = pkg
        if (rid != null) {
            String osAndVersion = rid
            String arch
            if (rid.endsWith('-arm')) {
                arch = 'ARM32'
                osAndVersion = rid.substring(0, rid.length() - 4)
            }
            else if (rid.endsWith('-arm64')) {
                arch = 'ARM64'
                osAndVersion = rid.substring(0, rid.length() - 6)
            }
            else if (rid.endsWith('-x64')) {
                arch = 'x64'
                osAndVersion = rid.substring(0, rid.length() - 4)
            }
            else if (rid.endsWith('-x86')) {
                arch = 'x86'
                osAndVersion = rid.substring(0, rid.length() - 4)
            }
            else {
                arch = "???"
            }
            final String[] parts = osAndVersion.split(/[.]/)
            String os = parts[0]
            String version = parts.length > 1 ? parts[1] : null
            switch (os) {
                case 'centos':
                    os = 'CentOS'
                    break
                case 'debian':
                    os = 'Debian'
                    break
                case 'fedora':
                    os = 'Fedora'
                    break
                case 'linux':
                    os = 'Linux'
                    break
                case 'linux-musl':
                    os = 'Alpine Linux'
                    break
                case 'opensuse':
                    os = 'OpenSUSE'
                    break
                case 'osx':
                    os = 'macOS'
                    break
                case 'rhel':
                    os = 'RHEL'
                    break
                case 'ubuntu':
                    os = 'Ubuntu'
                    break
                case 'win':
                    os = 'Windows'
                    break
                default:
                    //System.err.printf('NO MAPPING DEFINED FOR OS PART (%s) OF RID "%s"%n', os, rid)
                    os = '???'
            }
            version = (version != null) ? (' ' + version) : ''
            arch = (arch != null) ? ' - ' + arch : ''
            pkg['platform'] = os + version + arch
        }
        // Not currently used or needed, and it makes the file much bigger
        // pkg['hash'] = p.getString('hash')
        // Later SDKs also include an 'akams' key, with a (shorter) aka.ms link.
        // However, those links seem to be broken, so stick to 'url'.
        final String url = p.getString('url')
        pkg['url'] = url
        urls << url
        packages << pkg
    }
    if (urls.size() > 0) { // compute the URL prefix for the package (reduces file size)
        String urlPrefix = StringUtils.getCommonPrefix(urls as String[])
        if (!urlPrefix.endsWith('/')) {
            urlPrefix = urlPrefix.substring(0, urlPrefix.lastIndexOf('/') + 1)
        }
        if (urlPrefix.size() > 15) { // shorter is not worth emitting the property for
            for (def pkg : packages) {
                pkg['url'] = pkg['url'].substring(urlPrefix.size())
            }
            sdk['urlPrefix'] = urlPrefix
        }
    }
    sdk['packages'] = packages
    sdks[name] = sdk
    return sdk
}

private void createSdkDownloads() {
    def versions = []
    def sdks = [:]
    JSONObject releaseIndex = fetchJson('https://dotnetcli.blob.core.windows.net/dotnet/release-metadata/releases-index.json')
    for (JSONObject v : releaseIndex.getJSONArray('releases-index')) {
        def version = [:]
        version['name'] = v.getString('product') + ' ' + v.getString('channel-version')
        version['status'] = v.getString('support-phase').toUpperCase()
        version['endOfSupport'] = v.get('eol-date')
        def releases = []
        final JSONObject channel = fetchJson(v.getString('releases.json'))
        for (JSONObject r : channel.getJSONArray('releases')) {
            def release = [:]
            release['name'] = r.getString('release-version')
            release['released'] = r.getString('release-date')
            if (r.getBoolean('security')) {
                release['securityFixes'] = true
            }
            // Assumption based on Semantic Versioning
            if (r.getString('release-version').contains('-')) {
                release['preview'] = true
            }
            def releaseNotes = r.get('release-notes')
            if (releaseNotes instanceof String) {
                release['releaseNotes'] = releaseNotes
            }
            release['sdks'] = []
            // Older releases have only 'sdk'. Some have sdks: null. But when sdks is set, it always includes sdk.
            def releaseSdks = r.get('sdks')
            if (releaseSdks instanceof JSONObject && releaseSdks.isNullObject()) {
                releaseSdks = null
            }
            if (releaseSdks == null) {
                def s = r.get('sdk')
                if (s instanceof JSONObject && s.isNullObject()) {
                    s = null
                }
                if (s != null) {
                    def sdk = getSdk(sdks, s)
                    release['sdks'] << sdk['name']
                }
            }
            else {
                for (JSONObject s : releaseSdks) {
                    def sdk = getSdk(sdks, s)
                    release['sdks'] << sdk['name']
                }
            }
            releases << release
        }
        version['releases'] = releases
        versions << version
    }
    createDownloadable('Downloads', JSONObject.fromObject([ 'versions' : versions, 'sdks': sdks.values()]))
}

try {
    createSdkDownloads()
}
catch (Throwable t) {
    failure = addFailure(failure, t)
}

//endregion

if (failure != null) {
    throw failure
}
