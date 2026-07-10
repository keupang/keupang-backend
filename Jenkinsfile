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
        DEPLOY_DIR = '/home/cj8556/keupang-backend'
        COMPOSE_FILE = 'compose.deploy.yml'
    }

    stages {
        stage('Sync Deploy Directory') {
            steps {
                dir("${DEPLOY_DIR}") {
                    git branch: "${DEPLOY_BRANCH}", credentialsId: "${GIT_CREDENTIALS}", url: "${GIT_REPO}"
                }
            }
        }

        stage('Build') {
            steps {
                dir("${DEPLOY_DIR}") {
                    sh './gradlew clean build'
                }
            }
        }

        stage('Deploy') {
            steps {
                dir("${DEPLOY_DIR}") {
                    sh '''
                    set -e
                    test -f .env.deploy
                    docker compose --env-file .env.deploy -f "$COMPOSE_FILE" build
                    docker compose --env-file .env.deploy -f "$COMPOSE_FILE" up -d --remove-orphans
                    docker compose --env-file .env.deploy -f "$COMPOSE_FILE" ps
                    '''
                }
            }
        }
    }

    post {
        success {
            echo "Keupang backend deployed from ${DEPLOY_BRANCH} at ${DEPLOY_DIR}."
        }
        failure {
            echo 'Keupang backend deployment failed.'
        }
    }
}
