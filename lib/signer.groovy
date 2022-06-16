#!./lib/runner.groovy
import net.sf.json.*
import org.jvnet.hudson.update_center.Signer
import groovy.io.FileType

def list = []

def dir = new File('./target/')
dir.eachFileRecurse (FileType.FILES) { file ->
  if(file.getPath().endsWith('.json'))
    list << file;
}

list.each {
  String file  = it.path
  println "== ${file}"

  // Load content of JSON file to be signed
  File inputfile = new File(file)
  String utf8Content = inputfile.getText("UTF-8")
  if(utf8Content.length()<1) {
    println "ERROR: the file ${file} is empty. Exiting."
    System.exit(1)
  }

  // There is JSONp callback wrapping the envelope. Separator is the first comma (',').
  int commaIndex = utf8Content.indexOf(',')
  // Do not forget to remove the closing parenthses char at the end
  String jsonBody = utf8Content.substring(commaIndex + 1, utf8Content.length()-1)
  if(jsonBody.length()<1) {
    println "ERROR: the JSON body in the ${file} is empty. Exiting."
    System.exit(1)
  }

  JSONObject jsonContent = JSONObject.fromObject(jsonBody)

  // Sign the content (it adds JSON fields at thend)
  if (System.getenv("JENKINS_SIGNER")!=null) {
    println 'The environment variable "JENKINS_SIGNER" is defined: let\'s sign'
      new Signer().configureFromEnvironment().sign(jsonContent)
  }

  // Write HTML file (with signed content)
  new File("${file}.html").write("\uFEFF<!DOCTYPE html><html><head><meta http-equiv='Content-Type' content='text/html;charset=UTF-8' /></head><body><script>window.onload = function () { window.parent.postMessage(JSON.stringify(\n${jsonContent.toString(2)}\n),'*'); };</script></body></html>","UTF-8");
  println "== ${file}.html"
}
