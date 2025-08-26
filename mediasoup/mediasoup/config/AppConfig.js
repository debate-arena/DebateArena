// config/AppConfig.js - 애플리케이션 환경변수 및 설정 관리
require('dotenv').config({ path: '.env.production' });

// Redis 설정
const redisConfig = {
    password: process.env.REDIS_PASSWORD || '',
    host: process.env.REDIS_HOST || 'redis',
    port: process.env.REDIS_PORT || '6379',
    
    // Redis URL 생성 헬퍼
    getUrl() {
        return `redis://${this.password ? `:${this.password}@` : ''}${this.host}:${this.port}`;
    }
};

// MediaSoup 설정
const mediasoupConfig = {
    worker: {
        logLevel: process.env.MEDIASOUP_LOG_LEVEL || 'debug',
        logTags: ['info', 'ice', 'dtls', 'rtp', 'srtp', 'rtcp'],
        rtcMinPort: parseInt(process.env.MEDIASOUP_PORT_RANGE_MIN) || 40000,
        rtcMaxPort: parseInt(process.env.MEDIASOUP_PORT_RANGE_MAX) || 40100,
    },
    
    webRtcTransport: {
        listenInfos: [
            {
                protocol: 'udp',
                ip: process.env.MEDIASOUP_LISTEN_IP || '0.0.0.0',
                announcedAddress: process.env.MEDIASOUP_ANNOUNCED_IP || '127.0.0.1',
                portRange: {
                    min: parseInt(process.env.MEDIASOUP_PORT_RANGE_MIN) || 40000,
                    max: parseInt(process.env.MEDIASOUP_PORT_RANGE_MAX) || 40100
                },
            },
        ],
        enableUdp: true,
        enableTcp: true,
        preferUdp: true,
    },
    
    // MediaSoup 코덱 설정
    mediaCodecs: [
        {
            kind: 'audio',
            mimeType: 'audio/opus',
            clockRate: 48000,
            channels: 2,
        },
        {
            kind: 'video',
            mimeType: 'video/VP8',
            clockRate: 90000,
            parameters: {
                'x-google-start-bitrate': 1000,
            },
        },
    ]
};



// 서버 설정
const serverConfig = {
    statusLogInterval: 30000, // 30초마다 상태 로깅
    pauseResumeTestDelay: 2000, // resume까지 2초 대기
};

// 설정 정보 출력 함수
function logConfiguration() {
    console.log('[CONFIG] .env.production 환경변수가 로드되었습니다');
    console.log('\n=== 설정 정보 ===');
    console.log(`[CONFIG] 공개 IP: ${mediasoupConfig.webRtcTransport.listenInfos[0].announcedAddress}`);
    console.log(`[CONFIG] 리스닝 IP: ${mediasoupConfig.webRtcTransport.listenInfos[0].ip}`);
    console.log(`[CONFIG] 포트 범위: ${mediasoupConfig.worker.rtcMinPort}-${mediasoupConfig.worker.rtcMaxPort}`);
    console.log(`[CONFIG] 로그 레벨: ${mediasoupConfig.worker.logLevel}`);
    console.log(`[CONFIG] Redis: ${redisConfig.host}:${redisConfig.port}`);
    console.log('================\n');
}

module.exports = {
    redisConfig,
    mediasoupConfig,
    serverConfig,
    logConfiguration
}; 