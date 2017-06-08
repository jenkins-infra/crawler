pipeline {
    agent {
        label 'linux'
    }
    tools {
        groovy 'groovy'
    }
    stages {
        stage("Build") {
            steps {
                sh '''
                    for f in *.groovy
                    do
                      groovy -Dgrape.config=./grapeConfig.xml ./lib/runner.groovy $f || true
                    done
                '''
            }
        }
    }
}
