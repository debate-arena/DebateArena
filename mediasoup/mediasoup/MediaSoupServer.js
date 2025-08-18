// 설정 모듈 로드
const { 
    serverConfig, 
    logConfiguration 
} = require('./config/AppConfig');

// Redis 모듈 로드
const redisManager = require('./redis/RedisManager');

// MediaSoup 모듈 로드 (Redis 핸들러 통합)
const MediaSoupManager = require('./mediasoup/MediaSoupManager');

// MediaSoupManager에 RedisManager 주입
const mediaSoupManager = new MediaSoupManager(redisManager);

// 서비스 초기화
async function initializeServices() {
    try {
        // MediaSoupManager가 모든 초기화를 담당
        await mediaSoupManager.initialize();
        
        console.log('[SERVER] 모든 서비스 초기화 완료');
    } catch (error) {
        console.error('[SERVER] 서비스 초기화 실패:', error);
        process.exit(1);
    }
}

// 서버 초기화 및 시작
async function startServer() {
    try {
        console.log('[SERVER] MediaSoup SFU 서버 시작 중...');
        
        // 모든 서비스 초기화
        await initializeServices();

        console.log('[SERVER] MediaSoup SFU 서버가 실행 중입니다');
        console.log('[SERVER] Redis pub/sub 통신이 활성화되었습니다');
        console.log('[SERVER] 채널 수신: mediasoup:request');
        console.log('[SERVER] 채널 발송: mediasoup:response');
        console.log('[SERVER] 순수 Node.js 모드로 실행 중 (HTTP 서버 없음)');
        
        // 설정 정보 출력
        logConfiguration();
        
        // 주기적 시스템 상태 로깅
        setInterval(logSystemStatus, serverConfig.statusLogInterval);
        


    } catch (error) {
        console.error('[SERVER] 서버 시작 실패:', error);
        process.exit(1);
    }
}

// Graceful shutdown
process.on('SIGINT', async () => {
    console.log('\n[SERVER] 서버를 종료하는 중...');
    
    try {
        await redisManager.disconnect();
        mediaSoupManager.closeWorker();
        
        console.log('[SERVER] 서버 종료 완료');
        process.exit(0);
    } catch (error) {
        console.error('[SERVER] 종료 중 오류 발생:', error);
        process.exit(1);
    }
});

process.on('SIGTERM', async () => {
    console.log('\n[SERVER] SIGTERM 신호 수신, 안전하게 종료 중...');
    process.emit('SIGINT');
});

// 예외 처리
process.on('uncaughtException', (error) => {
    console.error('[SERVER] 처리되지 않은 예외:', error);
    process.exit(1);
});

process.on('unhandledRejection', (reason, promise) => {
    console.error('[SERVER] 처리되지 않은 Promise 거부:', promise, '이유:', reason);
    process.exit(1);
});

// 주기적 시스템 상태 로깅
async function logSystemStatus() {
    console.log('\n=== 시스템 상태 ===');
    console.log(`[STATUS] 메모리 사용량: ${Math.round(process.memoryUsage().heapUsed / 1024 / 1024)}MB`);
    console.log(`[STATUS] 실행 시간: ${Math.round(process.uptime())}초`);
    
    // MediaSoup 상세 상태 로깅
    await mediaSoupManager.logDetailedStatus();
}
// 서버 시작
startServer();

