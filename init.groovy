// code to load all the necessary dependencies
// placed in a separate file to load this lazily after we set the necessary system property to work around Maven resolution

@GrabResolver(name="repo.jenkins-ci.org",root='http://repo.jenkins-ci.org/public/')
@Grab(group="org.jvnet.hudson",module="htmlunit",version="2.2-hudson-9")
@Grab(group="org.jenkins-ci",module="update-center2",version="1.20")
class init {
    static {
        println "done"
    }
}
