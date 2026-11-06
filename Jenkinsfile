pipeline {

    agent any

    options {
        skipDefaultCheckout(true)
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Compile') {
            steps {
                dir('api-management-service') {
                    sh './mvnw -B -DskipTests compile'
                }
            }
        }

        stage('Tests') {

            steps {
                dir('api-management-service') {
                    sh './mvnw -B test'
                }
            }

            post {
                always {
                    junit testResults: 'api-management-service/target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Package') {

            steps {
                dir('api-management-service') {
                    sh './mvnw -B -DskipTests package'
                }

                archiveArtifacts 'api-management-service/target/*.jar'
            }
        }

        stage('Deploy') {

            when {
                branch 'main'
            }

            steps {

                sh '''
                    set -e

                    DEPLOY_ROOT=/opt/cicd-demo/spring
                    RELEASE_DIR="$DEPLOY_ROOT/releases/$BUILD_NUMBER"

                    rm -rf "$RELEASE_DIR"
                    mkdir -p "$RELEASE_DIR"

                    JAR_FILE=$(find api-management-service/target \
                        -maxdepth 1 \
                        -type f \
                        -name '*.jar' \
                        | head -1)

                    cp "$JAR_FILE" \
                       "$RELEASE_DIR/app.jar"

                    # Secrets live on the server, not in the repo. See README.
                    [ -f "$DEPLOY_ROOT/gatekeeper.env" ] || { echo "Missing $DEPLOY_ROOT/gatekeeper.env"; exit 1; }
                    set -a
                    . "$DEPLOY_ROOT/gatekeeper.env"
                    set +a

                    if [ -f "$DEPLOY_ROOT/app.pid" ]; then

                        OLD_PID=$(cat "$DEPLOY_ROOT/app.pid" || true)

                        if [ -n "$OLD_PID" ] && \
                           kill -0 "$OLD_PID" 2>/dev/null; then

                            kill "$OLD_PID" || true
                            sleep 3
                        fi
                    fi

                    JENKINS_NODE_COOKIE=dontKillMe \
                    APP_VERSION="$BUILD_NUMBER" \
                    nohup java \
                    -jar "$RELEASE_DIR/app.jar" \
                    > "$DEPLOY_ROOT/app.log" 2>&1 &

                    echo $! > "$DEPLOY_ROOT/app.pid"
                '''
            }
        }

        stage('Smoke Test') {

            when {
                branch 'main'
            }

            steps {

                sh '''
                    sleep 5
                    curl -f \
                    http://127.0.0.1:8081/health
                '''
            }
        }
    }

    post {

        success {
            echo 'SPRING PIPELINE SUCCESSFUL'
        }

        failure {
            echo 'SPRING PIPELINE FAILED'
        }
    }
}