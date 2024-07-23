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
    }

    if (infra.isTrusted()) {
        stage('Publish') {
            sh '''
                mkdir -p updates
                cp target/*.json target/*.html updates
            '''
            withCredentials([[$class: 'ZipFileBinding', credentialsId: 'update-center-publish-env', variable: 'UPDATE_CENTER_FILESHARES_ENV_FILES']]) {
                withEnv([
                    'AWS_DEFAULT_REGION=auto',
                    // TODO: find a way to reuse 'SYNC_UC_TASKS' from https://github.com/jenkins-infra/update-center2/blob/master/site/publish.sh#L9 to avoid repetition and automate delivery.
                ]) {
                    // Rsync copy tasks
                    sh '''
                    . "${UPDATE_CENTER_FILESHARES_ENV_FILES}/.env-rsync-updates.jenkins.io"

                    # Required variables that should now be set from the .env file
                    : "${RSYNC_HOST?}" "${RSYNC_USER?}" "${RSYNC_GROUP?}" "${RSYNC_REMOTE_DIR?}" "${RSYNC_IDENTITY_NAME?}"

                    rsync -rlptDvz -e "ssh -o StrictHostKeyChecking=no -i ${UPDATE_CENTER_FILESHARES_ENV_FILES}/${RSYNC_IDENTITY_NAME}" --exclude=.svn --chown="${RSYNC_USER}":"${RSYNC_GROUP}" ./updates/ "${RSYNC_USER}"@"${RSYNC_HOST}":"${RSYNC_REMOTE_DIR}"/updates/
                    '''

                    // Azure copy tasks
                    sh '''
                    test -f "${UPDATE_CENTER_FILESHARES_ENV_FILES}/.env-azsync-content"

                    # Don't print any command to avoid exposing credentials
                    set +x

                    . "${UPDATE_CENTER_FILESHARES_ENV_FILES}/.env-azsync-content"

                    # Required variables that should now be set from the .env file
                    : "${STORAGE_NAME?}" "${STORAGE_FILESHARE?}" "${STORAGE_DURATION_IN_MINUTE?}" "${STORAGE_PERMISSIONS?}" "${JENKINS_INFRA_FILESHARE_CLIENT_ID?}" "${JENKINS_INFRA_FILESHARE_CLIENT_SECRET?}" "${JENKINS_INFRA_FILESHARE_TENANT_ID?}"

                    # It's now safe
                    set -x

                    ## 'get-fileshare-signed-url.sh' command is a script stored in /usr/local/bin used to generate a signed file share URL with a short-lived SAS token
                    ## Source: https://github.com/jenkins-infra/pipeline-library/blob/master/resources/get-fileshare-signed-url.sh
                    fileShareUrl="$(get-fileshare-signed-url.sh)"
                    # We want to append the 'updates/' path on the URI of the generated URL to allow deletion of this subdirectory only
                    # But the URL has a query string so we need a text transformation
                    fileShareForCrawler="$(echo $fileShareUrl | sed 's#/?#/updates/?#')"

                    # Fail fast if no share URL can be generated
                    : "${fileShareForCrawler?}"

                    azcopy sync \
                        --skip-version-check `# Do not check for new azcopy versions (we have updatecli for this)` \
                        --exclude-path '.svn' \
                        --recursive=true \
                        --delete-destination=true `# important: use relative path for destination otherwise you will delete update_center2 data from the bucket root` \
                        ./updates/ "${fileShareForCrawler}"
                    '''

                    // AWS copy tasks
                    sh '''
                    for bucket in "s3sync-westeurope" "s3sync-eastamerica"
                    do
                        test -f "${UPDATE_CENTER_FILESHARES_ENV_FILES}/.env-${bucket}"

                        # Don't print any command to avoid exposing credentials
                        set +x

                        # Pipeline usually uses '/bin/sh' so no 'source' shell keyword available
                        . "${UPDATE_CENTER_FILESHARES_ENV_FILES}/.env-${bucket}"

                        # Required variables that should now be set from the .env file
                        : "${BUCKET_NAME?}" "${BUCKET_ENDPOINT_URL?}" "${AWS_ACCESS_KEY_ID?}" "${AWS_SECRET_ACCESS_KEY?}" "${AWS_DEFAULT_REGION?}"

                        # It's now safe
                        set -x

                        ## Note: AWS CLI are configured through environment variables (from Jenkins credentials) - https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-envvars.html
                        aws s3 sync ./updates/ s3://"${BUCKET_NAME}"/updates/ \
                            --delete `# important: use relative path for destination otherwise you will delete update_center2 data from the bucket root` \
                            --no-progress \
                            --no-follow-symlinks \
                            --size-only \
                            --exclude '.svn' \
                            --endpoint-url "${BUCKET_ENDPOINT_URL}"
                    done
                    '''
                }
            }
        }
    }
}
