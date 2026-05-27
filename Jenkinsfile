pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        timestamps()
    }

    environment {
        GIT_REPO = 'https://github.com/keupang/keupang-backend.git'
        GIT_CREDENTIALS = 'github-key'
        DEPLOY_BRANCH = 'prod'
        COMPOSE_FILE = 'compose.deploy.yml'
        ENV_FILE_CREDENTIALS = 'keupang-backend-env'
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: "${DEPLOY_BRANCH}", credentialsId: "${GIT_CREDENTIALS}", url: "${GIT_REPO}"
            }
        }

        stage('Build') {
            steps {
                sh './gradlew clean build'
            }
        }

        stage('Deploy') {
            steps {
                withCredentials([file(credentialsId: "${ENV_FILE_CREDENTIALS}", variable: 'DEPLOY_ENV_FILE')]) {
                    sh '''
                    set -e
                    cp "$DEPLOY_ENV_FILE" .env.deploy
                    docker compose --env-file .env.deploy -f "$COMPOSE_FILE" build
                    docker compose --env-file .env.deploy -f "$COMPOSE_FILE" up -d --remove-orphans
                    rm -f .env.deploy
                    '''
                }
            }
        }
    }

    post {
        always {
            sh 'rm -f .env.deploy'
        }
        success {
            echo "Keupang backend deployed from ${DEPLOY_BRANCH}."
        }
        failure {
            echo 'Keupang backend deployment failed.'
        }
    }
}
