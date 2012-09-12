package lib;

import net.sf.json.JSONObject
import org.jvnet.hudson.update_center.Signer

/**
 * Writes out the JSON data file.
 */
public class DataWriter {
    public static void write(String key,JSONObject envelope) {
        println envelope.toString(2)

        // write unsigned data to *.json because of JENKINS-15105
        File d = new File("target")
        d.mkdirs()
        new File(d,"${key}.json").write("downloadService.post('${key}',${envelope.toString(2)})","UTF-8");

        // then signed data to *.json.html
        if (System.getenv("JENKINS_SIGNER")!=null)
            new Signer().configureFromEnvironment().sign(envelope);
        new File(d,"${key}.json.html").write("\uFEFF<!DOCTYPE html><html><head><meta http-equiv='Content-Type' content='text/html;charset=UTF-8' /></head><body><script>window.onload = function () { window.parent.postMessage(JSON.stringify(\n${envelope.toString(2)}\n),'*'); };</script></body></html>","UTF-8");
    }
}
