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
const roomTransportSizeMap = new Map();
const producers = new Map();
const consumers = new Map();
const producerTransportMap = new Map();
const consumerTransportMap = new Map();
const producerHasConsumer = new Map();
const transportProducerMap = new Map();

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
        const redisPassword = process.env.REDIS_PASSWORD || '';
        const redisHost = process.env.REDIS_HOST || 'redis';
        const redisPort = process.env.REDIS_PORT || '6379';
        redisClient = redis.createClient({
            url: `redis://${redisPassword ? `:${redisPassword}@` : ''}${redisHost}:${redisPort}`,
        });

        // Subscriber client
        redisSubscriber = redis.createClient({
            url: `redis://${redisPassword ? `:${redisPassword}@` : ''}${redisHost}:${redisPort}`,
        });

        // Publisher client
        redisPublisher = redis.createClient({
            url: `redis://${redisPassword ? `:${redisPassword}@` : ''}${redisHost}:${redisPort}`,
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
        await redisSubscriber.subscribe('mediasoup:transport:disconnect', disconnectTransport);
        await redisSubscriber.subscribe('mediasoup:producer:mic:on', micOn);
        await redisSubscriber.subscribe('mediasoup:producer:mic:off', micOff);
        
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
        rtcMinPort: process.env.MEDIASOUP_PORT_RANGE_MIN,
        rtcMaxPort: process.env.MEDIASOUP_PORT_RANGE_MAX,
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
        roomTransportSizeMap.set(roomId, (roomTransportSizeMap.get(roomId) || 0));
        
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
    const { roomId, isProducer,producerUserEmail,sessionId } = payload;
    console.log("[createTransport] roomId:", roomId);
    console.log("[createTransport] payload:", payload);
    const router = routers.get(roomId);
    
    if (!router) {
        throw new Error(`Router not found for room: ${roomId}`);
    }
    
    const transport = await router.createWebRtcTransport({
        // listenIps: [
        //     {
        //         ip: process.env.MEDIASOUP_LISTEN_IP || '0.0.0.0',
        //         announcedIp: process.env.MEDIASOUP_ANNOUNCED_IP || '127.0.0.1',
        //     }
        // ],
        listenInfos: [
            {
                protocol: 'udp',                // 보통 WebRTC는 UDP
                ip: process.env.MEDIASOUP_LISTEN_IP,                  // 로컬에서 바인딩할 인터페이스
                announcedAddress:  process.env.MEDIASOUP_ANNOUNCED_IP,   // 클라이언트에 노출할 공인 IP (도메인 아님)
                portRange: {
                    min: parseInt(process.env.MEDIASOUP_PORT_RANGE_MIN) ,
                    max: parseInt(process.env.MEDIASOUP_PORT_RANGE_MAX) 
                },            
            },
        ],
        enableUdp: true,
        enableTcp: true,
        preferUdp: true,

    });
    
    transports.set(transport.id, transport);
    roomTransportSizeMap.set(roomId, roomTransportSizeMap.get(roomId) + 1);

    console.log(`[Transport created] - ID: ${transport.id}, Type: ${isProducer ? 'Producer' : 'Consumer'}
         RoomTransport:${roomTransportSizeMap.get(roomId)}`);
    
    transport.on('close', () => {
        console.log(`Transport closed - ID: ${transport.id}`);
    });
    
    transport.on('dtlsstatechange', async(dtlsState) => {
        console.log(`DTLS state change - Transport ${payload.userEmail}, ${dtlsState}`);
        if (dtlsState === 'failed') {
            console.log(`DTLS failed for transport ${transport.id}`);
            cleanupTransport(transport.id, null, roomId, 'DTLS failed');
        }
        if(dtlsState === 'connected'){
            const connectedResponse = {
                roomId : roomId,
                isProducer: isProducer,
                consumerUserEmail: payload.userEmail,
                producerUserEmail: producerUserEmail
            };
            await redisPublisher.publish('mediasoup:transport:established', JSON.stringify(connectedResponse));
            console.log(`DTLS connected for transport ${payload.userEmail}, ${payload.producerUserEmail}`);
            console.log(`${JSON.stringify(connectedResponse)   }`);
        }
        if(dtlsState === 'closed'){
            console.log(`DTLS closed for transport ${payload.userEmail}, ${payload.producerUserEmail}`);
        }
    });
    
    transport.on('icestatechange', async (iceState) => {
        console.log(`ICE state change - Transport ${transport.id}: ${iceState}`);
        if (iceState === 'failed') {
            console.log(`❌ ICE failed for transport ${transport.id}`);
            cleanupTransport(transport.id, null, roomId, 'ICE failed');
        }
        if (iceState === 'disconnected') {
            const disconnectedResponse = {
                roomId : roomId,
                userEmail: payload.userEmail,
                transportId: transport.id,
                sessionId: sessionId
            };
            await redisPublisher.publish('mediasoup:transport:disconnected', JSON.stringify(disconnectedResponse));
            console.log(`ICE disconnected for transport ${payload.userEmail}, ${payload.producerUserEmail}`);
        }
    });

    transport.on('sctpstatechange', (connectionState) => {
        console.log(`Connection state change - Transport ${transport.id}: ${connectionState}`);
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
        producerUserEmail: producerUserEmail,
        sessionId: sessionId
    };


    await redisPublisher.publish('mediasoup:transport:created', JSON.stringify(response));

}

async function disconnectTransport(payload) {
    payload = JSON.parse(payload);
    const { producerId, roomId , type,consumerId,transportId} = payload;
    console.log(`[disconnectTransport] - producerId: ${producerId}, roomId: ${roomId}, type: ${type}, consumerId: ${consumerId}, transportId: ${transportId}`);
    if(type === 'producer'){
        cleanupProducer(producerId, roomId);
    }else if(type === 'consumer'){
        cleanupConsumer(consumerId, roomId);
    }else{
        transportId.forEach(transportIdvalue => {
            cleanupTransport(transportIdvalue, roomId);
        });
    }

    if(roomTransportSizeMap.get(roomId) === 0){
        routers.get(roomId).close();
        routers.delete(roomId);
        roomTransportSizeMap.delete(roomId);
        console.log(`routers 삭제 : ${roomId}`);
    }

    
    console.log(`[Total active transports] : ${transports.size}`);
    console.log(`[Total active producers] : ${producers.size}`);
    console.log(`[Total active consumers] : ${consumers.size}`);
    console.log(`[Total active routers] : ${routers.size}`);
    console.log(`[Total active roomTransportSizeMap] : ${roomTransportSizeMap.get(roomId)}`);
    console.log(`[Total active producerHasConsumer] : ${producerHasConsumer.size}`);
}

// Transport 연결 핸들러
async function connectTransport(payload) {
    payload = JSON.parse(payload);
    const { transportId, dtlsParameters,userEmail,roomId } = payload;
    console.log(`[Connecting transport] - Transport ID: ${userEmail}`);
    
    const transport = transports.get(transportId);
    if (!transport) {
        throw new Error(`Transport not found: ${transportId}`);
    }
    try{
        await transport.connect({ dtlsParameters });
    }catch(error){
        console.error('Error connecting transport:', error);
    }

    const response = {
        userEmail: userEmail,
        connectedTransportId: transportId,
        roomId: roomId
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
        const response = {
            userEmail: payload.userEmail,
            error: "transport not found"
        };
        await redisPublisher.publish('mediasoup:producer:created', JSON.stringify(response));
        console.log(`[createProducer] Transport not found: ${transportId}: ${transportId}`);
        return;

    }
    // TODO : 추후 삭제
    paused = false;
    const producer = await transport.produce({ 
        kind, 
        rtpParameters,
        paused,
     });
    producers.set(producer.id, producer);
    producerTransportMap.set(producer.id, transportId);
    addProducerToTransport(transportId, producer.id);
    
    console.log(`[Producer created] - ID: ${producer.id}, Kind: ${kind}`);
    console.log(`[Producer created] - Paused: ${producer.paused}, Kind: ${producer.kind}`);
    
    producer.on('transportclose', () => {
        console.log(`Producer transportclose closed - ID: ${producer.id}`);
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
    const { producerId, rtpCapabilities, transportId,producerUserEmail,roomId } = payload;
    console.log(`[Creating consumer ${producerId}`);
    
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

        const response = {
            userEmail: payload.userEmail,
            error: "transport not found"
        };
        await redisPublisher.publish('mediasoup:consumer:created', JSON.stringify(response));
        console.log(`[createConsumer] Transport not found: ${transportId}`);
        return;

    }
    
    console.log("[Create Consumer] used :", transport.id);
    
    // TODO : 추후 삭제
    paused = false;
    const consumer = await transport.consume({
        producerId,
        rtpCapabilities,
        paused,
    });
    
    consumers.set(consumer.id, consumer);
    consumerTransportMap.set(consumer.id, transportId);
    addConsumerToProducer(producerId, consumer.id);

    console.log(`[Consumer created] - ID: ${consumer.id}, Kind: ${consumer.kind}`);
    console.log(`[Consumer stats] - Paused: ${consumer.paused}, Producer: ${producerId}`);
    
    consumer.on('transportclose', () => {
        console.log(`Consumer closed - ID: ${consumer.id}`);
    });
    
    consumer.on('producerclose', () => {
        console.log(`⏸Consumer paused - ID: ${consumer.id}`);
    });
    
    consumer.on('producerresume', () => {
        console.log(`▶Consumer resumed - ID: ${consumer.id}`);
    });
    
    console.log(`[Total active consumers] : ${consumers.size}`);
    
    const response = {
        userEmail: payload.userEmail,
        consumerId: consumer.id,
        producerId: producerId,
        rtpParameters: consumer.rtpParameters,
        kind: consumer.kind,
        producerUserEmail: producerUserEmail,
        roomId: roomId
    };

    await redisPublisher.publish('mediasoup:consumer:created', JSON.stringify(response));
}

async function micOn(payload) {
    payload = JSON.parse(payload);
    const { producerId } = payload;

    const producer = producers.get(producerId);

    if (!producer) {
        console.log(`[ERROR]Producer not found: ${producerId}`);
        return;
    }

    console.log(`[micOn] Producer ${producerId} is on`);
    producer.resume();

    producerHasConsumer.get(producerId).forEach(consumerId => {
        const consumer = consumers.get(consumerId);
        if (consumer) {
            consumer.resume();
        }
    });
}

async function micOff(payload) {
    payload = JSON.parse(payload);
    const { producerId } = payload;

    const producer = producers.get(producerId);

    if (!producer) {
        console.log(`[ERROR]Producer not found: ${producerId}`);
        return;
    }

    console.log(`[micOff] Producer ${producerId} is on`);
    producer.pause();

    producerHasConsumer.get(producerId).forEach(consumerId => {
        const consumer = consumers.get(consumerId);
        if (consumer) {
            consumer.pause();
        }
    });
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
function cleanupTransport(transportId,roomId,isEvent = false) {
    console.log(`Transport 정리 시작: ${transportId} `);
    // Transport 자체 삭제
    const transport = transports.get(transportId);
    if(!transport){
        console.log(`[cleanupTransport!] transport not found : ${transportId}`);
        return;
    }
    if (transport && !transport.closed) {
        transport.close();
        roomTransportSizeMap.set(roomId, roomTransportSizeMap.get(roomId) - 1);
    }

    transports.delete(transportId);
}

function cleanupProducer(producerId, roomId) {
    const transportId = producerTransportMap.get(producerId);

    if(producerId && producerId !== null){
        // 1. 해당 producer를 구독하는 consumer 의 transport 찾아서 삭제
        if(producerHasConsumer.has(producerId)){
            producerHasConsumer.get(producerId).forEach(consumerId => {
                const consumerTransportId = consumerTransportMap.get(consumerId);
                const consumerTransport = transports.get(consumerTransportId);
                if(consumerTransport && !consumerTransport.closed){
                    consumerTransport.close();
                    roomTransportSizeMap.set(roomId, roomTransportSizeMap.get(roomId) - 1);
                    console.log(`consumerTransport 삭제 : ${consumerTransportId}`);
                }
                if(consumerTransport){
                    transports.delete(consumerTransport.id);
                }
                if(consumerId){
                    consumers.delete(consumerId);
                    consumerTransportMap.delete(consumerId);
                }
            });
        }
        const producerTransportId = producerTransportMap.get(producerId);
        const producerTransport = transports.get(producerTransportId);

        // 2. 해당 producer를 사용하는 transport 삭제
        if(producerTransport && !producerTransport.closed){
            producerTransport.close();
            roomTransportSizeMap.set(roomId, roomTransportSizeMap.get(roomId) - 1);
            console.log(`producerTransport 삭제 : ${producerTransportId}`);
        }

        // 3. 해당 transport의 모든 producer 찾아서 삭제
        transportProducerMap.get(transportId).forEach(producerId => {
            producers.delete(producerId);
            producerTransportMap.delete(producerId);
        });
        transports.delete(producerTransport.id);
        transportProducerMap.delete(transportId);

        // 4. 해당 producer를 구독하는 consumer 삭제
        producerHasConsumer.delete(producerId);
        
    }

    console.log(`[cleanup producer] - Transport ID: ${transportId}, Producer ID: ${producerId}, Room ID: ${roomId}`);
}

function addConsumerToProducer(producerId, consumerId) {
    if (!producerHasConsumer.has(producerId)) {
        producerHasConsumer.set(producerId, new Set());
    }
    producerHasConsumer.get(producerId).add(consumerId);
}

function addProducerToTransport(transportId, producerId) {
    if (!transportProducerMap.has(transportId)) {
        transportProducerMap.set(transportId, new Set());
    }
    transportProducerMap.get(transportId).add(producerId);
}

// Producer 정리 함수 (관련 Consumer들도 함께 정리)
// function cleanupProducer(producerId, reason = 'producer closed') {
//     console.log(`🧹 Producer 정리 시작: ${producerId} (이유: ${reason})`);
    
//     // 해당 producer를 구독하는 모든 consumer 찾아서 삭제
//     const relatedConsumers = [];
//     for (const [consumerId, consumer] of consumers.entries()) {
//         if (consumer.producerId === producerId) {
//             relatedConsumers.push(consumerId);
//         }
//     }
    
//     relatedConsumers.forEach(consumerId => {
//         console.log(`  📥 Consumer 삭제: ${consumerId}`);
//         const consumer = consumers.get(consumerId);
//         if (consumer && !consumer.closed) {
//             consumer.close();
//         }
//         consumers.delete(consumerId);
//     });
    
//     // Producer 자체 삭제
//     const producer = producers.get(producerId);
//     if (producer && !producer.closed) {
//         producer.close();
//     }
//     producers.delete(producerId);
    
//     console.log(`✅ Producer 정리 완료: ${producerId} (Consumer: ${relatedConsumers.length}개 삭제)`);
// }

// Consumer 정리 함수 (단순 삭제)
function cleanupConsumer(consumerId, roomId) {
    const transportId = consumerTransportMap.get(consumerId);
    const transport = transports.get(transportId);
    if(transport && !transport.closed){
        transport.close();
        console.log(`consumerTransport 삭제 : ${transportId}`);
        roomTransportSizeMap.set(roomId, roomTransportSizeMap.get(roomId) - 1);
    }
    producerHasConsumer.get(consumers.get(consumerId).producerId).delete(consumerId);
    transports.delete(transportId);
    consumers.delete(consumerId);
    consumerTransportMap.delete(consumerId);


}


// Producer pause/resume 테스트 시작
function startProducerPauseResumeTest() {
    let pauseResumeCounter = 0;
    
    console.log('🎮 Starting producer pause/resume test (every 10 seconds)');
    
    setInterval(async () => {
        try {
            pauseResumeCounter++;
            
            if (producers.size === 0) {
                console.log(`⏱️ [${pauseResumeCounter}] No producers to pause/resume`);
                return;
            }

            console.log(`\n🔄 [${pauseResumeCounter}] Starting pause/resume cycle for ${producers.size} producers`);
            
            // 1단계: 모든 producer 일시정지
            console.log(`⏸️ Pausing all producers...`);
            const pausePromises = [];
            
            for (const [producerId, producer] of producers.entries()) {
                if (!producer.paused) {
                    pausePromises.push(
                        producer.pause()
                            .then(() => console.log(`  ⏸️ Paused producer ${producerId} (${producer.kind})`))
                            .catch(err => console.error(`  ❌ Failed to pause producer ${producerId}:`, err))
                    );
                } else {
                    console.log(`  ⏸️ Producer ${producerId} (${producer.kind}) already paused`);
                }
            }
            
            await Promise.all(pausePromises);
            console.log(`✅ All producers paused`);
            
            // 2초 대기 후 resume
            setTimeout(async () => {
                try {
                    console.log(`▶️ Resuming all producers...`);
                    const resumePromises = [];
                    
                    for (const [producerId, producer] of producers.entries()) {
                        if (producer.paused) {
                            resumePromises.push(
                                producer.resume()
                                    .then(() => console.log(`  ▶️ Resumed producer ${producerId} (${producer.kind})`))
                                    .catch(err => console.error(`  ❌ Failed to resume producer ${producerId}:`, err))
                            );
                        } else {
                            console.log(`  ▶️ Producer ${producerId} (${producer.kind}) already active`);
                        }
                    }
                    
                    await Promise.all(resumePromises);
                    console.log(`✅ All producers resumed`);
                    console.log(`🔄 [${pauseResumeCounter}] Pause/resume cycle completed\n`);
                    
                } catch (error) {
                    console.error(`❌ Error during resume phase:`, error);
                }
            }, 2000); // 2초 후 resume
            
        } catch (error) {
            console.error(`❌ Error during pause/resume cycle:`, error);
        }
    }, 10000); // 10초마다 실행
}

// 서버 초기화 및 시작
async function startServer() {
    try {
        console.log('🚀 Starting MediaSoup SFU Server...');
        console.log(`${process.env.REDIS_HOST || '127.0.0.1'}`);
        
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
        setInterval(logSystemStatus, 5000);
        
        // 초기 상태 로깅
        // logSystemStatus();

        // 10초마다 모든 producer pause/resume 테스트
        // startProducerPauseResumeTest();

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
function logSystemStatus() {
    console.log('\n=== 📊 System Status ===');
    console.log(`💾 Memory Usage: ${Math.round(process.memoryUsage().heapUsed / 1024 / 1024)}MB`);
    console.log(`⏱️ Uptime: ${Math.round(process.uptime())}s`);
    console.log('[Total active roomTransportSizeMap]:');
    roomTransportSizeMap.forEach((value, key) => {
      console.log(`  [${key}]: ${value}`);
    });



    console.log(`[Total active transports] : ${transports.size}`);
    console.log(`[Total active producers] : ${producers.size}`);
    console.log(`[Total active consumers] : ${consumers.size}`);
    console.log(`[Total active routers] : ${routers.size}`);
    console.log(`[Total active producerHasConsumer] : ${producerHasConsumer.size}`);
    producerHasConsumer.forEach((value, key) => {
        console.log(`  [${key}]: ${value.size}`);
    });
    console.log(`[Total active transportProducerMap] : ${transportProducerMap.size}`);
    console.log(`[Total active consumerTransportMap] : ${consumerTransportMap.size}`);
    console.log(`[Total active producerTransportMap] : ${producerTransportMap.size}`);
    console.log('========================\n');
}

