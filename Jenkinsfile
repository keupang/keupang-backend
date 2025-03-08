pipeline {
    agent any

    environment {
        GIT_REPO = 'https://github.com/keupang/keupang-backend.git'
        GIT_CREDENTIALS = 'github-key' // Credential ID
        DOCKER_HUB_CREDENTIALS = 'docker-key' // Docker Hub Credentials ID
        SLACK_CHANNEL = '#keupang-back' // Slack 채널 이름
        SLACK_CREDENTIAL_ID = 'slack-key' // Jenkins에 저장한 Slack Webhook Credential ID
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out source code...'
                git branch: 'prod', credentialsId: "${GIT_CREDENTIALS}", url: "${GIT_REPO}"
            }
        }

        stage('Build Projects') {
            steps {
                echo 'Building all projects...'
                sh '''
                # 각 프로젝트를 Gradle로 빌드
                ./gradlew clean build
                '''
            }
        }

        stage('Docker Build and Push') {
            steps {
                echo 'Building Docker images and pushing to Docker Hub...'
                withCredentials([usernamePassword(credentialsId: "${DOCKER_HUB_CREDENTIALS}", usernameVariable: "DOCKER_USER", passwordVariable: "DOCKER_PASS")]) {
                    sh '''
                    # Docker Hub 로그인
                    echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin

                    # 각 프로젝트의 Dockerfile을 사용하여 이미지 빌드 및 Push
                    docker buildx build --platform linux/amd64,linux/arm64 -t playdodo/keupang-config-server:1.0 ./keupang-config-server --push
                    docker buildx build --platform linux/amd64,linux/arm64 -t playdodo/keupang-eureka-server:1.0 ./keupang-eureka-server --push
                    docker buildx build --platform linux/amd64,linux/arm64 -t playdodo/keupang-api-gateway:1.0 ./keupang-gateway --push
                    docker buildx build --platform linux/amd64,linux/arm64 -t playdodo/keupang-service-product:1.0 ./keupang-product --push
                    docker buildx build --platform linux/amd64,linux/arm64 -t playdodo/keupang-service-user:1.0 ./keupang-user --push
                    docker buildx build --platform linux/amd64,linux/arm64 -t playdodo/keupang-service-auth:1.0 ./keupang-auth --push
                    docker buildx build --platform linux/amd64,linux/arm64 -t playdodo/keupang-service-stock:1.0 ./keupang-stock --push
                    '''
                }
            }
        }

        stage('Deploy to Server') {
            steps {
                echo 'Deploying on the server...'
                sshagent(['server-ssh-key']) { // 서버 접속을 위한 SSH 키의 Credential ID
                    sh '''
                    # 서버에 SSH로 접속해서 이미지 Pull 및 Compose 실행
                    ssh -o StrictHostKeyChecking=no root@api.keupang.store << EOF
                    cd /home
                    docker compose pull
                    docker compose up -d
                    exit
                    EOF
                    '''
                }
            }
        }
    }

    post {
        success {
            echo '✅ CI/CD Pipeline completed successfully!'
            slackSend(channel: "${SLACK_CHANNEL}", color: "good", message: "✅ CI/CD Pipeline completed successfully! Build #${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)")
        }
        failure {
            echo '❌ CI/CD Pipeline failed!'
            slackSend(channel: "${SLACK_CHANNEL}", color: "danger", message: "❌ CI/CD Pipeline failed! Build #${env.BUILD_NUMBER} (<${env.BUILD_URL}|Open>)")
        }
        always {
            echo '📋 CI/CD Pipeline finished.'
        }
    }
}
