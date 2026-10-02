#!/usr/bin/env groovy


List p = [buildDiscarder(logRotator(numToKeepStr: '5'))]

/* When we're running inside our trusted infrastructure, we want to
 * re-generate the tools meta-data every four hours
 */
if (infra.isTrustedCiController()) {
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
            sh 'mvn -V -B -e clean install'
        }

        stage('Generate') {
            timestamps {
                if (infra.isTrustedCiController()) {
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
        if (infra.isTrustedCiController()) {
            stash includes: 'target/**', name: 'target'
            stash includes: '.jenkins-scripts/**', name: 'scripts'
        }
    }
}

if (infra.isTrustedCiController()) {
    node('updatecenter') {
        stage('Publish') {
            unstash 'target'
            unstash 'scripts'
            withCredentials([[$class: 'ZipFileBinding', credentialsId: 'update-center-publish-env', variable: 'UPDATE_CENTER_FILESHARES_ENV_FILES']]) {
                sh 'bash ./.jenkins-scripts/publish.sh'
            }
        }
        stage ('Publish build report') {
            publishBuildStatusReport()
        }
    }
}
