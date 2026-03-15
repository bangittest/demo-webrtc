pipeline {
    agent any

    environment {
        IMAGE_NAME = "demo-webrtc"
        CONTAINER_NAME = "demo-webrtc-container"
    }

    stages {

        stage('Checkout Code') {
            steps {
                git branch: 'main', url: 'https://github.com/bangittest/demo-webrtc.git'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker build -t $IMAGE_NAME .'
            }
        }

        stage('Stop Old Container') {
            steps {
                sh '''
                docker rm -f $CONTAINER_NAME || true
                '''
            }
        }

        stage('Run with Docker Compose') {
            steps {
                sh '''
                docker-compose down || true
                docker-compose up -d --build
                '''
            }
        }

    }

    post {
        success {
            echo "Deploy success"
        }
        failure {
            echo "Deploy failed"
        }
    }
}
