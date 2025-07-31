pipeline {
    agent any
    
    environment {
        BRANCH_NAME = "${env.GIT_BRANCH}".replaceFirst(/^origin\//, '')
        NODE_VERSION = '22'
        FRONTEND_DIR = 'Front-end'
        DIST_DIR = 'Front-end/dist'
        
        // 배포 방식 선택 (true: nginx 직접 배포, false: Docker 배포)
        USE_DIRECT_NGINX = 'false'
        // 원격 배포 여부 (true: 원격 서버, false: 로컬 서버)
        USE_REMOTE_DEPLOYMENT = 'false'
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
                    
                    // Node.js 환경 확인
                    sh '''
                        echo "📋 Node.js 버전 확인:"
                        node --version
                        
                        echo "📋 npm 버전 확인:"
                        npm --version
                        
                        echo "📋 현재 작업 디렉토리:"
                        pwd
                        ls -la
                    '''
                    echo "✅ Node.js 환경이 설정되었습니다."
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
                    echo "📦 의존성 설치 중..."
                    sh '''
                        echo "📋 package.json 확인:"
                        ls -la package*.json
                        
                        echo "📦 npm 패키지 설치:"
                        npm ci --prefer-offline --no-audit
                        
                        echo "✅ 의존성 설치 완료"
                    '''
                }
            }
        }
        
        stage('Build Application') {
            steps {
                dir("${FRONTEND_DIR}") {
                    echo "🏗️ 애플리케이션 빌드 중..."
                    sh '''
                        echo "🏗️ npm run build 실행:"
                        npm run build
                        
                        echo "📋 빌드 결과 확인:"
                        ls -la dist/
                        
                        echo "✅ 빌드 완료"
                    '''
                }
            }
        }

        stage('권한 체크') {
            steps {
                sh '''
                    echo 🔍 현재 사용자:
                    whoami
                    echo 🔍 그룹 정보:
                    id
                    echo 🔍 docker.sock 권한:
                    ls -l /var/run/docker.sock
                '''
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
    
    def useDirectNginx = env.USE_DIRECT_NGINX ?: 'false'
    def useRemoteDeployment = env.USE_REMOTE_DEPLOYMENT ?: 'true'
    
    if (useDirectNginx == 'true' && useRemoteDeployment == 'false') {
        echo "🌐 로컬 Nginx 직접 배포 진행..."
        deployToNginxDirect()
        echo '✅ 운영 서버 배포 완료: https://debate-arena.duckdns.org'
    } else if (useRemoteDeployment == 'true') {
        echo "🌐 원격 서버 배포 진행..."
        // 방법 1: Nginx 서버에 정적 파일 배포
        sh """
            echo '📦 빌드된 파일들을 운영 서버로 복사 중...'
            scp -r ${DIST_DIR}/* jenkins@prod-server:/var/www/html/
            
            echo '🔄 Nginx 설정 reload'
            ssh jenkins@prod-server 'sudo nginx -s reload'
            
            echo '✅ 운영 서버 배포 완료: https://debate-arena.duckdns.org'
        """
    } else {
        echo "🐳 Docker 컨테이너 배포 진행..."
        // 방법 2: Docker 컨테이너로 배포
        dir("${FRONTEND_DIR}") {
            sh """
                echo '🐳 운영용 Docker 이미지 빌드 중...'
                
                # SSL 인증서 존재 여부 확인하여 nginx 설정 선택
                if [ -d "/etc/letsencrypt/live/debate-arena.duckdns.org" ]; then
                    echo '🔐 SSL 인증서 발견 - HTTPS 설정 사용'
                    docker build -t debate-arena-frontend:${env.BUILD_NUMBER} .
                    CONTAINER_PORTS="-p 80:80 -p 443:443"
                    SSL_VOLUME="-v /etc/letsencrypt:/etc/letsencrypt:ro"
                    ACCESS_URL="https://debate-arena.duckdns.org"
                else
                    echo '⚠️ SSL 인증서 없음 - HTTP 설정으로 임시 빌드'
                    cp Dockerfile Dockerfile.temp
                    sed -i 's/nginx.conf/nginx-temp.conf/g' Dockerfile.temp
                    docker build -f Dockerfile.temp -t debate-arena-frontend:${env.BUILD_NUMBER} .
                    rm -f Dockerfile.temp
                    CONTAINER_PORTS="-p 80:80"
                    SSL_VOLUME=""
                    ACCESS_URL="http://debate-arena.duckdns.org"
                fi
                
                docker tag debate-arena-frontend:${env.BUILD_NUMBER} debate-arena-frontend:latest
                
                echo '🚀 컨테이너 재시작 중...'
                docker stop debate-arena-frontend || true
                docker rm debate-arena-frontend || true
                docker run -d --name debate-arena-frontend \
                    \$CONTAINER_PORTS \$SSL_VOLUME \
                    debate-arena-frontend:latest
                
                echo "✅ Docker 배포 완료: \$ACCESS_URL"
            """
        }
    }
}

def deployToDevelopment() {
    echo "🧪 개발 서버 배포 시작..."
    
    // Docker가 사용 가능한지 확인
    def dockerAvailable = sh(script: 'command -v docker >/dev/null 2>&1', returnStatus: true) == 0
    def useDirectNginx = env.USE_DIRECT_NGINX ?: 'false'
    
    if (useDirectNginx == 'true') {
        echo "🌐 Nginx 직접 배포 진행..."
        deployToNginxDirect()
    } else if (dockerAvailable) {
        echo "🐳 Docker를 사용한 배포 진행..."
        dir("${FRONTEND_DIR}") {
            sh """
                echo '🐳 개발용 Docker 이미지 빌드 중...'
                
                # SSL 인증서 존재 여부 확인하여 nginx 설정 선택
                if [ -d "/etc/letsencrypt/live/debate-arena.duckdns.org" ]; then
                    echo '🔐 SSL 인증서 발견 - HTTPS 설정 사용'
                    cp Dockerfile Dockerfile.dev
                    sed -i 's/nginx.conf/nginx-dev.conf/g' Dockerfile.dev
                else
                    echo '⚠️ SSL 인증서 없음 - HTTP 설정 사용'
                    cp Dockerfile Dockerfile.dev
                    sed -i 's/nginx.conf/nginx-temp.conf/g' Dockerfile.dev
                fi
                
                docker build -f Dockerfile.dev -t debate-arena-frontend-dev:${env.BUILD_NUMBER} .
                docker tag debate-arena-frontend-dev:${env.BUILD_NUMBER} debate-arena-frontend-dev:latest
                
                echo '🚀 컨테이너 재시작 중...'
                docker stop debate-arena-frontend-dev || true
                docker rm -f debate-arena-frontend-dev || true
                
                # SSL 인증서 존재 여부에 따라 포트 및 볼륨 설정
                if [ -d "/etc/letsencrypt/live/debate-arena.duckdns.org" ]; then
                    echo '🔐 HTTPS 포트로 컨테이너 실행'
                    docker run -d --name debate-arena-frontend-dev \
                        -p 80:80 -p 443:443 \
                        -v /etc/letsencrypt:/etc/letsencrypt:ro \
                        debate-arena-frontend-dev:latest
                else
                    echo '🌐 HTTP 포트로 컨테이너 실행'
                    docker run -d --name debate-arena-frontend-dev \
                        -p 80:80 \
                        debate-arena-frontend-dev:latest
                fi
                
                # 임시 Dockerfile 정리
                rm -f Dockerfile.dev
                
                echo '✅ Docker 배포 완료'
            """
        }
    } else {
        echo "⚠️ Docker를 사용할 수 없습니다. Nginx 직접 배포로 진행..."
        deployToNginxDirect()
    }
    
    if (useDirectNginx == 'true') {
        echo '🌐 접속 URL: https://debate-arena.duckdns.org'
        echo '🐛 디버그 정보: https://debate-arena.duckdns.org/debug'
    } else {
        // SSL 인증서 존재 여부에 따라 URL 표시
        def sslExists = sh(script: '[ -d "/etc/letsencrypt/live/debate-arena.duckdns.org" ] && echo "true" || echo "false"', returnStdout: true).trim()
        if (sslExists == 'true') {
            echo '🌐 접속 URL: https://debate-arena.duckdns.org'
            echo '🐛 디버그 정보: https://debate-arena.duckdns.org/debug'
        } else {
            echo '🌐 접속 URL: http://debate-arena.duckdns.org'
            echo '🐛 디버그 정보: http://debate-arena.duckdns.org/debug'
            echo '⚠️ SSL 인증서를 설정하려면 다음 명령어를 실행하세요:'
            echo '   sudo bash setup-ssl.sh'
        }
    }
}

def deployToNginxDirect() {
    echo "🌐 Nginx 직접 배포 시작..."
    
    dir("${FRONTEND_DIR}") {
        sh '''
            echo "📋 빌드 결과 확인:"
            if [ ! -d "dist" ]; then
                echo "❌ dist 디렉토리가 생성되지 않았습니다!"
                exit 1
            fi
            
            if [ ! -f "dist/index.html" ]; then
                echo "❌ index.html이 빌드되지 않았습니다!"
                exit 1
            fi
            
            echo "📁 빌드된 파일 목록:"
            ls -la dist/
            
            echo "🚀 Nginx로 배포 중..."
            # 백업 생성 (오류 발생 시 무시)
            sudo mkdir -p /usr/share/nginx/html.backup || true
            sudo cp -r /usr/share/nginx/html/* /usr/share/nginx/html.backup/ 2>/dev/null || true
            
            # 기존 파일 제거 및 새 파일 복사
            sudo rm -rf /usr/share/nginx/html/*
            sudo cp -r dist/* /usr/share/nginx/html/
            
            # 권한 설정
            sudo chown -R www-data:www-data /usr/share/nginx/html/ || sudo chown -R nginx:nginx /usr/share/nginx/html/ || true
            sudo chmod -R 644 /usr/share/nginx/html/*
            sudo find /usr/share/nginx/html/ -type d -exec chmod 755 {} \\;
            
            echo "🔄 Nginx 설정 테스트 및 재시작"
            sudo nginx -t
            sudo systemctl reload nginx || sudo service nginx reload
            
            echo "📋 배포 완료 후 파일 확인:"
            ls -la /usr/share/nginx/html/
            
            echo "✅ Nginx 직접 배포 완료"
            
            echo "🔐 SSL 인증서 확인 및 설정..."
            # SSL 인증서가 있는지 확인
            if [ ! -d "/etc/letsencrypt/live/debate-arena.duckdns.org" ]; then
                echo "⚠️ SSL 인증서가 없습니다. 수동으로 setup-ssl.sh를 실행하세요."
                echo "   sudo bash setup-ssl.sh"
            else
                echo "✅ SSL 인증서가 이미 설정되어 있습니다."
                # 인증서 갱신 확인
                sudo certbot renew --dry-run --quiet && echo "✅ SSL 인증서 갱신 테스트 성공" || echo "⚠️ SSL 인증서 갱신 테스트 실패"
            fi
        '''
    }
} 