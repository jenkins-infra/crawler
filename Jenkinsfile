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

node('maven-17') {
    stage ('Prepare') {
        deleteDir()
        checkout scm
    }

    withEnv([
        "PATH+GROOVY=${tool 'groovy'}/bin",
    ]) {
        stage('Build') {
            sh 'mvn -v'
            sh 'mvn -e clean install'
        }

        stage('Generate') {
            timestamps {
                if (infra.isTrusted()) {
                    withCredentials([[$class: 'ZipFileBinding', credentialsId: 'update-center-signing', variable: 'SECRET']]) {
                        sh 'bash ./.jenkins-scripts/generate.sh'
                    }
                }
                else {
                    sh 'bash ./.jenkins-scripts/generate.sh'
                }
            }
        }
    }

    stage('Archive') {
        dir ('target') {
            archiveArtifacts '**'
        }
        if (infra.isTrusted()) {
            stash includes: 'target/**', name: 'target'
        }
    }
}

if (infra.isTrusted()) {
    node('updatecenter') {
        stage('Publish') {
            unstash 'target'
            withCredentials([[$class: 'ZipFileBinding', credentialsId: 'update-center-publish-env', variable: 'UPDATE_CENTER_FILESHARES_ENV_FILES']]) {
                sh 'bash ./.jenkins-scripts/publish.sh'
            }
        }
    }
}
