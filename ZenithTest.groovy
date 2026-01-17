/* Test results of generators */

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.junit.jupiter.api.Test;

/**
 * Test to be run after all installer metadata has been generated.
 * Sanity checks for the installer metadata.
 */
class ZenithTest {

    void checkFileSize(String fileName) {
        long minimumSize = 500;
        File dataFile = new File("target/" + fileName);
        assertTrue(dataFile.exists(), "File target/" + fileName + " does not exist");

        long actualSize = dataFile.length();
        assertTrue(
                actualSize > minimumSize,
                "Size of target/" + fileName + " was " + actualSize + ", less than minimum " + minimumSize);
    }

    @Test
    void adoptopenjdk() {
        checkFileSize("io.jenkins.plugins.adoptopenjdk.AdoptOpenJDKInstaller.json");
    }

    // TODO: Uncomment after first run that provides non-empty data for Allure command line installer
    // @Test
    // void allure() {
    //     checkFileSize("ru.yandex.qatools.allure.jenkins.tools.AllureCommandlineInstaller.json");
    // }

    @Test
    void ant() {
        checkFileSize("hudson.tasks.Ant.AntInstaller.json");
    }

    @Test
    void buckminster() {
        checkFileSize("hudson.plugins.buckminster.BuckminsterInstallation.BuckminsterInstaller.json");
    }

    @Test
    void chromedriver() {
        checkFileSize("org.jenkins-ci.plugins.chromedriver.ChromeDriver.json");
    }

    @Test
    void cmake() {
        checkFileSize("hudson.plugins.cmake.CmakeInstaller.json");
    }

    @Test
    void codeql() {
        checkFileSize("io.jenkins.plugins.codeql.CodeQLInstaller.json");
    }

    @Test
    void consul() {
        checkFileSize("com.inneractive.jenkins.plugins.consul.ConsulInstaller.json");
    }

    @Test
    void dependencycheck() {
        checkFileSize("org.jenkinsci.plugins.DependencyCheck.tools.DependencyCheckInstaller.json");
    }

    @Test
    void dotnetSdk() {
        checkFileSize("io.jenkins.plugins.dotnet.data.Downloads.json");
    }

    @Test
    void flyway() {
        checkFileSize("sp.sd.flywayrunner.installation.FlywayInstaller.json");
    }

    @Test
    void golang() {
        checkFileSize("org.jenkinsci.plugins.golang.GolangInstaller.json");
    }

    @Test
    void gradle() {
        checkFileSize("hudson.plugins.gradle.GradleInstaller.json");
    }

    @Test
    void grails() {
        checkFileSize("com.g2one.hudson.grails.GrailsInstaller.json");
    }

    @Test
    void groovy() {
        checkFileSize("hudson.plugins.groovy.GroovyInstaller.json");
    }

    @Test
    void jdk() {
        checkFileSize("hudson.tools.JDKInstaller.json");
    }

    // TODO: Fix the leiningen generator and uncomment this test
    // @Test
    // void leiningen() {
    //     checkFileSize("org.jenkins-ci.plugins.leiningen.LeinInstaller.json");
    // }

    @Test
    void maven() {
        checkFileSize("hudson.tasks.Maven.MavenInstaller.json");
    }

    // TODO: Fix the mongodb generator and uncomment this test
    // @Test
    // void mongodb() {
    //     checkFileSize("org.jenkinsci.plugins.mongodb.MongoDBInstaller.json");
    // }

    @Test
    void nodejs() {
        checkFileSize("hudson.plugins.nodejs.tools.NodeJSInstaller.json");
    }

    @Test
    void packer() {
        checkFileSize("biz.neustar.jenkins.plugins.packer.PackerInstaller.json");
    }

    @Test
    void play() {
        checkFileSize("hudson.plugins.play.PlayInstaller.json");
    }

    @Test
    void recipe() {
        checkFileSize("org.jenkinsci.plugins.recipe.RecipeCatalog.json");
    }

    @Test
    void sbt() {
        checkFileSize("org.jvnet.hudson.plugins.SbtPluginBuilder.SbtInstaller.json");
    }

    @Test
    void sbuild() {
        checkFileSize("org.sbuild.jenkins.plugin.SBuildInstaller.json");
    }

    @Test
    void scala() {
        checkFileSize("hudson.plugins.scala.ScalaInstaller.json");
    }

    @Test
    void scriptler() {
        checkFileSize("org.jenkinsci.plugins.scriptler.CentralScriptJsonCatalog.json");
    }

    @Test
    void sonarqubescanner() {
        checkFileSize("hudson.plugins.sonar.SonarRunnerInstaller.json");
    }

    @Test
    void sonarqubescannermsbuild() {
        checkFileSize("hudson.plugins.sonar.MsBuildSonarQubeRunnerInstaller.json");
    }

    @Test
    void terraform() {
        checkFileSize("org.jenkinsci.plugins.terraform.TerraformInstaller.json");
    }
}
