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
                    junit testResults: 'backend/target/surefire-reports/*.xml', allowEmptyResults: true
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
                    junit testResults: 'onboarding-service/target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }


        stage('Create Environment File') {
            steps {
                withCredentials([
                    string(credentialsId: 'MYSQL_ROOT_PASSWORD', variable: 'MYSQL_ROOT_PASSWORD'),
                    string(credentialsId: 'MYSQL_PASSWORD', variable: 'MYSQL_PASSWORD'),
                    string(credentialsId: 'SPRING_DATASOURCE_PASSWORD', variable: 'SPRING_DATASOURCE_PASSWORD')
                ]) {

                    sh '''
cat > .env <<EOF
MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
MYSQL_DATABASE=rupeex_db
MYSQL_USER=rupeex
MYSQL_PASSWORD=${MYSQL_PASSWORD}

SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/rupeex_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&zeroDateTimeBehavior=CONVERT_TO_NULL
SPRING_DATASOURCE_USERNAME=rupeex
SPRING_DATASOURCE_PASSWORD=${SPRING_DATASOURCE_PASSWORD}

SERVER_PORT=8080
EOF

echo ".env created"
                    '''
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
                    # Start the database first and wait for it to be healthy before
                    # bringing up the app services, which fail fast if the DB user
                    # lacks privileges.
                    docker-compose -f ${COMPOSE_FILE} up -d db

                    echo "Waiting for MySQL to become healthy..."
                    for i in $(seq 1 30); do
                        status=$(docker inspect --format='{{.State.Health.Status}}' rupeex-db 2>/dev/null || echo "starting")
                        if [ "$status" = "healthy" ]; then
                            echo "MySQL is healthy"
                            break
                        fi
                        sleep 2
                    done

                    # Self-healing grant fix: `docker-compose down` does not remove
                    # the db_data volume, and MySQL's init scripts (which create the
                    # app user/database/grants) only run once against an empty data
                    # directory. If db_data was ever initialized with different
                    # credentials/db name on this host, the app user can end up
                    # authenticated but missing privileges on rupeex_db ("Access
                    # denied ... to database" / MySQL error 1044). Re-applying the
                    # grant here is idempotent and keeps every deploy self-healing
                    # without touching existing data.
                    set -a
                    . ./.env
                    set +a
                    docker exec -i rupeex-db mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" -e "
                        CREATE USER IF NOT EXISTS '${MYSQL_USER}'@'%' IDENTIFIED BY '${SPRING_DATASOURCE_PASSWORD}';
                        ALTER USER '${MYSQL_USER}'@'%' IDENTIFIED BY '${SPRING_DATASOURCE_PASSWORD}';
                        CREATE DATABASE IF NOT EXISTS \\`${MYSQL_DATABASE}\\`;
                        GRANT ALL PRIVILEGES ON \\`${MYSQL_DATABASE}\\`.* TO '${MYSQL_USER}'@'%';
                        FLUSH PRIVILEGES;
                    "

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

                    echo "Backend logs:"
                    docker logs --tail=50 rupeex-app || true

                    echo "Frontend logs:"
                    docker logs --tail=100 rupeex-frontend || true
                '''
            }
        }


        stage('Cleanup') {
            steps {
                sh '''
                    docker image prune -f || true
                    rm -f .env || true
                '''
            }
        }


        stage('Fix Workspace Ownership') {
            steps {
                sh '''
                    if command -v sudo >/dev/null 2>&1; then
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
                } catch (Exception e) {
                    echo "Workspace cleanup failed: ${e.message}"
                }
            }
        }
    }
}