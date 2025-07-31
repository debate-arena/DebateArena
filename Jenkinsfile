pipeline {
    agent any
    
    environment {
        BRANCH_NAME = "${env.GIT_BRANCH}".replaceFirst(/^origin\//, '')
        NODE_VERSION = '22'
        FRONTEND_DIR = 'Front-end'
        DIST_DIR = 'Front-end/dist'
    }
    
    tools {
        nodejs "${NODE_VERSION}"
    }
    
    stages {
        stage('Trigger Check') {
            steps {
                echo "✅ Frontend Jenkinsfile이 실행되었습니다!"
                echo "브랜치: ${env.GIT_BRANCH}"
                echo "커밋 메시지: ${env.GIT_COMMIT}"
                echo "빌드 번호: ${env.BUILD_NUMBER}"
                echo "실제 브랜치 이름: ${BRANCH_NAME}"
            }
        }
        
        stage('Environment Setup') {
            steps {
                script {
                    echo "🔧 Node.js 환경 설정 중..."
                    
                    // Jenkins tools로 설치된 Node.js 확인
                    sh '''
                        echo "📋 Node.js 버전:"
                        node --version
                        echo "📋 npm 버전:"
                        npm --version
                        echo "📋 설치 경로:"
                        which node
                        which npm
                    '''
                    echo "✅ Jenkins tools를 통해 Node.js 환경이 설정되었습니다."
                }
            }
        }
        
        stage('Setup Environment Variables') {
            steps {
                script {
                    echo "⚙️ 환경변수 파일 설정 중..."
                    withCredentials([file(credentialsId: 'frontend-env', variable: 'ENV_FILE')]) {
                        dir("${FRONTEND_DIR}") {
                            sh '''
                                echo "📝 .env 파일 복사 중..."
                                cp $ENV_FILE .env
                                echo "✅ .env 파일 설정 완료"
                                
                                # .env 파일 존재 확인 (보안상 내용은 출력하지 않음)
                                if [ -f ".env" ]; then
                                    echo "📋 .env 파일이 성공적으로 생성되었습니다."
                                    echo "📏 파일 크기: $(wc -c < .env) bytes"
                                else
                                    echo "❌ .env 파일 생성 실패"
                                    exit 1
                                fi
                            '''
                        }
                    }
                }
            }
        }
        
        stage('Install Dependencies') {
            steps {
                dir("${FRONTEND_DIR}") {
                    echo "📦 Node.js로 의존성 설치 중..."
                    sh 'npm ci --prefer-offline --no-audit'
                }
            }
        }
        
        stage('Build Application') {
            steps {
                dir("${FRONTEND_DIR}") {
                    echo "🏗️ Node.js로 애플리케이션 빌드 중..."
                    sh 'npm run build'
                    
                    // 빌드 결과 확인
                    sh 'ls -la dist/'
                }
            }
        }
        
        stage('Deploy') {
            steps {
                script {
                    if (BRANCH_NAME == 'master') {
                        echo "🚀 운영 서버에 프론트엔드 배포 진행"
                        deployToProduction()
                    } else if (BRANCH_NAME == 'develop' || BRANCH_NAME.startsWith('fe/')) {
                        echo "🧪 개발 서버에 프론트엔드 배포 진행 (브랜치: ${BRANCH_NAME})"
                        deployToDevelopment()
                    } else {
                        echo "❌ 알 수 없는 브랜치입니다. 배포를 중단합니다."
                        currentBuild.result = 'ABORTED'
                        error("지원하지 않는 브랜치: ${BRANCH_NAME}")
                    }
                }
            }
        }
    }
    
    post {
        always {
            echo "🧹 빌드 후 정리 작업 수행"
            // 빌드 아티팩트 보관
            archiveArtifacts artifacts: "${DIST_DIR}/**", allowEmptyArchive: true
            
            // 워크스페이스 정리 (선택사항)
            // cleanWs()
        }
        
        success {
            echo "✅ 프론트엔드 빌드 및 배포가 성공적으로 완료되었습니다!"
            // 성공 알림 (Slack, Email 등)
            // slackSend(channel: '#deployment', message: "✅ Frontend deployed successfully to ${BRANCH_NAME}")
        }
        
        failure {
            echo "❌ 프론트엔드 빌드 또는 배포가 실패했습니다!"
            // 실패 알림
            // slackSend(channel: '#deployment', message: "❌ Frontend deployment failed on ${BRANCH_NAME}")
        }
    }
}

def deployToProduction() {
    echo "🚀 운영 서버 배포 시작..."
    
    // 방법 1: Nginx 서버에 정적 파일 배포
    sh """
        echo '📦 빌드된 파일들을 운영 서버로 복사 중...'
        scp -r ${DIST_DIR}/* jenkins@prod-server:/var/www/html/
        
        echo '🔄 Nginx 설정 reload'
        ssh jenkins@prod-server 'sudo nginx -s reload'
        
        echo '✅ 운영 서버 배포 완료: http://mydebate.duckdns.org'
    """
    
    // 방법 2: Docker 컨테이너로 배포 (주석 해제 시 사용)
    // dir("${FRONTEND_DIR}") {
    //     sh """
    //         echo '🐳 Docker 이미지 빌드 중...'
    //         docker build -t debate-arena-frontend:${env.BUILD_NUMBER} .
    //         docker tag debate-arena-frontend:${env.BUILD_NUMBER} debate-arena-frontend:latest
    //         
    //         echo '🚀 컨테이너 재시작 중...'
    //         docker stop debate-arena-frontend || true
    //         docker rm debate-arena-frontend || true
    //         docker run -d --name debate-arena-frontend -p 80:80 debate-arena-frontend:latest
    //         
    //         echo '✅ Docker 배포 완료: http://서버IP'
    //     """
    // }
}

def deployToDevelopment() {
    echo "🧪 개발 서버 배포 시작..."
    
    dir("${FRONTEND_DIR}") {
        sh """
            echo '🐳 개발용 Docker 이미지 빌드 중...'
            # 개발용 Dockerfile 생성 (개발용 nginx 설정 사용)
            cp Dockerfile Dockerfile.dev
            sed -i 's/nginx.conf/nginx-dev.conf/g' Dockerfile.dev
            
            docker build -f Dockerfile.dev -t debate-arena-frontend-dev:${env.BUILD_NUMBER} .
            docker tag debate-arena-frontend-dev:${env.BUILD_NUMBER} debate-arena-frontend-dev:latest
            
            echo '🚀 컨테이너 재시작 중...'
            docker stop debate-arena-frontend-dev || true
            docker rm debate-arena-frontend-dev || true
            docker run -d --name debate-arena-frontend-dev -p 80:80 debate-arena-frontend-dev:latest
            
            # 임시 Dockerfile 정리
            rm -f Dockerfile.dev
            
            echo '✅ Docker 배포 완료'
            echo '🌐 접속 URL: http://3.34.95.4'
            echo '🐛 디버그 정보: http://3.34.95.4/debug'
        """
    }
} 