#!groovy
// not really

properties([
        buildDiscarder(logRotator(numToKeepStr: '5')),
])

node('linux') {
    stage ('Prepare') {
        deleteDir()
        checkout scm
    }

    stage ('Generate') {
        withEnv([
                "PATH+GROOVY=${tool 'groovy'}/bin",
                "JAVA_HOME=${tool 'jdk8'}",
                "PATH+JAVA=${tool 'jdk8'}/bin"
    ]) {
            sh '''
                for f in *.groovy
                do
                  groovy -Dgrape.config=./grapeConfig.xml ./lib/runner.groovy $f || true
                done
            '''
        }
    }

    stage('Archive') {
        dir ('target') {
            archiveArtifacts '**'
        }
    }
}
