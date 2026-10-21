pipeline {
    agent any

    environment {
        DB_URL = 'jdbc:postgresql://localhost:55432/loanportal_test'
        DB_USER = 'loanportal'
        DB_PASSWORD = 'loanportal_dev_pw'
        SESSION_SECRET = 'ci-session-secret-not-for-production-use-only'
    }

    stages {
        stage('Build') {
            steps {
                sh './gradlew clean build -x test --no-daemon'
            }
        }

        stage('Test') {
            steps {
                sh './gradlew test --no-daemon'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'build/test-results/test/*.xml'
                }
            }
        }

        stage('Report') {
            steps {
                archiveArtifacts artifacts: 'build/quarkus-app/**', allowEmptyArchive: true
                archiveArtifacts artifacts: 'build/reports/**', allowEmptyArchive: true
            }
        }
    }

    post {
        always {
            cleanWs()
        }
    }
}
