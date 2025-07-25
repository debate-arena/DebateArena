pipeline {
    agent any

    stages {
        stage('Trigger Check') {
            steps {
                echo '✅ Jenkinsfile이 실행되었습니다!'
                echo "현재 브랜치: ${env.GIT_BRANCH}"
                echo "커밋 메시지: ${env.GIT_COMMIT}"
                echo "빌드 번호: ${env.BUILD_NUMBER}"
            }
        }
    }
}