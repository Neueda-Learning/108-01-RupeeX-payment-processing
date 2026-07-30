pipeline {
    agent any

    environment {
        COMPOSE_FILE = "docker-compose.yml"
    }

    triggers {
        githubPush()
    }

    stages {

        stage('Checkout Source') {
            steps {
                checkout scm
            }
        }

        stage('Check Docker') {
            steps {
                sh '''
                    docker --version
                    docker-compose --version
                '''
            }
        }

        stage('Stop Existing Containers') {
            steps {
                sh '''
                    docker-compose -f $COMPOSE_FILE down || true
                '''
            }
        }

        stage('Build Docker Images') {
            steps {
                sh '''
                    docker-compose -f $COMPOSE_FILE build --no-cache
                '''
            }
        }

        stage('Deploy Application') {
            steps {
                sh '''
                    docker-compose -f $COMPOSE_FILE up -d
                '''
            }
        }

        stage('Verify Deployment') {
            steps {
                sh '''
                    docker ps
                    docker-compose -f $COMPOSE_FILE ps
                '''
            }
        }

        stage('Cleanup') {
            steps {
                sh '''
                    docker image prune -f
                '''
            }
        }
    }

    post {
        success {
            echo "Deployment successful 🚀"
        }

        failure {
            echo "Deployment failed ❌"
        }

        always {
            cleanWs()
        }
    }
}