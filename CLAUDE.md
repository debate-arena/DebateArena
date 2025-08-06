# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot debate application that enables real-time debate functionality through WebSocket connections. The application supports STT (Speech-to-Text) message processing and debate room management.

## Common Development Commands

### Build and Test
- **Build project**: `./gradlew build`
- **Run tests**: `./gradlew test`
- **Build without tests**: `./gradlew build -x test`
- **Run application**: `./gradlew bootRun`

### Docker Commands
- **Build Docker image**: `docker build -t debate-app .`
- **Run with Docker**: `docker run -p 8082:8082 debate-app`

## Architecture Overview

### Core Components

1. **WebSocket Communication**
   - Main WebSocket endpoint: `/ws`
   - Message broker prefixes: `/pub` (client to server), `/sub` (server to client broadcasts), `/queue` (point-to-point)
   - Uses STOMP protocol over WebSocket

2. **STT Message Processing**
   - Two types of STT messages: Opinion STT and Battle STT
   - Real-time broadcasting to all participants in a room
   - Message accumulation and processing logic in `RoomManager`

3. **Debate Room Management**
   - `DebateService` manages room creation and STT processing
   - `RoomManager` handles per-room state and message broadcasting
   - Rooms stored in `ConcurrentHashMap` for thread-safe access

### Key Classes and Responsibilities

- **DebateApiController**: WebSocket message handlers for STT messages (`/debate/stt/opinion`, `/debate/stt/battle`)
- **DebateRoomApiController**: REST API for room management (`/rooms/*`)
- **DebateService**: Core business logic for room creation, STT processing, and message broadcasting
- **RoomManager**: Per-room state management and message coordination
- **WebSocketConfig**: WebSocket and STOMP configuration

### Database Structure

- **DebateRoom**: Main room entity with topic and match type
- **Topic**: Debate topics with first/second options
- **User**: Basic user entity with JWT authentication

### Security and Authentication

- JWT-based authentication with `JwtProvider` and `JwtAuthenticationFilter`
- WebSocket messages authenticated using `Principal`
- Swagger API documentation enabled

### Data Flow

1. Clients connect via WebSocket (`/ws` endpoint)
2. STT messages sent to `/pub/debate/stt/opinion` or `/pub/debate/stt/battle`
3. Messages processed by `DebateService` and broadcasted to room participants
4. Room state managed by `RoomManager` instances

## Technology Stack

- **Framework**: Spring Boot 3.5.4
- **Java Version**: 21
- **Database**: MySQL (production), H2 (development)
- **Message Broker**: Spring's SimpleBroker for WebSocket
- **Authentication**: JWT with Spring Security
- **Documentation**: Swagger/OpenAPI 3
- **Additional**: Kafka integration (planned), Redis for caching

## Development Notes

- The application runs on port 8082 by default
- Test topics are automatically created on startup if none exist
- Room management uses in-memory `ConcurrentHashMap` (consider Redis for production scaling)
- STT message processing includes both real-time broadcasting and accumulation for AI processing