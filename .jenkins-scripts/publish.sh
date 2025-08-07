#!/bin/bash
# .jenkins-scripts/publish.sh: execute publication of crawler generated metadatas to the different Update Center sync targets
# NOTE: NEVER delete any remote files
# TODO: find a way to reuse 'SYNC_UC_TASKS' from https://github.com/jenkins-infra/update-center2/blob/master/site/publish.sh#L9 to avoid repetition and automate delivery.

set -eux -o pipefail

# Prepare Artifacts for publication
mkdir -p updates
cp target/*.json target/*.html updates

# Rsync sync tasks
rsync_publish_tasks=("rsync-archives.jenkins.io")

for rsync_publish_task in "${rsync_publish_tasks[@]}"
do
    envToLoad="${UPDATE_CENTER_FILESHARES_ENV_FILES}/.env-${rsync_publish_task}"

    test -f "${envToLoad}"

    # shellcheck source=/dev/null
    source "${envToLoad}"

    # Required variables that should now be set from the .env file
    : "${RSYNC_HOST?}" "${RSYNC_USER?}" "${RSYNC_GROUP?}" "${RSYNC_REMOTE_DIR?}" "${RSYNC_IDENTITY_NAME?}"

    time rsync --recursive --links --perms --times -D \
        --chown="${RSYNC_USER}":"${RSYNC_GROUP}" \
        --checksum --verbose --compress \
        --rsh="ssh -i ${UPDATE_CENTER_FILESHARES_ENV_FILES}/${RSYNC_IDENTITY_NAME}" `# rsync identity file is stored with .env files` \
        --exclude=.svn `# TODO: still needed?` \
        ./updates/ "${RSYNC_USER}"@"${RSYNC_HOST}":"${RSYNC_REMOTE_DIR}"/updates/
done

# Local Rsync sync tasks
localrsync_publish_tasks=("localrsync-updates.jenkins.io-content" "localrsync-updates.jenkins.io-redirections")

for localrsync_publish_task in "${localrsync_publish_tasks[@]}"
do
    envToLoad="${UPDATE_CENTER_FILESHARES_ENV_FILES}/.env-${localrsync_publish_task}"

    test -f "${envToLoad}"

    # shellcheck source=/dev/null
    source "${envToLoad}"

    # Required variables that should now be set from the .env file
    : "${RSYNC_REMOTE_DIR?}"

    time rsync --recursive --links --times -D \
        --checksum --verbose \
        --exclude=.svn `# TODO: still needed?` \
        ./updates/ "${RSYNC_REMOTE_DIR}"/updates/
done

# Cloudflare R2 (uses AWS S3 protocol) sync tasks
export AWS_DEFAULT_REGION=auto

# sanity check for build observability
aws --version

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
    # Do NOT delete files as part of the 'sync' command (aws s3 sync tends to randomly delete after uploading - https://github.com/jenkins-infra/helpdesk/issues/4403)
    aws s3 sync ./updates/ s3://"${BUCKET_NAME}"/updates/ \
        --no-progress \
        --no-follow-symlinks \
        --endpoint-url "${BUCKET_ENDPOINT_URL}" \
        --checksum-algorithm CRC32
done
