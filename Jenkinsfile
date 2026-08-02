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


        stage('Create Environment File') {
            steps {
                withCredentials([
                    string(credentialsId: 'MYSQL_ROOT_PASSWORD', variable: 'MYSQL_ROOT_PASSWORD'),
                    string(credentialsId: 'MYSQL_PASSWORD', variable: 'MYSQL_PASSWORD'),
                    string(credentialsId: 'SPRING_DATASOURCE_PASSWORD', variable: 'SPRING_DATASOURCE_PASSWORD')
                ]) {
                    sh """
                    cat > .env <<EOF
MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
MYSQL_DATABASE=rupeex
MYSQL_USER=rupeex
MYSQL_PASSWORD=${MYSQL_PASSWORD}

SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/rupeex?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=rupeex
SPRING_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD}

SERVER_PORT=8080
EOF

                    echo ".env file created"
                    """
                }
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
                    docker-compose -f ${COMPOSE_FILE} down || true
                '''
            }
        }


        stage('Build Docker Images') {
            steps {
                sh '''
                    docker-compose -f ${COMPOSE_FILE} build --no-cache
                '''
            }
        }


        stage('Deploy Application') {
            steps {
                sh '''
                    docker-compose -f ${COMPOSE_FILE} up -d
                '''
            }
        }


        stage('Wait For Application') {
            steps {
                sh '''
                    sleep 20
                '''
            }
        }


       stage('Verify Deployment') {
            steps {
                sh '''
                    docker ps
                    docker-compose -f ${COMPOSE_FILE} ps

                    echo "Checking application logs..."

                    docker logs --tail=50 rupeex-app || true

                    echo "Frontend logs:"

                    docker logs --tail=100 rupeex-frontend || true
                '''
            }
        }


        stage('Cleanup') {
            steps {
                sh '''
                    docker image prune -f
                    rm -f .env
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

            sh '''
                echo "Backend logs:"
                docker logs --tail=100 rupeex-app || true

                echo "Frontend logs:"
                docker logs --tail=100 rupeex-frontend || true
            '''
        }


        always {
            cleanWs()
        }
    }
}