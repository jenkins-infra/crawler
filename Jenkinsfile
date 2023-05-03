#!/usr/bin/env groovy


List p = [buildDiscarder(logRotator(numToKeepStr: '5'))]

/* When we're running inside our trusted infrastructure, we want to
 * re-generate the tools meta-data every four hours
 */
if (infra.isTrusted()) {
    p.add(pipelineTriggers([cron('H */4 * * *')]))
    p.add(disableConcurrentBuilds())
}

properties(p)

node('linux') {
    stage ('Prepare') {
        deleteDir()
        checkout scm
    }

    withEnv([
            "PATH+GROOVY=${tool 'groovy'}/bin",
            "PATH+MVN=${tool 'mvn'}/bin",
            "JAVA_HOME=${tool 'jdk8'}",
            "PATH+JAVA=${tool 'jdk8'}/bin"
    ]) {
        stage('Build') {
            sh 'mvn -e clean install'
        }

        stage('Generate') {
            String command = '''
                for f in *.groovy
                do
                    groovy -Dgrape.config=./grapeConfig.xml ./lib/runner.groovy $f || true
                done
            '''

            timestamps {
                if (infra.isTrusted()) {
                    withCredentials([[$class: 'ZipFileBinding', credentialsId: 'update-center-signing-2023', variable: 'SECRET']]) {
                        sh """
                            export JENKINS_SIGNER="-key \"$SECRET/update-center.key\" -certificate \"$SECRET/update-center.cert\" -root-certificate \"$SECRET/jenkins-update-center-root-ca.crt\"";
                            ${command}
                        """
                    }
                }
                else {
                    sh command
                }
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
            sh 'mkdir -p updates'
            sh 'cp target/*.json target/*.html updates'
            sshagent(['updates-rsync-key']) {
                sh 'rsync -avz  -e \'ssh -o StrictHostKeyChecking=no\' --exclude=.svn updates/ www-data@updates.jenkins.io:/var/www/updates.jenkins.io/updates/'
            }
        }
    }
}
