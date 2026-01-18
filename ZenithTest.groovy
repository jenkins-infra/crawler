/* Test results of generators */

import static org.junit.Assert.assertTrue;

import java.io.File;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test to be run after all installer metadata has been generated.
 * Sanity checks for the installer metadata.
 */
class ZenithTest {

    void checkFileSize(long minimumSize, String fileName) {
        File dataFile = new File("target/" + fileName);
        assertTrue("File target/" + fileName + " does not exist", dataFile.exists());

        long actualSize = dataFile.length();
        assertTrue(
                "Size of target/" + fileName + " was " + actualSize + ", less than minimum " + minimumSize,
                actualSize > minimumSize,
                );
    }

    @Test
    void adoptopenjdk() {
        checkFileSize(300_000L, "io.jenkins.plugins.adoptopenjdk.AdoptOpenJDKInstaller.json");
    }

    // TODO: Enable after first run that provides non-empty data for Allure command line installer
    @Test
    @Ignore
    void allure() {
        checkFileSize("ru.yandex.qatools.allure.jenkins.tools.AllureCommandlineInstaller.json");
    }

    @Test
    void ant() {
        checkFileSize(4_000L, "hudson.tasks.Ant.AntInstaller.json");
    }

    @Test
    void buckminster() {
        checkFileSize(4_000L, "hudson.plugins.buckminster.BuckminsterInstallation.BuckminsterInstaller.json");
    }

    @Test
    void chromedriver() {
        checkFileSize(10_000L, "org.jenkins-ci.plugins.chromedriver.ChromeDriver.json");
    }

    @Test
    void cmake() {
        checkFileSize(200_000L, "hudson.plugins.cmake.CmakeInstaller.json");
    }

    @Test
    void codeql() {
        checkFileSize(400L, "io.jenkins.plugins.codeql.CodeQLInstaller.json");
    }

    @Test
    void consul() {
        checkFileSize(400_000L, "com.inneractive.jenkins.plugins.consul.ConsulInstaller.json");
    }

    @Test
    void dependencycheck() {
        checkFileSize(15_000L, "org.jenkinsci.plugins.DependencyCheck.tools.DependencyCheckInstaller.json");
    }

    @Test
    void dotnetSdk() {
        checkFileSize(90_000L, "io.jenkins.plugins.dotnet.data.Downloads.json");
    }

    @Test
    void flyway() {
        checkFileSize(15_000L, "sp.sd.flywayrunner.installation.FlywayInstaller.json");
    }

    @Test
    void golang() {
        checkFileSize(600_000L, "org.jenkinsci.plugins.golang.GolangInstaller.json");
    }

    @Test
    void gradle() {
        checkFileSize(60_000L, "hudson.plugins.gradle.GradleInstaller.json");
    }

    @Test
    void grails() {
        checkFileSize(24_000L, "com.g2one.hudson.grails.GrailsInstaller.json");
    }

    @Test
    void groovy() {
        checkFileSize(20_000L, "hudson.plugins.groovy.GroovyInstaller.json");
    }

    @Test
    void jdk() {
        checkFileSize(400_000L, "hudson.tools.JDKInstaller.json");
    }

    // TODO: Fix the leiningen generator and stop ignoring this test
    @Test
    @Ignore
    void leiningen() {
        checkFileSize("org.jenkins-ci.plugins.leiningen.LeinInstaller.json");
    }

    @Test
    void maven() {
        checkFileSize(8_000L, "hudson.tasks.Maven.MavenInstaller.json");
    }

    // TODO: Fix the mongodb generator and stop ignoring this test
    @Test
    @Ignore
    void mongodb() {
        checkFileSize("org.jenkinsci.plugins.mongodb.MongoDBInstaller.json");
    }

    @Test
    void nodejs() {
        checkFileSize(80_000L, "hudson.plugins.nodejs.tools.NodeJSInstaller.json");
    }

    @Test
    void packer() {
        checkFileSize(300_000L, "biz.neustar.jenkins.plugins.packer.PackerInstaller.json");
    }

    @Test
    void play() {
        checkFileSize(8_000L, "hudson.plugins.play.PlayInstaller.json");
    }

    @Test
    void recipe() {
        checkFileSize(2_000L, "org.jenkinsci.plugins.recipe.RecipeCatalog.json");
    }

    @Test
    void sbt() {
        checkFileSize(10_000L, "org.jvnet.hudson.plugins.SbtPluginBuilder.SbtInstaller.json");
    }

    @Test
    void sbuild() {
        checkFileSize(2_000L, "org.sbuild.jenkins.plugin.SBuildInstaller.json");
    }

    @Test
    void scala() {
        checkFileSize(10_000L, "hudson.plugins.scala.ScalaInstaller.json");
    }

    @Test
    void scriptler() {
        checkFileSize(24_000L, "org.jenkinsci.plugins.scriptler.CentralScriptJsonCatalog.json");
    }

    @Test
    void sonarqubescanner() {
        checkFileSize(8_000L, "hudson.plugins.sonar.SonarRunnerInstaller.json");
    }

    @Test
    void sonarqubescannermsbuild() {
        checkFileSize(60_000L, "hudson.plugins.sonar.MsBuildSonarQubeRunnerInstaller.json");
    }

    @Test
    void terraform() {
        checkFileSize(70_000L, "org.jenkinsci.plugins.terraform.TerraformInstaller.json");
    }
}
