#!/bin/bash -ex
# Script used on Jenkins to build & publish data
export JENKINS_SIGNER="-key \"$SECRET/update-center.key\" -certificate \"$SECRET/update-center.cert\" -root-certificate \"$SECRET/jenkins-update-center-root-ca.crt\""

# need to pick up newer groovy
export PATH=/usr/local/groovy/bin:$PATH

mvn -e clean install

for f in *.groovy
do
  ./$f || true
done

mkdir updates || true
cp target/*.json target/*.html updates
cd updates

# pushing the tool update data to updates.jenkins-ci.org
cd ..
rsync -avz --exclude=.svn updates/ www-data@updates.jenkins-ci.org:/var/www/updates.jenkins-ci.org/updates/
rsync -avz --exclude=.svn updates/ www-data@updates.jenkins-ci.org:/var/www/updates2.jenkins-ci.org/updates/

