/**
 * Generate a file that lists all JDK IDs.
 */
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.gargoylesoftware.htmlunit.html.HtmlOption;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import net.sf.json.JSONObject;
import net.sf.json.JSONArray;

// family is a major release of JDK
class Family {
    String name;
    String entryPoint;
}

ID_PATTERN = Pattern.compile("ProductRef=([^&]+)"); // this is how we find a product code

wc = new WebClient()
wc.setJavaScriptEnabled(false);
wc.setCssEnabled(false);
HtmlPage p = getPage("http://java.sun.com/products/archive/");

JSONArray all = new JSONArray();

[
    new Family(name:"JDK 6",entryPoint:"6u12"),
    new Family(name:"JDK 5.0",entryPoint:"5.0_15"),
    new Family(name:"JDK 1.4",entryPoint:"1.4.2_16")
].collect(all) { Family f ->
    return new JSONObject().accumulate("name",f.name).accumulate("list",listFamily(p,f));
}

envelope = new JSONObject().accumulate("jdks", all)
System.out.println(envelope.toString(2));

if(project!=null) {
    // if we run from GMaven during a build, put that out in a file as well, with the JSONP support
    File d = new File(project.basedir, "target")
    d.mkdirs()
    new File(d,"hudson.tools.JDKInstaller.json").write("downloadService.post('hudson.tools.JDKInstaller',${envelope.toString()})");
}

JSONArray listFamily(HtmlPage p, Family f) throws Exception {
    JSONArray jdks = new JSONArray();

    HtmlElement o = p.selectSingleNode("//option[@value='http://java.sun.com/products/archive/j2se/${f.entryPoint}/index.html']");
    if (o == null) {
        System.err.println("Warning: no match for //option[@value='http://java.sun.com/products/archive/j2se/${f.entryPoint}/index.html'] in ${p.getDocumentURI()}}");
        return jdks;
    }
    HtmlSelect select = o.getParentNode();

    /* XXX this is failing to find 6u22...why?
    if(f.name=="JDK 6") {
        Pattern bareJDK = Pattern.compile("jdk-6u([0-9]+)-oth-JPR");
        HtmlForm form = getPage("http://java.sun.com/javase/downloads/widget/jdk6.jsp").forms.find { HtmlForm form ->
            return bareJDK.matcher(form.actionAttribute).find();
        };
        if (form == null) {
            System.err.println("Warning: no JDK link in http://java.sun.com/javase/downloads/widget/jdk6.jsp");
        } else {
            String pc = findProductCode(form.actionAttribute);
            def add = {String _pc ->
                Matcher m = bareJDK.matcher(_pc)
                m.find();
                jdks << makeJDK("6 Update " + m.group(1), _pc);
            }
            add(pc);
            add(pc.replace("21", "20"));
        }
    }
    */

    select.getOptions().collect(jdks) { HtmlOption opt ->
        return makeJDK(buildName(opt.getTextContent()),findID(f,opt.getValueAttribute()));
    }

    // archive page doesn't show the latest version of the release.
    // so try to guess them. If we have 6u22 on the archive page, maybe we have 6u23.
    if(f.name=="JDK 6") {
        try {
            Matcher idMatcher = Pattern.compile("\\Ajdk-6u(\\d+)-oth-JPR@CDS-CDS_Developer\\Z").matcher(jdks[0].id);
            Matcher nameMatcher = Pattern.compile("\\A6 Update (\\d+)\\Z").matcher(jdks[0].name);
            if (idMatcher.find() && nameMatcher.find() && idMatcher.group(1).equals(nameMatcher.group(1))) {
                Integer max = Integer.parseInt(idMatcher.group(1));
                // Try up to three versions after the max
                for (int i = max+1; i<=max+3; i++) {
                    try {
                        String id = "jdk-6u"+i+"-oth-JPR@CDS-CDS_Developer";
                        print("Trying "+ id +"\n");
                        HtmlPage pp = getPage("https://cds.sun.com/is-bin/INTERSHOP.enfinity/WFS/CDS-CDS_Developer-Site/en_US/-/USD/ViewProductDetail-Start?ProductRef="+id);
                        HtmlElement e = pp.getElementById("dnld_license"); // Is there a license checkbox?
                        if (e != null) {
                            jdks.add(0, makeJDK("6 Update " + i, id));
                        }
                    } catch (Error e) {
                    }
                }    
            }
        } catch (Error e) {
            print("Error doing version extension:" + e + "\n");
        }
    }

    return jdks;
}

JSONObject makeJDK(name,id) {
    return new JSONObject().accumulate("name",name).accumulate("id",id)
}

String buildName(String label) {
    // cut off extra explanation
    int idx = label.indexOf('+');
    if(idx>=0)  label=label.substring(0,idx);
    idx = label.indexOf('(');
    if(idx>=0)  label=label.substring(0,idx);
    return label.trim();
}


String findID(Family f, String href) throws Exception {

    HtmlPage p = getPage(href);
    HtmlAnchor a = p.getAnchors().find { HtmlAnchor a ->
        def m = ID_PATTERN.matcher(a.getHrefAttribute());
        return m.find() && m.group(1).contains("dk") && !m.group(1).contains("re")
    };

    if(a==null) {
        if (f.name=="JDK 5.0") {
            // JDK5 started hiding links in the survey, so if we can't figure it out,
            // assume the default naming convention
            def m = href =~ /j2se\/5.0_([0-9]+)\/index.html/
            if (m)
                return "jdk-1.5.0_${m.group(1)}-oth-JPR@CDS-CDS_Developer" as String;
        }
        throw new IllegalStateException("No JDK link in "+href);
    }
    return findProductCode(a.hrefAttribute);
}

String findProductCode(String url) {
    Matcher m = ID_PATTERN.matcher(url);
    if(m.find())
        return m.group(1);
    throw new IllegalStateException("Failed to find ID for " + url);
}

HtmlPage getPage(String url) {
    print("Fetching ${url} ...")
    long start = System.currentTimeMillis()
    HtmlPage p = wc.getPage(url)
    long end = System.currentTimeMillis()
    println("done (took ${end - start} msec)")
    p
}
