pipeline {
    agent any

    environment {
        COMPOSE_FILE = "docker-compose.yml"
        COMPOSE_PROJECT_NAME = "rupeex-home"
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


        stage('Load Environment File') {
            steps {
                withCredentials([
                    file(credentialsId: 'rupeex-env', variable: 'ENV_FILE')
                ]) {
                    sh '''
                        cp "$ENV_FILE" .env

                        # Remove Windows CRLF characters
                        tr -d "\\r" < .env > .env.tmp
                        mv .env.tmp .env

                        chmod 600 .env

                        echo ".env loaded and fixed"
                    '''
                }
            }
        }


        stage('Backend Tests') {
            steps {
                dir('backend') {
                    sh '''
                        chmod +x mvnw
                        ./mvnw --batch-mode --no-transfer-progress test
                    '''
                }
            }
            post {
                always {
                    junit(
                        testResults: 'backend/target/surefire-reports/*.xml',
                        allowEmptyResults: true
                    )
                }
            }
        }


        stage('Onboarding Service Tests') {
            steps {
                dir('onboarding-service') {
                    sh '''
                        chmod +x mvnw
                        ./mvnw --batch-mode --no-transfer-progress test
                    '''
                }
            }
            post {
                always {
                    junit(
                        testResults: 'onboarding-service/target/surefire-reports/*.xml',
                        allowEmptyResults: true
                    )
                }
            }
        }


        stage('Check Docker') {
            steps {
                sh '''
                    docker --version
                    docker-compose --version
                    docker ps
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
                    docker-compose -f ${COMPOSE_FILE} up -d db


                    echo "Waiting for MySQL health..."

                    for i in $(seq 1 30)
                    do
                        STATUS=$(docker inspect \
                        --format='{{.State.Health.Status}}' \
                        rupeex-db 2>/dev/null || echo starting)

                        if [ "$STATUS" = "healthy" ]
                        then
                            echo "MySQL is healthy"
                            break
                        fi

                        sleep 2
                    done


                    set -a
                    . ./.env
                    set +a


                    docker exec -i rupeex-db mysql \
                    -uroot \
                    -p"${MYSQL_ROOT_PASSWORD}" \
                    -e "
                    CREATE USER IF NOT EXISTS '${MYSQL_USER}'@'%'
                    IDENTIFIED BY '${SPRING_DATASOURCE_PASSWORD}';

                    ALTER USER '${MYSQL_USER}'@'%'
                    IDENTIFIED BY '${SPRING_DATASOURCE_PASSWORD}';

                    CREATE DATABASE IF NOT EXISTS ${MYSQL_DATABASE};

                    GRANT ALL PRIVILEGES 
                    ON ${MYSQL_DATABASE}.* 
                    TO '${MYSQL_USER}'@'%';

                    FLUSH PRIVILEGES;
                    "


                    docker-compose -f ${COMPOSE_FILE} up -d
                '''
            }
        }


        stage('Wait For Application') {
            steps {
                sh '''
                    echo "Waiting for all services to start..."
                    sleep 10

                    echo "Checking service status..."
                    docker-compose -p ${COMPOSE_PROJECT_NAME} -f ${COMPOSE_FILE} ps

                    echo "Waiting for backend to be ready..."
                    for i in $(seq 1 30); do
                        if docker logs rupeex-app 2>&1 | grep -q "Started RupeeXApplication"; then
                            echo "✓ Backend is ready!"
                            break
                        fi
                        echo "Attempt $i/30: Backend not ready yet..."
                        sleep 2
                    done

                    echo "Waiting for RabbitMQ to be healthy..."
                    for i in $(seq 1 30); do
                        STATUS=$(docker inspect \
                        --format='{{.State.Health.Status}}' \
                        rupeex-rabbitmq 2>/dev/null || echo "not-found")

                        if [ "$STATUS" = "healthy" ]; then
                            echo "✓ RabbitMQ is healthy!"
                            break
                        fi
                        echo "Attempt $i/30: RabbitMQ status = $STATUS"
                        sleep 2
                    done

                    echo "Waiting for Ollama to be healthy..."
                    for i in $(seq 1 30); do
                        STATUS=$(docker inspect \
                        --format='{{.State.Health.Status}}' \
                        rupeex-ollama 2>/dev/null || echo "not-found")

                        if [ "$STATUS" = "healthy" ]; then
                            echo "✓ Ollama is healthy!"
                            break
                        fi
                        echo "Attempt $i/30: Ollama status = $STATUS"
                        sleep 2
                    done

                    echo "Checking if bot-service started successfully..."
                    for i in $(seq 1 20); do
                        if docker logs rupeex-bot-service 2>&1 | grep -q "listening on 4001"; then
                            echo "✓ Bot service is running!"
                            break
                        fi
                        echo "Attempt $i/20: Bot service not ready yet..."
                        sleep 2
                    done

                    echo "Final service status:"
                    docker ps --format "table {{.Names}}\\t{{.Status}}"
                '''
            }
        }


        stage('Verify Deployment') {
            steps {
                sh '''
                    echo "Verifying all containers are running..."
                    docker ps

                    echo "Checking docker-compose service status..."
                    docker-compose -p ${COMPOSE_PROJECT_NAME} -f ${COMPOSE_FILE} ps

                    echo "Backend logs:"
                    docker logs --tail=50 rupeex-app || true

                    echo "Frontend logs:"
                    docker logs --tail=50 rupeex-frontend || true
                '''
            }
        }


        stage('Cleanup Secrets') {
            steps {
                sh '''
                    rm -f .env || true
                    docker image prune -f || true
                '''
            }
        }


        stage('Fix Workspace Ownership') {
            steps {
                sh '''
                    if command -v sudo >/dev/null 2>&1
                    then
                        sudo chown -R jenkins:jenkins ${WORKSPACE} || true
                    else
                        chown -R jenkins:jenkins ${WORKSPACE} || true
                    fi
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
            script {
                try {
                    cleanWs(
                        deleteDirs: true,
                        disableDeferredWipeout: true
                    )
                }
                catch (Exception e) {
                    echo "Workspace cleanup failed: ${e.message}"
                }
            }
        }
    }
}