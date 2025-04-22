#!/bin/bash
# .jenkins-scripts/generate.sh: generate Jenkins tool installer metadatas for all Groovy files

set -eux -o pipefail

# Provide signing keys in the directory $SECRET if you want to sign metadatas. Not mandatory.
if test -d "${SECRET:-/notexisting}"
then
  export JENKINS_SIGNER="-key \"$SECRET/update-center.key\" -certificate \"$SECRET/update-center.cert\" -root-certificate \"$SECRET/jenkins-update-center-root-ca.crt\""
fi

for f in *.groovy
do
    echo "= Crawler '$f':"
    groovy -Dgrape.config=./grapeConfig.xml ./lib/runner.groovy "$f" \
      || true # Do not fail immediatly
done
