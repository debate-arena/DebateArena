# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot application that serves as a signaling server for WebRTC/SFU (Selective Forwarding Unit) communication using MediaSoup. The application handles real-time communication between clients through STOMP over WebSocket connections with JWT authentication and integrates with an external MediaSoup server for media processing via Redis pub/sub messaging.

## Architecture

### Core Components

- **SignalingController**: STOMP message controller that handles client messages for join, transport creation, producer/consumer operations
- **MediasoupPublisher**: Service that publishes messages to Redis channels for MediaSoup server communication
- **MediasoupSubscriber**: Service that handles Redis pub/sub responses and forwards them to STOMP clients
- **SocketMessageListener**: Redis message listener that processes MediaSoup server responses
- **JwtChannelInterceptor**: Custom WebSocket channel interceptor for JWT authentication
- **RoomManager**: In-memory management of rooms and participants with session tracking
- **SessionManager**: WebSocket session lifecycle management
- **WebSocketEventListener**: Handles WebSocket connection/disconnection events

### Key Configuration

- **STOMP over WebSocket**: Endpoint at `/signaling` with CORS enabled, supports both SockJS and native WebSocket
- **Message Broker**: Simple broker with `/topic` and `/queue` destinations, application prefix `/signaling`, user prefix `/user`
- **JWT Authentication**: Required for WebSocket connections via Authorization header in STOMP CONNECT frame
- **Redis**: Local Redis instance (localhost:6379) with dedicated pub/sub channels for MediaSoup events
- **MediaSoup Integration**: Asynchronous communication with external MediaSoup server via Redis
- **SSL**: Application includes SSL verification bypass for development (security consideration)
- **Security**: Spring Security configured with stateless sessions, JWT authentication handled at WebSocket level

### Authentication Flow

1. Client obtains JWT token via REST endpoint `/api/auth/token`
2. Client connects to WebSocket with JWT token in Authorization header
3. JwtChannelInterceptor validates token during STOMP CONNECT
4. Authenticated user information stored in session attributes
5. All subsequent STOMP operations use authenticated session

### Message Flow

1. Client authenticates and connects via STOMP to `/signaling` endpoint
2. SignalingController handles STOMP messages with `/signaling` prefix (e.g., `/signaling/join`, `/signaling/createTransport`)
3. MediasoupPublisher publishes requests to Redis channels for MediaSoup server
4. External MediaSoup server processes requests and publishes responses to Redis
5. SocketMessageListener receives Redis messages and MediasoupSubscriber forwards to STOMP clients via `/user/{sessionId}/queue/*` destinations
6. Room and session state managed in-memory with cleanup on disconnect

### STOMP Topic Structure

**Client to Server (via `/signaling` prefix):**
- `/signaling/join` - Join room request
- `/signaling/createTransport` - Create transport request
- `/signaling/connectTransport` - Connect transport request
- `/signaling/createProducer` - Create producer request
- `/signaling/createConsumer` - Create consumer request
- `/signaling/resume` - Resume consumer request

**Server to Client (via `/user/{sessionId}/queue/` prefix):**
- `/user/{sessionId}/queue/router` - Router creation response
- `/user/{sessionId}/queue/transport` - Transport creation response
- `/user/{sessionId}/queue/transport-connected` - Transport connection response
- `/user/{sessionId}/queue/producer` - Producer creation response
- `/user/{sessionId}/queue/consumer` - Consumer creation response
- `/user/{sessionId}/queue/new-producer` - New producer notification
- `/user/{sessionId}/queue/participant-left` - Participant disconnection notification

### Redis Pub/Sub Channels

**Published by Signaling Server:**
- `mediasoup:router:create` - Router creation requests
- `mediasoup:transport:create` - Transport creation requests
- `mediasoup:transport:connect` - Transport connection requests
- `mediasoup:producer:create` - Producer creation requests
- `mediasoup:consumer:create` - Consumer creation requests
- `mediasoup:consumer:resume` - Consumer resume requests

**Subscribed by Signaling Server:**
- `mediasoup:router:created` - Router creation responses
- `mediasoup:transport:created` - Transport creation responses
- `mediasoup:transport:connected` - Transport connection responses
- `mediasoup:producer:created` - Producer creation responses
- `mediasoup:consumer:created` - Consumer creation responses

## Development Commands

### Build and Run
```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Run tests
./gradlew test
```

### Development Setup Requirements
- Java 21
- Redis server running on localhost:6379
- External MediaSoup server (URL configured in MediasoupPublisher)

### Project Structure
- `src/main/java/com/arena/test04spring/`
  - `controller/`: STOMP message controllers
  - `service/`: Business logic for MediaSoup integration
  - `config/`: Spring configuration (STOMP, Redis, Session/Room management)
  - `dto/`: Data transfer objects for API communication
  - `model/`: Domain models (Room, Participant)
  - `subscriber/`: Redis message listeners

### Key Dependencies
- Spring Boot 3.5.3 with WebSocket, Messaging, and Redis support
- Jackson for JSON processing
- Lombok for boilerplate code reduction
- Lettuce Redis client

### Development Notes
- Application name: test04spring
- Debug logging enabled for main package
- SSL configured but commented out in application.properties
- Uses Java 21 toolchain
- Gradle 8.14.3 wrapper configured

## Configuration Details

### JWT Configuration
- Default secret key: `mySecretKeyForSignalingServerAuthentication1234567890`
- Token validation on every STOMP CONNECT
- User email stored as JWT subject
- 24-hour token expiration for authentication endpoint

### WebSocket Configuration
- Endpoint: `/signaling` (with and without SockJS fallback)
- CORS: Allows all origins (`*`) for development
- Channel interceptor: JWT authentication required
- Session attributes: `userEmail` and `token` stored per session

### Redis Configuration  
- Connection: localhost:6379 (Lettuce client)
- String serialization for keys and values
- Message listener container for pub/sub
- Dedicated channels for each MediaSoup operation type

### Security Configuration
- CSRF disabled
- Stateless session management
- All endpoints permit all (authentication handled at WebSocket level)
- SSL verification bypass for development environments

## Data Models

### Core DTOs
- **RouterInfo**: Router creation response with RTP capabilities and participant list
- **TransportInfo**: Transport creation details with DTLS/ICE parameters
- **ProducerCreated**: Producer creation confirmation with room and session info
- **ConsumerInfo**: Consumer creation details for media consumption
- **TransportConnectedResponseDto**: Transport connection confirmation

### Domain Models
- **Room**: Container for roomId, matchType, and participant sessions
- **Participant**: Session identifier with optional producer ID

## API Endpoints

### REST Authentication
```bash
# Get JWT token for WebSocket authentication
POST /api/auth/token
Content-Type: application/json
{
  "userEmail": "user@example.com"
}

# Response
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### WebSocket Connection
```javascript
// Connect with JWT token
const token = "Bearer eyJhbGciOiJIUzI1NiJ9...";
const socket = new SockJS('/signaling');
const stompClient = Stomp.over(socket);

stompClient.connect(
  { 'Authorization': token },
  onConnected,
  onError
);
```

## Session and Room Management

### Session Lifecycle
1. **Connection**: JWT validation → session creation → user info storage
2. **Join Room**: Room assignment → participant registration
3. **Media Operations**: Transport/Producer/Consumer creation via MediaSoup
4. **Disconnection**: Session cleanup → participant removal → room cleanup

### Room Operations
- Rooms created dynamically when participants join
- In-memory participant tracking with ConcurrentHashMap
- Automatic cleanup when rooms become empty
- Cross-participant notifications for join/leave events

## Development Workflow

### Testing JWT Authentication
```bash
# 1. Get token
curl -X POST http://localhost:8080/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{"userEmail":"test@example.com"}'

# 2. Use token in WebSocket connection Authorization header
```

### Debugging Tips
- Enable debug logging: `logging.level.com.arena.test04spring=debug`
- Monitor Redis channels: `redis-cli monitor`
- Check WebSocket connections in browser DevTools
- Review session cleanup in disconnect events

### Common Issues
- **JWT Authentication Failure**: Check Authorization header format in STOMP CONNECT
- **Redis Connection**: Ensure Redis server is running on localhost:6379
- **MediaSoup Integration**: Verify external MediaSoup server is accessible
- **SSL Issues**: Development mode disables SSL verification (security consideration)

## Architecture Patterns

### Asynchronous Communication
- Redis pub/sub decouples signaling server from MediaSoup server
- Non-blocking message processing with dedicated channels
- Error handling via exception logging (not blocking)

### Session Management
- In-memory storage for development (not production-ready for clustering)
- ConcurrentHashMap for thread-safe operations
- Session-to-room mapping for efficient lookups

### Security Model
- JWT-based authentication at WebSocket connection level
- Stateless Spring Security configuration
- User context stored in WebSocket session attributes

## Production Considerations

### Scalability
- Current in-memory storage limits to single instance
- Consider Redis or database for session/room persistence
- Load balancer needs sticky sessions for WebSocket connections

### Security
- SSL verification bypass should be removed for production
- JWT secret should be externalized and rotated
- CORS configuration should be restricted to specific origins
- Consider rate limiting for authentication endpoints

### Monitoring
- Add metrics for WebSocket connections, room counts, message throughput
- Health checks for Redis connectivity and MediaSoup integration
- Error tracking for failed MediaSoup operations