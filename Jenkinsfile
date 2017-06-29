#!/usr/bin/env groovy


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
                "PATH+MVN=${tool 'mvn'}/bin",
                "JAVA_HOME=${tool 'jdk8'}",
                "PATH+JAVA=${tool 'jdk8'}/bin"
    ]) {
            String command = '''
                mvn -e clean install;

                for f in *.groovy
                do
                  groovy -Dgrape.config=./grapeConfig.xml ./lib/runner.groovy $f || true
                done
            '''
            if (infra.isTrusted()) {
                withCredentials([[$class: 'ZipFileBinding', credentialsId: 'update-center-signing', variable: 'SECRET']]) {
                    withEnv([
                        'JENKINS_SIGNER="-key \"$SECRET/update-center.key\" -certificate \"$SECRET/update-center.cert\" -root-certificate \"$SECRET/jenkins-update-center-root-ca.crt\"',
                    ]) {
                        sh command
                    }
                }
            }
            else {
                sh command
            }
        }
    }

    stage('Archive') {
        dir ('target') {
            archiveArtifacts '**'
        }
    }

    if (infra.isTrusted()) {
        stage('Publish') {
            dir('updates') {
                sh 'cp target/*.json target/*.html updates'
            }
            sshagent(['updates-rsync-key']) {
                sh 'rsync -avz  -e \'ssh -o StrictHostKeyChecking=no\' --exclude=.svn updates/ www-data@updates.jenkins.io:/var/www/updates.jenkins.io/updates/'
            }
        }
    }
}
