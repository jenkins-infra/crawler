#!/bin/bash -ex
# Script used on Jenkins to build & publish data
export JENKINS_SIGNER="-key \"$SECRET/update-center.key\" -certificate \"$SECRET/update-center.cert\" -root-certificate \"$SECRET/jenkins-update-center-root-ca.crt\""

# need to pick up newer groovy
export PATH=/usr/local/groovy/bin:$PATH

mvn -e clean install

for f in *.groovy
do
  groovy -Dgrape.config=./grapeConfig.xml ./lib/runner.groovy $f || true
done

mkdir updates || true
cp target/*.json target/*.html updates
cd updates

# pushing the tool update data to updates.jenkins-ci.org
cd ..
rsync -avz  -e 'ssh -o UserKnownHostsFile=/dev/null,StrictHostKeyChecking=no' --exclude=.svn updates/ www-data@updates.jenkins.io:/var/www/updates.jenkins.io/updates/

