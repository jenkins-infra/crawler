# Infra Backend Crawlers

These scripts generate machine readable tool installer metadata.
You need Groovy 2.0 or later to run them.
You can add new one by doing monkey-see monkey-do. Any one of these scripts
are individually runnable, for example:

```shell
groovy -Dgrape.config=./grapeConfig.xml ./lib/runner.groovy mongodb.groovy
```

## Signing Files

If you need to "only" sign the JSON files (after an update center certificate renewal for instance):

```shell
mkdir -p ./target/
rsync -av "${source}/*.json" ./target/
export JENKINS_SIGNER="-key path/to/uc-cert.key -certificate path/to/uc-cert.cert -root-certificate path/to/uc-root-ca.crt"
groovy -Dgrape.config=./grapeConfig.xml ./lib/runner.groovy ./lib/signer.groovy
# You can find the signed files in ./target/*.html
```
