// redis/RedisManager.js - Redis 클라이언트 관리 및 Pub/Sub 통신
const redis = require('redis');
const { redisConfig } = require('../config/AppConfig');

// Redis 채널 설정 (static 상수)
const REDIS_CHANNELS = {
    // Subscribe channels
    subscribe: {
        routerCreate: 'mediasoup:router:create',
        transportCreate: 'mediasoup:transport:create',
        transportConnect: 'mediasoup:transport:connect',
        producerCreate: 'mediasoup:producer:create',
        consumerCreate: 'mediasoup:consumer:create',
        consumerResume: 'mediasoup:consumer:resume',
        transportDisconnect: 'mediasoup:transport:disconnect',
        producerMicOn: 'mediasoup:producer:mic:on',
        producerMicOff: 'mediasoup:producer:mic:off',
    },
    
    // Publish channels
    publish: {
        response: 'mediasoup:response',
        routerCreated: 'mediasoup:router:created',
        transportCreated: 'mediasoup:transport:created',
        transportConnected: 'mediasoup:transport:connected',
        transportEstablished: 'mediasoup:transport:established',
        transportDisconnected: 'mediasoup:transport:disconnected',
        producerCreated: 'mediasoup:producer:created',
        consumerCreated: 'mediasoup:consumer:created',
    }
};

class RedisManager {
    constructor() {
        this.client = null;
        this.subscriber = null;
        this.publisher = null;
    }

    // Redis 클라이언트들 초기화
    async initialize() {
        try {
            console.log(`[REDIS] Redis 연결 시도 중: ${redisConfig.host}:${redisConfig.port}`);

            // Main Redis client
            this.client = redis.createClient({
                url: redisConfig.getUrl(),
            });

            // Subscriber client
            this.subscriber = redis.createClient({
                url: redisConfig.getUrl(),
            });

            // Publisher client
            this.publisher = redis.createClient({
                url: redisConfig.getUrl(),
            });

            await this.client.connect();
            await this.subscriber.connect();
            await this.publisher.connect();

            console.log('[REDIS] Redis 클라이언트 연결 완료');
            return true;
        } catch (error) {
            console.error('[REDIS] Redis 초기화 실패:', error);
            throw error;
        }
    }

    // 메시지 핸들러들을 등록하고 구독 시작
    async subscribeToChannels(handlers) {
        try {
            await this.subscriber.subscribe(REDIS_CHANNELS.subscribe.routerCreate, handlers.createRouter);
            await this.subscriber.subscribe(REDIS_CHANNELS.subscribe.transportCreate, handlers.createTransport);
            await this.subscriber.subscribe(REDIS_CHANNELS.subscribe.transportConnect, handlers.connectTransport);
            await this.subscriber.subscribe(REDIS_CHANNELS.subscribe.producerCreate, handlers.createProducer);
            await this.subscriber.subscribe(REDIS_CHANNELS.subscribe.consumerCreate, handlers.createConsumer);
            await this.subscriber.subscribe(REDIS_CHANNELS.subscribe.consumerResume, handlers.resumeConsumer);
            await this.subscriber.subscribe(REDIS_CHANNELS.subscribe.transportDisconnect, handlers.disconnectTransport);
            await this.subscriber.subscribe(REDIS_CHANNELS.subscribe.producerMicOn, handlers.micOn);
            await this.subscriber.subscribe(REDIS_CHANNELS.subscribe.producerMicOff, handlers.micOff);
            
            console.log('[REDIS] 모든 MediaSoup 채널 구독 완료');
        } catch (error) {
            console.error('[REDIS] 채널 구독 오류:', error);
            throw error;
        }
    }

    // 응답 메시지 발송
    async publishResponse(requestId, success, data, error) {
        const response = {
            id: requestId,
            success,
            data,
            error,
            timestamp: Date.now()
        };

        try {
            await this.publisher.publish(REDIS_CHANNELS.publish.response, JSON.stringify(response));
            console.log(`[REDIS] 응답 전송 완료 - 요청 ID: ${requestId}, 성공: ${success}`);
        } catch (err) {
            console.error('[REDIS] 응답 전송 실패:', err);
        }
    }

    // Router 생성 완료 알림
    async publishRouterCreated(response) {
        try {
            await this.publisher.publish(REDIS_CHANNELS.publish.routerCreated, JSON.stringify(response));
        } catch (err) {
            console.error('[REDIS] Router 생성 알림 발송 실패:', err);
        }
    }

    // Transport 생성 완료 알림
    async publishTransportCreated(response) {
        try {
            await this.publisher.publish(REDIS_CHANNELS.publish.transportCreated, JSON.stringify(response));
        } catch (err) {
            console.error('[REDIS] Transport 생성 알림 발송 실패:', err);
        }
    }

    // Transport 연결 완료 알림
    async publishTransportConnected(response) {
        try {
            await this.publisher.publish(REDIS_CHANNELS.publish.transportConnected, JSON.stringify(response));
        } catch (err) {
            console.error('[REDIS] Transport 연결 알림 발송 실패:', err);
        }
    }

    // Transport 연결 확립 알림
    async publishTransportEstablished(response) {
        try {
            await this.publisher.publish(REDIS_CHANNELS.publish.transportEstablished, JSON.stringify(response));
        } catch (err) {
            console.error('[REDIS] Transport 연결 확립 알림 발송 실패:', err);
        }
    }

    // Transport 연결 끊김 알림
    async publishTransportDisconnected(response) {
        try {
            await this.publisher.publish(REDIS_CHANNELS.publish.transportDisconnected, JSON.stringify(response));
        } catch (err) {
            console.error('[REDIS] Transport 연결 끊김 알림 발송 실패:', err);
        }
    }

    // Producer 생성 완료 알림
    async publishProducerCreated(response) {
        try {
            await this.publisher.publish(REDIS_CHANNELS.publish.producerCreated, JSON.stringify(response));
        } catch (err) {
            console.error('[REDIS] Producer 생성 알림 발송 실패:', err);
        }
    }

    // Consumer 생성 완료 알림
    async publishConsumerCreated(response) {
        try {
            await this.publisher.publish(REDIS_CHANNELS.publish.consumerCreated, JSON.stringify(response));
        } catch (err) {
            console.error('[REDIS] Consumer 생성 알림 발송 실패:', err);
        }
    }

    // Redis 메시지 핸들러 (기존 handleRedisMessage와 동일한 로직)
    async handleMessage(message) {
        try {
            const request = JSON.parse(message);
            console.log(`[REDIS] Redis 메시지 수신: ${request.action} (ID: ${request.id})`);

            const handler = this.messageHandlers.get(request.action);
            if (!handler) {
                await this.publishResponse(request.id, false, null, `Unknown action: ${request.action}`);
                return;
            }

            const result = await handler(request.payload);
            await this.publishResponse(request.id, true, result, null);

        } catch (error) {
            console.error('[REDIS] Redis 메시지 처리 오류:', error);
            const requestId = message.id || 'unknown';
            await this.publishResponse(requestId, false, null, error.message);
        }
    }

    // Redis 연결 종료
    async disconnect() {
        try {
            if (this.client) await this.client.quit();
            if (this.subscriber) await this.subscriber.quit();
            if (this.publisher) await this.publisher.quit();
            console.log('[REDIS] Redis 클라이언트 연결 종료 완료');
        } catch (error) {
            console.error('[REDIS] Redis 클라이언트 연결 종료 오류:', error);
        }
    }

    // Getter methods
    getClient() {
        return this.client;
    }

    getSubscriber() {
        return this.subscriber;
    }

    getPublisher() {
        return this.publisher;
    }
}

// 싱글톤 인스턴스 생성
const redisManager = new RedisManager();

module.exports = redisManager; 