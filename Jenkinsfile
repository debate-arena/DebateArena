pipeline {
    agent any
    environment {
        BRANCH_NAME = "${env.GIT_BRANCH}".replaceFirst(/^origin\//, '')
    }
    stages {
        stage('Trigger Check') {
            steps {
                echo "✅ Jenkinsfile이 실행되었습니다!"
                echo "브랜치: ${env.GIT_BRANCH}"
                echo "커밋 메시지: ${env.GIT_COMMIT}"
                echo "빌드 번호: ${env.BUILD_NUMBER}"
                echo "실제 브랜치 이름: ${BRANCH_NAME}"
            }
        }

        stage('Branch별 작업') {
            steps {
                script {
                    if (BRANCH_NAME == 'master') {
                        echo "🚀 운영 서버에 배포 진행"
                        // 운영 서버 배포 스크립트 예시
                        // sh './deploy-prod.sh'
                    } else if (BRANCH_NAME == 'develop') {
                        echo "🧪 개발 서버에 배포 진행"
                        // 개발 서버 배포 스크립트 예시
                        // sh './deploy-dev.sh'
                    } else {
                        echo "❌ 알 수 없는 브랜치입니다. 작업을 중단합니다."
                        currentBuild.result = 'ABORTED'
                        error("지원하지 않는 브랜치: ${BRANCH_NAME}")
                    }
                }
            }
        }
    }
}

