// mediasoup-server.js - Pure Node.js with Redis Pub/Sub
const mediasoup = require('mediasoup');
const redis = require('redis');
const { v4: uuidv4 } = require('uuid');

// .env.production 파일 로드
require('dotenv').config({ path: '.env.production' });
console.log('🔧 Environment variables loaded from .env.production');

// Redis clients
let redisClient;
let redisSubscriber;
let redisPublisher;

// MediaSoup objects
let worker;
const routers = new Map();
const transports = new Map();
const producers = new Map();
const consumers = new Map();

// Message handlers map
const messageHandlers = new Map();

const mediaCodecs = [
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
];

// Redis 클라이언트 초기화
async function initializeRedis() {
    try {
        // Main Redis client
        redisClient = redis.createClient({
            host: process.env.REDIS_HOST || 'localhost',
            port: parseInt(process.env.REDIS_PORT) || 6379,
            password: process.env.REDIS_PASSWORD || undefined,
        });

        // Subscriber client
        redisSubscriber = redis.createClient({
            host: process.env.REDIS_HOST || 'localhost',
            port: parseInt(process.env.REDIS_PORT) || 6379,
            password: process.env.REDIS_PASSWORD || undefined,
        });

        // Publisher client
        redisPublisher = redis.createClient({
            host: process.env.REDIS_HOST || 'localhost',
            port: parseInt(process.env.REDIS_PORT) || 6379,
            password: process.env.REDIS_PASSWORD || undefined,
        });

        await redisClient.connect();
        await redisSubscriber.connect();
        await redisPublisher.connect();

        console.log('✅ Redis clients connected successfully');

        // Subscribe to mediasoup request channel
        // await redisSubscriber.subscribe('mediasoup:request', handleRedisMessage);
        await redisSubscriber.subscribe('mediasoup:router:create', createRouter);
        await redisSubscriber.subscribe('mediasoup:transport:create', createTransport);
        await redisSubscriber.subscribe('mediasoup:transport:connect', connectTransport);
        await redisSubscriber.subscribe('mediasoup:producer:create', createProducer);
        await redisSubscriber.subscribe('mediasoup:consumer:create', createConsumer);
        await redisSubscriber.subscribe('mediasoup:consumer:resume', resumeConsumer);
        
        console.log('✅ Subscribed to mediasoup:request channel');

    } catch (error) {
        console.error('❌ Redis initialization failed:', error);
        process.exit(1);
    }
}

// MediaSoup worker 초기화
async function initializeWorker() {
    worker = await mediasoup.createWorker({
        logLevel: process.env.MEDIASOUP_LOG_LEVEL || 'debug',
        logTags: ['info', 'ice', 'dtls', 'rtp', 'srtp', 'rtcp'],
    });

    worker.on('died', () => {
        console.error('❌ MediaSoup worker died');
        process.exit(1);
    });

    console.log('✅ MediaSoup worker initialized successfully');
    console.log(`📊 Worker PID: ${worker.pid}`);
    console.log(`🔧 Worker settings: logLevel=${worker.logLevel}`);
}

// Redis 메시지 핸들러
async function handleRedisMessage(message) {
    try {
        const request = JSON.parse(message);
        console.log(`📨 Received Redis message: ${request.action} (ID: ${request.id})`);

        const handler = messageHandlers.get(request.action);
        if (!handler) {
            await sendResponse(request.id, false, null, `Unknown action: ${request.action}`);
            return;
        }

        const result = await handler(request.payload);
        await sendResponse(request.id, true, result, null);

    } catch (error) {
        console.error('❌ Error handling Redis message:', error);
        const requestId = message.id || 'unknown';
        await sendResponse(requestId, false, null, error.message);
    }
}

// Redis 응답 전송
async function sendResponse(requestId, success, data, error) {
    const response = {
        id: requestId,
        success,
        data,
        error,
        timestamp: Date.now()
    };

    try {
        await redisPublisher.publish('mediasoup:response', JSON.stringify(response));
        console.log(`📤 Sent response for request ID: ${requestId}, Success: ${success}`);
    } catch (err) {
        console.error('❌ Failed to send Redis response:', err);
    }
}

// 라우터 생성 핸들러
async function createRouter(payload)  {
    payload = JSON.parse(payload);
    const { roomId } = payload;
    console.log("[createRouter] roomId:", roomId);
    console.log("[createRouter] payload:", payload);
    
    let router = routers.get(roomId);
    if (!router) {
        router = await worker.createRouter({ mediaCodecs });
        routers.set(roomId, router);
        
        router.on('close', () => {
            console.log(`[Router] Router closed for room: ${roomId}`);
            routers.delete(roomId);
        });
    } else {
        console.log(`[createRouter] Existing router room: ${roomId}  Router ID: ${router.id}`);
    }

    const response = {
        type : "createdRouter",
        userEmail: payload.userEmail,
        id: router.id,
        rtpCapabilities: router.rtpCapabilities,
        roomId: roomId
    };

    await redisPublisher.publish('mediasoup:router:created', JSON.stringify(response));

}

// Transport 생성 핸들러
async function createTransport(payload) {
    payload = JSON.parse(payload);
    const { roomId, isProducer,producerUserEmail } = payload;
    const router = routers.get(roomId);
    
    if (!router) {
        throw new Error(`Router not found for room: ${roomId}`);
    }
    
    const transport = await router.createWebRtcTransport({
        listenIps: [
            {
                ip: process.env.MEDIASOUP_LISTEN_IP || '0.0.0.0',
                announcedIp: process.env.MEDIASOUP_ANNOUNCED_IP || '127.0.0.1',
            }
        ],
        enableUdp: true,
        enableTcp: true,
        preferUdp: true,
        portRange: {
            min: parseInt(process.env.MEDIASOUP_PORT_RANGE_MIN) || 40000,
            max: parseInt(process.env.MEDIASOUP_PORT_RANGE_MAX) || 40100
        },
        // iceTransportPolicy: 'relay',
        // iceServers: [
        //     {
        //         urls: ['turn:175.117.82.139:3478'],
        //         username: 'testuser',
        //         credential: 'testpass'
        //     }
        // ]
    });
    
    transports.set(transport.id, transport);
    console.log(`[Transport created] - ID: ${transport.id}, Type: ${isProducer ? 'Producer' : 'Consumer'}`);
    
    transport.on('close', () => {
        console.log(`Transport closed - ID: ${transport.id}`);
        cleanupTransport(transport.id, 'transport close');
    });
    
    transport.on('dtlsstatechange', (dtlsState) => {
        console.log(`DTLS state change - Transport ${transport.id}: ${dtlsState}`);
        if (dtlsState === 'failed') {
            console.log(`❌ DTLS failed for transport ${transport.id}`);
            cleanupTransport(transport.id, 'DTLS failed');
        }
    });
    
    transport.on('icestatechange', (iceState) => {
        console.log(`ICE state change - Transport ${transport.id}: ${iceState}`);
        if (iceState === 'failed') {
            console.log(`❌ ICE failed for transport ${transport.id}`);
            cleanupTransport(transport.id, 'ICE failed');
        }
    });
    
    console.log(`[Total active transports] : ${transports.size}`);
    
    const response = {
        type : "createdTransport",
        userEmail: payload.userEmail,
        transportId: transport.id,
        roomId : roomId,
        dtlsParameters: transport.dtlsParameters,
        iceCandidates: transport.iceCandidates,
        iceParameters: transport.iceParameters,
        isProducer,
        producerUserEmail: producerUserEmail
    };


    await redisPublisher.publish('mediasoup:transport:created', JSON.stringify(response));

}

// Transport 연결 핸들러
async function connectTransport(payload) {
    payload = JSON.parse(payload);
    const { transportId, dtlsParameters,userEmail } = payload;
    console.log(`[Connecting transport] - Transport ID: ${transportId}`);
    
    const transport = transports.get(transportId);
    if (!transport) {
        throw new Error(`Transport not found: ${transportId}`);
    }
    
    await transport.connect({ dtlsParameters });
    console.log(`[Connecting transport] ID: ${transportId}`);
    console.log(`[Connecting transport] DTLS state: ${transport.dtlsState}`);
    console.log(`[Connecting transport] ICE state: ${transport.iceState}`); 

    const response = {
        userEmail: userEmail,
        connectedTransportId: transportId
    };

    await redisPublisher.publish('mediasoup:transport:connected', JSON.stringify(response));

}

// Producer 생성 핸들러
async function createProducer(payload) {
    payload = JSON.parse(payload);
    const { transportId, kind, rtpParameters } = payload;
    console.log(`[Creating producer] - Transport: ${transportId}, Kind: ${kind}`);
    
    const transport = transports.get(transportId);
    if (!transport) {
        throw new Error(`Transport not found: ${transportId}`);
    }
    
    const producer = await transport.produce({ kind, rtpParameters });
    producers.set(producer.id, producer);
    
    console.log(`[Producer created] - ID: ${producer.id}, Kind: ${kind}`);
    console.log(`[Producer stats] - Paused: ${producer.paused}, Kind: ${producer.kind}`);
    
    producer.on('close', () => {
        console.log(`Producer closed - ID: ${producer.id}`);
        cleanupProducer(producer.id, 'producer close');
    });
    
    producer.on('pause', () => {
        console.log(`⏸Producer paused - ID: ${producer.id}`);
    });
    
    producer.on('resume', () => {
        console.log(`▶Producer resumed - ID: ${producer.id}`);
    });
    
    console.log(`[Total active producers] : ${producers.size}`);

    const response = {
        userEmail: payload.userEmail,
        roomId: payload.roomId,
        producerId: producer.id,
        kind
    };

    await redisPublisher.publish('mediasoup:producer:created', JSON.stringify(response));
}

// Consumer 생성 핸들러
async function createConsumer(payload) {
    payload = JSON.parse(payload);
    const { producerId, rtpCapabilities, transportId,producerUserEmail } = payload;
    console.log(`[Creating consumer - Router , Producer: ${producerId}`);
    
    // const router = routers.get(routerId);
    const producer = producers.get(producerId);
    
    // if (!router) {
    //     throw new Error(`Router not found: ${routerId}`);
    // }
    
    if (!producer) {
        throw new Error(`Producer not found: ${producerId}`);
    }
    
    // if (!router.canConsume({ producerId, rtpCapabilities })) {
    //     throw new Error(`Cannot consume - Router: ${routerId}, Producer: ${producerId}`);
    // }
    
    const transport = transports.get(transportId);
    if (!transport) {
        throw new Error(`Transport not found: ${transportId}`);
    }
    
    console.log("[Using transport] :", transport.id);
    
    const consumer = await transport.consume({
        producerId,
        rtpCapabilities,
        paused: true,
    });
    
    consumers.set(consumer.id, consumer);
    
    console.log(`[Consumer created] - ID: ${consumer.id}, Kind: ${consumer.kind}`);
    console.log(`[Consumer stats] - Paused: ${consumer.paused}, Producer: ${producerId}`);
    
    consumer.on('close', () => {
        console.log(`Consumer closed - ID: ${consumer.id}`);
        cleanupConsumer(consumer.id, 'consumer close');
    });
    
    consumer.on('pause', () => {
        console.log(`⏸Consumer paused - ID: ${consumer.id}`);
    });
    
    consumer.on('resume', () => {
        console.log(`▶Consumer resumed - ID: ${consumer.id}`);
    });
    
    consumer.on('producerclose', () => {
        console.log(`📤 Producer closed for consumer - ID: ${consumer.id}`);
        cleanupConsumer(consumer.id, 'producer closed');
    });
    
    console.log(`[Total active consumers] : ${consumers.size}`);
    
    const response = {
        userEmail: payload.userEmail,
        consumerId: consumer.id,
        producerId: producerId,
        rtpParameters: consumer.rtpParameters,
        kind: consumer.kind,
        producerUserEmail: producerUserEmail
    };

    await redisPublisher.publish('mediasoup:consumer:created', JSON.stringify(response));
}

// Consumer resume 핸들러
async function resumeConsumer(payload) {
    payload = JSON.parse(payload);
    const { consumerId } = payload;
    const consumer = consumers.get(consumerId);
    
    if (!consumer) {
        throw new Error(`Consumer not found: ${consumerId}`);
    }
    
    console.log("Resuming consumer:", consumerId);
    await consumer.resume();
    console.log(`Consumer stats - Paused: ${consumer.paused}`);
    
    return { success: true };
}

// 1단계: Transport 정리 함수
function cleanupTransport(transportId, reason = 'connection lost') {
    console.log(`🧹 Transport 정리 시작: ${transportId} (이유: ${reason})`);
    
    // 2단계: 해당 transport의 모든 producer 찾아서 삭제
    const relatedProducers = [];
    for (const [producerId, producer] of producers.entries()) {
        if (producer.transport && producer.transport.id === transportId) {
            relatedProducers.push(producerId);
        }
    }
    
    relatedProducers.forEach(producerId => {
        console.log(`  📤 Producer 삭제: ${producerId}`);
        const producer = producers.get(producerId);
        if (producer && !producer.closed) {
            producer.close();
        }
        producers.delete(producerId);
    });
    
    // 3단계: 해당 transport의 모든 consumer 찾아서 삭제
    const relatedConsumers = [];
    for (const [consumerId, consumer] of consumers.entries()) {
        if (consumer.transport && consumer.transport.id === transportId) {
            relatedConsumers.push(consumerId);
        }
    }
    
    relatedConsumers.forEach(consumerId => {
        console.log(`  📥 Consumer 삭제: ${consumerId}`);
        const consumer = consumers.get(consumerId);
        if (consumer && !consumer.closed) {
            consumer.close();
        }
        consumers.delete(consumerId);
    });
    
    // 4단계: Transport 자체 삭제
    const transport = transports.get(transportId);
    if (transport && !transport.closed) {
        transport.close();
    }
    transports.delete(transportId);
    
    console.log(`✅ Transport 정리 완료: ${transportId} (Producer: ${relatedProducers.length}개, Consumer: ${relatedConsumers.length}개 삭제)`);
}

// Producer 정리 함수 (관련 Consumer들도 함께 정리)
function cleanupProducer(producerId, reason = 'producer closed') {
    console.log(`🧹 Producer 정리 시작: ${producerId} (이유: ${reason})`);
    
    // 해당 producer를 구독하는 모든 consumer 찾아서 삭제
    const relatedConsumers = [];
    for (const [consumerId, consumer] of consumers.entries()) {
        if (consumer.producerId === producerId) {
            relatedConsumers.push(consumerId);
        }
    }
    
    relatedConsumers.forEach(consumerId => {
        console.log(`  📥 Consumer 삭제: ${consumerId}`);
        const consumer = consumers.get(consumerId);
        if (consumer && !consumer.closed) {
            consumer.close();
        }
        consumers.delete(consumerId);
    });
    
    // Producer 자체 삭제
    const producer = producers.get(producerId);
    if (producer && !producer.closed) {
        producer.close();
    }
    producers.delete(producerId);
    
    console.log(`✅ Producer 정리 완료: ${producerId} (Consumer: ${relatedConsumers.length}개 삭제)`);
}

// Consumer 정리 함수 (단순 삭제)
function cleanupConsumer(consumerId, reason = 'consumer closed') {
    console.log(`🧹 Consumer 정리: ${consumerId} (이유: ${reason})`);
    
    const consumer = consumers.get(consumerId);
    if (consumer && !consumer.closed) {
        consumer.close();
    }
    consumers.delete(consumerId);
    
    console.log(`✅ Consumer 정리 완료: ${consumerId}`);
}


// 서버 초기화 및 시작
async function startServer() {
    try {
        console.log('🚀 Starting MediaSoup SFU Server...');
        
        // Redis 및 MediaSoup 초기화
        await initializeRedis();
        await initializeWorker();

        console.log('✅ MediaSoup SFU server is running');
        console.log('📡 Redis pub/sub communication enabled');
        console.log('📨 Listening on channel: mediasoup:request');
        console.log('📤 Publishing on channel: mediasoup:response');
        console.log('🔧 Server running in pure Node.js mode (no HTTP server)');
        
        console.log('\n=== 📋 Configuration ===');
        console.log(`📍 Announced IP: ${process.env.MEDIASOUP_ANNOUNCED_IP || '127.0.0.1'}`);
        console.log(`🎯 Listen IP: ${process.env.MEDIASOUP_LISTEN_IP || '0.0.0.0'}`);
        console.log(`🔌 Port Range: ${process.env.MEDIASOUP_PORT_RANGE_MIN || 40000}-${process.env.MEDIASOUP_PORT_RANGE_MAX || 40100}`);
        console.log(`📊 Log Level: ${process.env.MEDIASOUP_LOG_LEVEL || 'debug'}`);
        console.log(`🔗 Redis: ${process.env.REDIS_HOST || 'localhost'}:${process.env.REDIS_PORT || 6379}`);
        console.log('=======================\n');
        
        // 5분마다 시스템 상태 로깅
        // setInterval(logSystemStatus, 5 * 60 * 1000);
        
        // 초기 상태 로깅
        // logSystemStatus();

    } catch (error) {
        console.error('❌ Server startup failed:', error);
        process.exit(1);
    }
}

// Graceful shutdown
process.on('SIGINT', async () => {
    console.log('\n🛑 Shutting down server...');
    
    try {
        if (redisClient) await redisClient.quit();
        if (redisSubscriber) await redisSubscriber.quit();
        if (redisPublisher) await redisPublisher.quit();
        if (worker) worker.close();
        
        console.log('✅ Server shutdown complete');
        process.exit(0);
    } catch (error) {
        console.error('❌ Error during shutdown:', error);
        process.exit(1);
    }
});

process.on('SIGTERM', async () => {
    console.log('\n🛑 Received SIGTERM, shutting down gracefully...');
    process.emit('SIGINT');
});

// 예외 처리
process.on('uncaughtException', (error) => {
    console.error('❌ Uncaught Exception:', error);
    process.exit(1);
});

process.on('unhandledRejection', (reason, promise) => {
    console.error('❌ Unhandled Rejection at:', promise, 'reason:', reason);
    process.exit(1);
});

// 서버 시작
startServer();


// // 상태 조회 핸들러
// messageHandlers.set('getStatus', async (payload) => {
//     const status = {
//         server: {
//             uptime: process.uptime(),
//             memory: process.memoryUsage(),
//             pid: process.pid
//         },
//         mediasoup: {
//             worker: {
//                 pid: worker?.pid,
//                 died: worker?.died || false
//             },
//             stats: {
//                 routers: routers.size,
//                 transports: transports.size,
//                 producers: producers.size,
//                 consumers: consumers.size
//             }
//         },
//         rooms: Array.from(routers.keys())
//     };
    
//     console.log('📊 Server status requested');
//     return status;
// });

// // 통계 조회 핸들러
// messageHandlers.set('getStats', async (payload) => {
//     const { type, id } = payload;
//     console.log(`📈 Stats requested for ${type}: ${id}`);
    
//     let stats = null;
    
//     switch(type) {
//         case 'producer':
//             const producer = producers.get(id);
//             if (producer) {
//                 stats = await producer.getStats();
//             }
//             break;
//         case 'consumer':
//             const consumer = consumers.get(id);
//             if (consumer) {
//                 stats = await consumer.getStats();
//             }
//             break;
//         case 'transport':
//             const transport = transports.get(id);
//             if (transport) {
//                 stats = await transport.getStats();
//             }
//             break;
//         default:
//             throw new Error('Invalid type');
//     }
    
//     if (!stats) {
//         throw new Error(`${type} not found`);
//     }
    
//     return stats;
// });

// 주기적 시스템 상태 로깅
// function logSystemStatus() {
//     console.log('\n=== 📊 System Status ===');
//     console.log(`🏠 Active Rooms: ${routers.size}`);
//     console.log(`🚛 Active Transports: ${transports.size}`);
//     console.log(`📤 Active Producers: ${producers.size}`);
//     console.log(`📥 Active Consumers: ${consumers.size}`);
//     console.log(`💾 Memory Usage: ${Math.round(process.memoryUsage().heapUsed / 1024 / 1024)}MB`);
//     console.log(`⏱️ Uptime: ${Math.round(process.uptime())}s`);
//     console.log('========================\n');
// }

