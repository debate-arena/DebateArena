// mediasoup/MediaSoupManager.js - MediaSoup SFU 객체 관리 및 Redis 메시지 처리
const mediasoup = require('mediasoup');
const { mediasoupConfig } = require('../config/AppConfig');

class MediaSoupManager {
    constructor(redisManager) {
        this.worker = null;
        this.routers = new Map();
        this.transports = new Map();
        this.roomTransportSizeMap = new Map();
        this.producers = new Map();
        this.consumers = new Map();
        this.userHasTransport = new Map();
        this.userHasConsumerTransport = new Map();
        this.redis = redisManager; // RedisManager instance
    }

    // MediaSoup 및 Redis 완전 초기화
    async initialize() {
        try {
            console.log('[MEDIASOUP] MediaSoup 서비스 초기화 시작');
            
            // 1. MediaSoup Worker 초기화
            await this.initializeWorker();
            
            // 2. Redis 초기화
            await this.redis.initialize();
            
            // 3. Redis 구독 설정
            await this.setupRedisSubscriptions();
            
            console.log('[MEDIASOUP] MediaSoup 서비스 초기화 완료');
        } catch (error) {
            console.error('[MEDIASOUP] MediaSoup 서비스 초기화 실패:', error);
            throw error;
        }
    }

    // MediaSoup worker 초기화
    async initializeWorker() {
        try {
            this.worker = await mediasoup.createWorker({
                logLevel: mediasoupConfig.worker.logLevel,
                logTags: mediasoupConfig.worker.logTags,
                rtcMinPort: mediasoupConfig.worker.rtcMinPort,
                rtcMaxPort: mediasoupConfig.worker.rtcMaxPort,
            });

            this.worker.on('died', () => {
                console.error('[MEDIASOUP] MediaSoup worker가 종료되었습니다');
                process.exit(1);
            });

            console.log('[MEDIASOUP] MediaSoup worker 초기화 성공');
            console.log(`[MEDIASOUP] Worker PID: ${this.worker.pid}`);
            console.log(`[MEDIASOUP] Worker 설정: logLevel=${this.worker.logLevel}`);
            
            return true;
        } catch (error) {
            console.error('[MEDIASOUP] MediaSoup worker 초기화 실패:', error);
            throw error;
        }
    }

    // Redis 구독 설정
    async setupRedisSubscriptions() {
        try {
            console.log('[MEDIASOUP] Redis 구독 설정 시작');
            
            const handlers = {
                createRouter: this.createRouter.bind(this),
                createTransport: this.createTransport.bind(this),
                connectTransport: this.connectTransport.bind(this),
                createProducer: this.createProducer.bind(this),
                createConsumer: this.createConsumer.bind(this),
                resumeConsumer: this.resumeConsumer.bind(this),
                disconnectTransport: this.disconnectTransport.bind(this),
                micOn: this.micOn.bind(this),
                micOff: this.micOff.bind(this)
            };
            
            await this.redis.subscribeToChannels(handlers);
            
            console.log('[MEDIASOUP] Redis 구독 설정 완료');
        } catch (error) {
            console.error('[MEDIASOUP] Redis 구독 설정 실패:', error);
            throw error;
        }
    }



    // Transport 이벤트 핸들러 설정
    setupTransportEventHandlers(transport, roomId, userEmail, isProducer, producerUserEmail, eventCallbacks) {
        transport.on('close', async () => {
            console.log(`Transport closed - ID: ${transport.id}`);
            this.transports.delete(transport.id);
            
            if (eventCallbacks.onClose) {
                await eventCallbacks.onClose({
                    roomId,
                    userEmail,
                    transportId: transport.id,
                });
            }
        });
        
        transport.on('dtlsstatechange', async (dtlsState) => {
            console.log(`DTLS state change - Transport ${userEmail}, ${dtlsState}`);
            
            if (dtlsState === 'failed' && eventCallbacks.onDisconnected) {
                await eventCallbacks.onDisconnected({
                    roomId,
                    userEmail,
                    transportId: transport.id,
                });
                console.log(`DTLS failed for transport ${transport.id}`);
            }
            
            if (dtlsState === 'connected' && eventCallbacks.onEstablished) {
                await eventCallbacks.onEstablished({
                    roomId,
                    isProducer,
                    consumerUserEmail: userEmail,
                    producerUserEmail
                });
                console.log(`DTLS connected for transport ${userEmail}, ${producerUserEmail}`);
            }
            
            if (dtlsState === 'closed') {
                console.log(`DTLS closed for transport ${userEmail}, ${producerUserEmail}`);
            }
        });
        
        transport.on('icestatechange', async (iceState) => {
            console.log(`ICE state change - Transport ${transport.id}: ${iceState}`);
            
            if (iceState === 'failed') {
                console.log(`❌ ICE failed for transport ${transport.id}`);
            }
            
            if (iceState === 'disconnected' && eventCallbacks.onDisconnected) {
                await eventCallbacks.onDisconnected({
                    roomId,
                    userEmail,
                    transportId: transport.id,
                });
                console.log(`ICE disconnected for transport ${userEmail}, ${producerUserEmail}`);
            }
        });

        transport.on('sctpstatechange', (connectionState) => {
            console.log(`Connection state change - Transport ${transport.id}: ${connectionState}`);
        });
    }



    // Transport 정리
    cleanupTransport(transportId, roomId) {
        try {
            console.log(`[CLEANUP] 전송 정리 시작 - ID: ${transportId}`);
            const transport = this.transports.get(transportId);
            
            if (transport && !transport.closed) {
                transport.close();
                this.decrementRoomTransportCount(roomId);
            }

            this.transports.delete(transportId);
            console.log(`[CLEANUP] 전송 정리 완료 - ID: ${transportId}`);
        } catch (error) {
            console.error(`[CLEANUP] 전송 정리 실패 - ID ${transportId}:`, error);
            // Transport 정리는 중요하지만 에러를 throw하지 않음 (시스템이 계속 동작해야 함)
        }
    }



    // Helper methods
    addUserToTransport(email, transportId) {
        if (this.userHasTransport.has(email)) {
            this.userHasTransport.get(email).push(transportId);
        } else {
            this.userHasTransport.set(email, [transportId]);
        }
    }

    addUserToConsumerTransport(email, transportId) {
        if (this.userHasConsumerTransport.has(email)) {
            this.userHasConsumerTransport.get(email).push(transportId);
        } else {
            this.userHasConsumerTransport.set(email, [transportId]);
        }
    }

    incrementRoomTransportCount(roomId) {
        const currentCount = this.roomTransportSizeMap.get(roomId) || 0;
        this.roomTransportSizeMap.set(roomId, currentCount + 1);
    }

    decrementRoomTransportCount(roomId) {
        const currentCount = this.roomTransportSizeMap.get(roomId) || 0;
        if (currentCount > 0) {
            this.roomTransportSizeMap.set(roomId, currentCount - 1);
        }
    }

    // 시스템 상태 조회
    getSystemStatus() {
        return {
            worker: {
                pid: this.worker?.pid,
                died: this.worker?.died || false
            },
            stats: {
                routers: this.routers.size,
                transports: this.transports.size,
                producers: this.producers.size,
                consumers: this.consumers.size,
                userHasTransport: this.userHasTransport.size,
                userHasConsumerTransport: this.userHasConsumerTransport.size
            },
            roomTransportSizes: Object.fromEntries(this.roomTransportSizeMap)
        };
    }

    // 상세 로그 출력
    async logDetailedStatus() {
        try {
            console.log('\n=== MediaSoup 상태 ===');
            console.log(`[STATUS] 활성 라우터 수: ${this.routers.size}`);
            console.log(`[STATUS] 활성 전송 수: ${this.transports.size}`);
            console.log(`[STATUS] 활성 송신자 수: ${this.producers.size}`);
            console.log(`[STATUS] 활성 수신자 수: ${this.consumers.size}`);
            console.log(`[STATUS] 사용자별 송신 전송 수: ${this.userHasTransport.size}`);
            console.log(`[STATUS] 사용자별 수신 전송 수: ${this.userHasConsumerTransport.size}`);
            
            console.log('[STATUS] 방별 전송 수:');
            this.roomTransportSizeMap.forEach((value, key) => {
                console.log(`  방 ${key}: ${value}개`);
            });

            console.log('[STATUS] 사용자별 송신 전송 매핑:');
            this.userHasTransport.forEach((value, key) => {
                console.log(`  사용자 ${key}: ${value}`);
            });

            console.log('[STATUS] 사용자별 수신 전송 매핑:');
            this.userHasConsumerTransport.forEach((value, key) => {
                console.log(`  사용자 ${key}: ${value}`);
            });

            // Router별 상세 정보
            for (const [roomId, router] of this.routers.entries()) {
                try {
                    console.log(`[STATUS] 라우터 상세 - 방: ${roomId}`);
                    const routerDump = await router.dump();
                    const transportCount = routerDump.transportIds.length;
                    routerDump.transportIds.forEach(id => console.log(`  전송 ID: ${id}`));
                    console.log(`  라우터가 관리하는 전송 개수: ${transportCount}`);
                } catch (error) {
                    console.error(`[STATUS] 라우터 상태 조회 실패 - 방 ${roomId}:`, error);
                }
            }
            
            console.log('========================\n');
        } catch (error) {
            console.error(`[STATUS] 상세 상태 로깅 실패:`, error);
        }
    }

    // Worker 종료
    closeWorker() {
        try {
            if (this.worker) {
                this.worker.close();
                console.log('[MEDIASOUP] MediaSoup worker 종료 완료');
            } else {
                console.log('[MEDIASOUP] MediaSoup worker가 초기화되지 않았습니다');
            }
        } catch (error) {
            console.error('[MEDIASOUP] MediaSoup worker 종료 실패:', error);
            // Worker 종료 실패는 로그만 남기고 진행 (프로세스 종료 시에는 어차피 정리됨)
        }
    }

    // Getter methods
    getWorker() {
        return this.worker;
    }

    getRouter(roomId) {
        return this.routers.get(roomId);
    }

    getTransport(transportId) {
        return this.transports.get(transportId);
    }

    getProducer(producerId) {
        return this.producers.get(producerId);
    }

    getConsumer(consumerId) {
        return this.consumers.get(consumerId);
    }



    // ============= Redis 메시지 핸들러들 =============

    // 공통 에러 처리 유틸리티
    async handleError(error, operation, payload, publishMethod) {
        console.error(`[${operation}] Error:`, error);
        
        const errorResponse = {
            userEmail: payload.userEmail,
            error: error.message,
            ...this.extractBasicInfo(payload)
        };
        // await publishMethod.call(this.redis, errorResponse);
    }

    // 기본 정보 추출 유틸리티
    extractBasicInfo(payload) {
        const basicInfo = {};
        
        // 공통 필드들 추출
        if (payload.roomId) basicInfo.roomId = payload.roomId;
        if (payload.transportId) basicInfo.transportId = payload.transportId;
        if (payload.producerId) basicInfo.producerId = payload.producerId;
        if (payload.consumerId) basicInfo.consumerId = payload.consumerId;
        if (payload.sessionId) basicInfo.sessionId = payload.sessionId;
        if (payload.isProducer !== undefined) basicInfo.isProducer = payload.isProducer;
        if (payload.producerUserEmail) basicInfo.producerUserEmail = payload.producerUserEmail;
        
        return basicInfo;
    }

    // 성공 로그 유틸리티
    logSuccess(operation, details) {
        const stats = this.getSystemStatus().stats;
        console.log(`[${operation}] 성공 - ${details}`);
        console.log(`[시스템 상태] 라우터:${stats.routers} 전송:${stats.transports} 송신자:${stats.producers} 수신자:${stats.consumers}`);
    }

    // Router 생성 (Redis 메시지 처리)
    async createRouter(message) {
        const payload = JSON.parse(message);
        const { roomId } = payload;
        
        console.log(`[ROUTER] 라우터 생성 시작 - 방: ${roomId}, 사용자: ${payload.userEmail}`);
        
        try {
            // Router 생성 로직 직접 구현
            let router = this.routers.get(roomId);
            if (!router) {
                router = await this.worker.createRouter({ 
                    mediaCodecs: mediasoupConfig.mediaCodecs 
                });
                this.routers.set(roomId, router);
                this.roomTransportSizeMap.set(roomId, 0);
                
                router.on('close', () => {
                    console.log(`[ROUTER] 라우터가 닫혔습니다 - 방: ${roomId}`);
                    this.routers.delete(roomId);
                    this.roomTransportSizeMap.delete(roomId);
                });
                
                console.log(`[ROUTER] 라우터 생성 완료 - 방: ${roomId}, ID: ${router.id}`);
            } else {
                console.log(`[ROUTER] 기존 라우터 사용 - 방: ${roomId}, ID: ${router.id}`);
            }

            const response = {
                type: "createdRouter",
                userEmail: payload.userEmail,
                id: router.id,
                rtpCapabilities: router.rtpCapabilities,
                roomId: roomId
            };

            await this.redis.publishRouterCreated(response);
            this.logSuccess('createRouter', `Router ID: ${router.id}, Room: ${roomId}`);
            
        } catch (error) {
            await this.handleError(error, 'createRouter', payload, this.redis.publishRouterCreated);
        }
    }

    // Transport 생성 (Redis 메시지 처리)
    async createTransport(message) {
        const payload = JSON.parse(message);
        const { roomId, isProducer, producerUserEmail, sessionId } = payload;
        
        console.log(`[TRANSPORT] 전송 생성 시작 - 방: ${roomId}, 사용자: ${payload.userEmail}, 타입: ${isProducer ? '송신' : '수신'}`);
        
        try {
            // Router 확인
            const router = this.routers.get(roomId);
            if (!router) {
                throw new Error(`방 ${roomId}에 대한 라우터를 찾을 수 없습니다`);
            }

            // Transport 생성
            const transport = await router.createWebRtcTransport({
                listenInfos: mediasoupConfig.webRtcTransport.listenInfos,
                enableUdp: mediasoupConfig.webRtcTransport.enableUdp,
                enableTcp: mediasoupConfig.webRtcTransport.enableTcp,
                preferUdp: mediasoupConfig.webRtcTransport.preferUdp,
            });

            // Transport를 사용자에게 매핑
            this.addUserToTransport(payload.userEmail, transport.id);
            this.transports.set(transport.id, transport);
            this.incrementRoomTransportCount(roomId);

            console.log(`[TRANSPORT] 전송 생성 완료 - ID: ${transport.id}, 타입: ${isProducer ? '송신' : '수신'}, 방 전송 수: ${this.roomTransportSizeMap.get(roomId)}`);
            
            // Transport 이벤트 핸들러 설정
            this.setupTransportEventHandlersForRedis(transport, roomId, payload.userEmail, isProducer, producerUserEmail);
            
            const response = {
                type: "createdTransport",
                userEmail: payload.userEmail,
                transportId: transport.id,
                roomId: roomId,
                dtlsParameters: transport.dtlsParameters,
                iceCandidates: transport.iceCandidates,
                iceParameters: transport.iceParameters,
                isProducer,
                producerUserEmail: producerUserEmail,
                sessionId: sessionId
            };

            await this.redis.publishTransportCreated(response);
            this.logSuccess('createTransport', `Transport ID: ${transport.id}, Type: ${isProducer ? 'Producer' : 'Consumer'}`);
            
        } catch (error) {
            await this.handleError(error, 'createTransport', payload, this.redis.publishTransportCreated);
        }
    }

    // Redis용 Transport 이벤트 핸들러 설정
    setupTransportEventHandlersForRedis(transport, roomId, userEmail, isProducer, producerUserEmail) {
        const eventCallbacks = {
            onClose: async (data) => {
                console.log(`[EVENT] 전송 닫힘 - ID: ${data.transportId}, 사용자: ${data.userEmail}`);
                await this.redis.publishTransportDisconnected(data);
            },
            onDisconnected: async (data) => {
                console.log(`[EVENT] 전송 연결 끊김 - ID: ${data.transportId}, 사용자: ${data.userEmail}`);
                await this.redis.publishTransportDisconnected(data);
            },
            onEstablished: async (data) => {
                console.log(`[EVENT] 전송 연결 확립 - 수신자: ${data.consumerUserEmail}, 송신자: ${data.producerUserEmail}`);
                await this.redis.publishTransportEstablished(data);
            }
        };
        
        this.setupTransportEventHandlers(
            transport, 
            roomId, 
            userEmail, 
            isProducer, 
            producerUserEmail, 
            eventCallbacks
        );
    }

    // Transport 연결 (Redis 메시지 처리)
    async connectTransport(message) {
        const payload = JSON.parse(message);
        const { transportId, dtlsParameters, userEmail, roomId } = payload;
        
        console.log(`[TRANSPORT] 전송 연결 시작 - ID: ${transportId}, 사용자: ${userEmail}`);
        
        try {
            // Transport 연결 로직 직접 구현
            const transport = this.transports.get(transportId);
            if (!transport) {
                throw new Error(`전송 ID ${transportId}를 찾을 수 없습니다`);
            }

            await transport.connect({ dtlsParameters });
            console.log(`[TRANSPORT] 전송 연결 완료 - ID: ${transportId}`);
            
            const response = {
                userEmail: userEmail,
                connectedTransportId: transportId,
                roomId: roomId
            };

            await this.redis.publishTransportConnected(response);
            this.logSuccess('connectTransport', `Transport ID: ${transportId}, User: ${userEmail}`);
            
        } catch (error) {
            await this.handleError(error, 'connectTransport', payload, this.redis.publishTransportConnected);
        }
    }

    // Producer 생성 (Redis 메시지 처리)
    async createProducer(message) {
        const payload = JSON.parse(message);
        const { transportId, kind, rtpParameters } = payload;
        
        console.log(`[PRODUCER] 송신자 생성 시작 - 전송: ${transportId}, 종류: ${kind}, 사용자: ${payload.userEmail}`);
        
        try {
            // Producer 생성 로직 직접 구현
            const transport = this.transports.get(transportId);
            if (!transport) {
                throw new Error(`전송 ID ${transportId}를 찾을 수 없습니다`);
            }

            // TODO : 추후 삭제
            const paused = true;
            const producer = await transport.produce({ 
                kind, 
                rtpParameters,
                paused,
            });

            this.producers.set(producer.id, producer);
            
            console.log(`[PRODUCER] 송신자 생성 완료 - ID: ${producer.id}, 종류: ${kind}, 일시정지: ${producer.paused}`);
            
            producer.on('transportclose', () => {
                console.log(`[EVENT] 송신자 전송 닫힘 - ID: ${producer.id}`);
                this.producers.delete(producer.id);
            });
            
            const response = {
                userEmail: payload.userEmail,
                roomId: payload.roomId,
                producerId: producer.id,
                kind
            };

            await this.redis.publishProducerCreated(response);
            this.logSuccess('createProducer', `Producer ID: ${producer.id}, Kind: ${kind}, Transport: ${transportId}`);
            
        } catch (error) {
            await this.handleError(error, 'createProducer', payload, this.redis.publishProducerCreated);
        }
    }

    // Consumer 생성 (Redis 메시지 처리)
    async createConsumer(message) {
        const payload = JSON.parse(message);
        const { producerId, rtpCapabilities, transportId, producerUserEmail, roomId } = payload;
        
        console.log(`[CONSUMER] 수신자 생성 시작 - 송신자: ${producerId}, 전송: ${transportId}, 사용자: ${payload.userEmail}`);
        
        try {
            // Consumer 생성 로직 직접 구현
            const transport = this.transports.get(transportId);
            if (!transport) {
                throw new Error(`전송 ID ${transportId}를 찾을 수 없습니다`);
            }

            const producer = this.producers.get(producerId);
            if (!producer) {
                throw new Error(`송신자 ID ${producerId}를 찾을 수 없습니다`);
            }

            // TODO : 추후 삭제
            const paused = false;
            const consumer = await transport.consume({
                producerId,
                rtpCapabilities,
                paused,
            });

            this.consumers.set(consumer.id, consumer);
            
            console.log(`[CONSUMER] 수신자 생성 완료 - ID: ${consumer.id}, 종류: ${consumer.kind}, 일시정지: ${consumer.paused}, 송신자: ${producerId}`);
            
            consumer.on('transportclose', () => {
                console.log(`[EVENT] 수신자 전송 닫힘 - ID: ${consumer.id}`);
                this.consumers.delete(consumer.id);
            });

            // Producer 사용자를 Consumer Transport에 매핑
            this.addUserToConsumerTransport(producerUserEmail, transportId);
            
            const response = {
                userEmail: payload.userEmail,
                consumerId: consumer.id,
                producerId: producerId,
                rtpParameters: consumer.rtpParameters,
                kind: consumer.kind,
                producerUserEmail: producerUserEmail,
                roomId: roomId
            };

            await this.redis.publishConsumerCreated(response);
            this.logSuccess('createConsumer', `Consumer ID: ${consumer.id}, Producer: ${producerId}, Kind: ${consumer.kind}`);
            
        } catch (error) {
            await this.handleError(error, 'createConsumer', payload, this.redis.publishConsumerCreated);
        }
    }

    // Consumer resume (Redis 메시지 처리)
    async resumeConsumer(message) {
        const payload = JSON.parse(message);
        const { consumerId } = payload;
        
        console.log(`[CONSUMER] 수신자 재개 시작 - ID: ${consumerId}, 사용자: ${payload.userEmail}`);
        
        try {
            // Consumer resume 로직 직접 구현
            const consumer = this.consumers.get(consumerId);
            if (!consumer) {
                throw new Error(`수신자 ID ${consumerId}를 찾을 수 없습니다`);
            }

            await consumer.resume();
            console.log(`[CONSUMER] 수신자 재개 완료 - ID: ${consumerId}, 일시정지: ${consumer.paused}`);
            
            console.log(`[CONSUMER] 수신자 재개 성공 - ID: ${consumerId}`);
            return { success: true };
            
        } catch (error) {
            console.error(`[CONSUMER] 수신자 재개 오류:`, error);
            throw error; // resumeConsumer는 직접 응답하지 않고 에러를 throw
        }
    }

    // Producer Mic On (Redis 메시지 처리)
    async micOn(message) {
        const payload = JSON.parse(message);
        const { producerId } = payload;
        
        console.log(`[MIC] 마이크 켜기 시작 - 송신자: ${producerId}`);
        
        try {
            // Producer resume 로직 직접 구현
            const producer = this.producers.get(producerId);
            if (!producer) {
                throw new Error(`송신자 ID ${producerId}를 찾을 수 없습니다`);
            }

            await producer.resume();
            console.log(`[MIC] 송신자 재개 완료 - ID: ${producerId}, 일시정지: ${producer.paused}`);
            
            console.log(`[MIC] 마이크 켜기 성공 - 송신자: ${producerId}`);
            
        } catch (error) {
            console.error(`[MIC] 마이크 켜기 오류 - 송신자 ${producerId}:`, error);
        }
    }

    // Producer Mic Off (Redis 메시지 처리)
    async micOff(message) {
        const payload = JSON.parse(message);
        const { producerId } = payload;
        
        console.log(`[MIC] 마이크 끄기 시작 - 송신자: ${producerId}`);
        
        try {
            // Producer pause 로직 직접 구현
            const producer = this.producers.get(producerId);
            if (!producer) {
                throw new Error(`송신자 ID ${producerId}를 찾을 수 없습니다`);
            }

            await producer.pause();
            console.log(`[MIC] 송신자 일시정지 완료 - ID: ${producerId}, 일시정지: ${producer.paused}`);
            
            console.log(`[MIC] 마이크 끄기 성공 - 송신자: ${producerId}`);
            
        } catch (error) {
            console.error(`[MIC] 마이크 끄기 오류 - 송신자 ${producerId}:`, error);
        }
    }

    // Transport 연결 해제 (Redis 메시지 처리)
    async disconnectTransport(message) {
        const payload = JSON.parse(message);
        const { roomId, userEmail } = payload;
        
        console.log(`[DISCONNECT] 전송 연결 해제 시작 - 사용자: ${userEmail}, 방: ${roomId}`);
        
        try {
            // 사용자별 Transport 정리 로직 직접 구현
            console.log(`[CLEANUP] 사용자 정리 시작 - 사용자: ${userEmail}, 방: ${roomId}`);
            
            // 사용자의 Producer Transport들 정리
            const userTransports = this.userHasTransport.get(userEmail) || [];
            console.log(`[CLEANUP] 송신 전송 정리 중 - ${userTransports.length}개, 사용자: ${userEmail}`);
            userTransports.forEach(id => this.cleanupTransport(id, roomId));
            this.userHasTransport.delete(userEmail);
        }catch(error){
            console.error(`[DISCONNECT] 연결 해제 오류 - 사용자: ${userEmail}, 방: ${roomId}:`, error);
        }
        try{

            // 사용자의 Consumer Transport들 정리
            const userConsumerTransports = this.userHasConsumerTransport.get(userEmail) || [];
            console.log(`[CLEANUP] 수신 전송 정리 중 - ${userConsumerTransports.length}개, 사용자: ${userEmail}`);
            userConsumerTransports.forEach(id => this.cleanupTransport(id, roomId));
            this.userHasConsumerTransport.delete(userEmail);
        } catch (error) {
            console.error(`[DISCONNECT] 연결 해제 오류 - 사용자: ${userEmail}, 방: ${roomId}:`, error);
        }
        try{
            // 방에 더 이상 Transport가 없으면 Router 정리
            const currentTransportCount = this.roomTransportSizeMap.get(roomId) || 0;
            if (currentTransportCount === 0) {
                const router = this.routers.get(roomId);
                if (router) {
                    router.close();
                    this.routers.delete(roomId);
                    this.roomTransportSizeMap.delete(roomId);
                    console.log(`[CLEANUP] 라우터 삭제 완료 - 방: ${roomId}`);
                }
            }
            
            console.log(`[CLEANUP] 사용자 정리 완료 - 사용자: ${userEmail}, 방: ${roomId}`);
            
            const status = this.getSystemStatus();
            console.log(`[DISCONNECT] 연결 해제 성공 - 사용자: ${userEmail}`);
            console.log(`[시스템 상태] 라우터:${status.stats.routers} 전송:${status.stats.transports} 송신자:${status.stats.producers} 수신자:${status.stats.consumers}`);
            console.log(`[방 ${roomId} 전송 수]: ${status.roomTransportSizes[roomId] || 0}`);
        }catch(error){
            console.error(`[DISCONNECT] 연결 해제 오류 - 사용자: ${userEmail}, 방: ${roomId}:`, error);
        }
    }


}

// 클래스를 직접 export (인스턴스는 server.js에서 생성)
module.exports = MediaSoupManager; 