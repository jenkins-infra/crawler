#!/bin/bash
# .jenkins-scripts/publish.sh: execute publication of crawler generated metadatas to the different Update Center sync targets
# TODO: find a way to reuse 'SYNC_UC_TASKS' from https://github.com/jenkins-infra/update-center2/blob/master/site/publish.sh#L9 to avoid repetition and automate delivery.

set -eux -o pipefail

# Prepare Artifacts for publication
mkdir -p updates
cp target/*.json target/*.html updates

# Rsync sync tasks
# shellcheck source=/dev/null
source "${UPDATE_CENTER_FILESHARES_ENV_FILES}/.env-rsync-updates.jenkins.io"

# Required variables that should now be set from the .env file
: "${RSYNC_HOST?}" "${RSYNC_USER?}" "${RSYNC_GROUP?}" "${RSYNC_REMOTE_DIR?}" "${RSYNC_IDENTITY_NAME?}"

rsync -rlptDvz -e "ssh -o StrictHostKeyChecking=no -i ${UPDATE_CENTER_FILESHARES_ENV_FILES}/${RSYNC_IDENTITY_NAME}" --exclude=.svn --chown="${RSYNC_USER}":"${RSYNC_GROUP}" ./updates/ "${RSYNC_USER}"@"${RSYNC_HOST}":"${RSYNC_REMOTE_DIR}"/updates/

## Azure Buckets sync tasks
sync_azsync_tasks=("azsync-content" "azsync-redirections-unsecured" "azsync-redirections-secured")

for az_bucket in "${sync_azsync_tasks[@]}"
do
    # Don't print any command to avoid exposing credentials
    set +x

    envToLoad="${UPDATE_CENTER_FILESHARES_ENV_FILES}/.env-${az_bucket}"
    test -f "${envToLoad}"

    # shellcheck source=/dev/null
    source "${envToLoad}"
    # Required variables that should now be set from the .env file
    : "${STORAGE_NAME?}" "${STORAGE_FILESHARE?}" "${STORAGE_DURATION_IN_MINUTE?}" "${STORAGE_PERMISSIONS?}" "${JENKINS_INFRA_FILESHARE_CLIENT_ID?}" "${JENKINS_INFRA_FILESHARE_CLIENT_SECRET?}" "${JENKINS_INFRA_FILESHARE_TENANT_ID?}" "${FILESHARE_SYNC_DEST_URI?}"

    ## 'get-fileshare-signed-url.sh' command is a script stored in /usr/local/bin used to generate a signed file share URL with a short-lived SAS token
    ## Source: https://github.com/jenkins-infra/pipeline-library/blob/master/resources/get-fileshare-signed-url.sh
    fileShareBaseUrl="$(get-fileshare-signed-url.sh)"

    # Append the '$FILESHARE_SYNC_DEST_URI' (which must ends with a slash) AND '/updates/' paths on the URI of the generated URL
    # But the URL has a query string so we need a text transformation
    # shellcheck disable=SC2001 # The shell internal search and replace would be tedious due to escapings, hence keeping sed
    fileShareForCrawler="$(echo "${fileShareBaseUrl}" | sed "s#/?#${FILESHARE_SYNC_DEST_URI}updates/?#")"

    # Fail fast if no share URL can be generated
    : "${fileShareForCrawler?}"

    # It's now safe
    set -x

    azcopy sync \
        --skip-version-check `# Do not check for new azcopy versions (we have updatecli for this)` \
        --exclude-path '.svn' \
        --recursive=true \
        --delete-destination=true `# important: use relative path for destination otherwise you will delete update_center2 data from the bucket root` \
        ./updates/ "${fileShareForCrawler}"
done

# Cloudflare R2 (uses AWS S3 protocol) sync tasks
export AWS_DEFAULT_REGION=auto
# Cloudflare R2 does not support more than 2 concurent requests - https://community.cloudflare.com/t/is-it-actually-possible-to-upload-to-r2-buckets-using-wrangler/388762/7
aws configure set default.s3.max_concurrent_requests 2
aws configure set default.s3.multipart_threshold "50MB"

sync_s3_tasks=("s3sync-westeurope" "s3sync-eastamerica")

for bucket in "${sync_s3_tasks[@]}"
do
    test -f "${UPDATE_CENTER_FILESHARES_ENV_FILES}/.env-${bucket}"

    # Don't print any command to avoid exposing credentials
    set +x

    # Pipeline usually uses '/bin/sh' so no 'source' shell keyword available
    # shellcheck source=/dev/null
    source "${UPDATE_CENTER_FILESHARES_ENV_FILES}/.env-${bucket}"

    # Required variables that should now be set from the .env file
    : "${BUCKET_NAME?}" "${BUCKET_ENDPOINT_URL?}" "${AWS_ACCESS_KEY_ID?}" "${AWS_SECRET_ACCESS_KEY?}" "${AWS_DEFAULT_REGION?}"

    # It's now safe
    set -x

    ## Note: AWS CLI are configured through environment variables (from Jenkins credentials) - https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-envvars.html
    # First run is to only copy files (overriding them if needed) - https://github.com/aws/aws-cli/issues/1417#issuecomment-121555682
    aws s3 sync ./updates/ s3://"${BUCKET_NAME}"/updates/ \
        --no-progress \
        --no-follow-symlinks \
        --endpoint-url "${BUCKET_ENDPOINT_URL}"
    # Second run is only to delete files absent from the source - https://github.com/aws/aws-cli/issues/1417#issuecomment-121555682
    aws s3 sync ./updates/ s3://"${BUCKET_NAME}"/updates/ \
        --delete `# important: use relative path for destination otherwise you will delete update_center2 data from the bucket root` \
        --no-progress \
        --no-follow-symlinks \
        --endpoint-url "${BUCKET_ENDPOINT_URL}"
done
