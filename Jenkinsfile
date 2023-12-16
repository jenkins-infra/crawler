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
            "JAVA_HOME=${tool 'jdk17'}",
            "PATH+JAVA=${tool 'jdk17'}/bin"
    ]) {
        stage('Build') {
            sh 'mvn -e clean install'
        }

        stage('Generate') {
            String command = '''
                for f in *.groovy
                do
                    echo "= Crawler '$f':"
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
            sh '''
                mkdir -p updates
                cp target/*.json target/*.html updates
            '''
            sshagent(['updates-rsync-key']) {
                sh 'rsync -avz  -e \'ssh -o StrictHostKeyChecking=no\' --exclude=.svn updates/ www-data@updates.jenkins.io:/var/www/updates.jenkins.io/updates/'
            }
            withCredentials([
                string(credentialsId: 'updates-jenkins-io-file-share-sas-token-query-string', variable: 'UPDATES_FILE_SHARE_QUERY_STRING'),
                string(credentialsId: 'aws-access-key-id-updatesjenkinsio', variable: 'AWS_ACCESS_KEY_ID'),
                string(credentialsId: 'aws-secret-access-key-updatesjenkinsio', variable: 'AWS_SECRET_ACCESS_KEY')
            ]) {
                withEnv([
                    'AWS_DEFAULT_REGION=auto',
                    'UPDATES_R2_BUCKETS=westeurope-updates-jenkins-io',
                    'UPDATES_R2_ENDPOINT=https://8d1838a43923148c5cee18ccc356a594.r2.cloudflarestorage.com',
                ]) {
                    sh '''
                    azcopy sync ./updates/ "https://updatesjenkinsio.file.core.windows.net/updates-jenkins-io/updates/?${UPDATES_FILE_SHARE_QUERY_STRING}" --exclude-path '.svn' --recursive=true

                    ## Note: AWS CLI are configured through environment variables (from Jenkins credentials) - https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-envvars.html
                    aws s3 sync ./updates/ s3://"${UPDATES_R2_BUCKETS}"/updates/ \
                        --no-progress \
                        --no-follow-symlinks \
                        --size-only \
                        --exclude '.svn' \
                        --endpoint-url "${UPDATES_R2_ENDPOINT}"
                    '''
                }
            }
        }
    }
}
