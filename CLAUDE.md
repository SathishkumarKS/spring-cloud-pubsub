# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

- **Build:** `./gradlew build`
- **Run tests:** `./gradlew test`
- **Run a single test class:** `./gradlew test --tests "io.satkuppu.learn.spring_cloud_pubsub.order.service.OrderServiceTest"`
- **Run a single test method:** `./gradlew test --tests "io.satkuppu.learn.spring_cloud_pubsub.order.OrderControllerIntegrationTests.POST api_orders creates order and publishes event to PubSub"`
- **Run app locally:** `./gradlew bootRun`
- **Clean build:** `./gradlew clean build`

## Tech Stack

- **Language:** Kotlin 1.9, Java 21
- **Framework:** Spring Boot 3.5 with Spring Cloud Stream
- **Messaging:** Google Cloud Pub/Sub via `spring-cloud-gcp-pubsub-stream-binder`
- **Database:** H2 in-memory, JPA/Hibernate
- **Build:** Gradle (Kotlin DSL)
- **Testing:** JUnit 5, MockMvc, Mockito-Kotlin, Testcontainers (GCloud Pub/Sub emulator)

## Architecture

This is a Spring Boot application that manages orders and publishes order events to Google Cloud Pub/Sub via Spring Cloud Stream.

### Order Domain (single bounded context under `order/`)

- **controller/** — REST API at `/api/orders` (CRUD endpoints)
- **service/** — Business logic; publishes `OrderReceivedEvent` to Pub/Sub binding `order-out-0` after transaction commit using `StreamBridge`
- **repository/** — Spring Data JPA repository
- **model/** — JPA entity (`Order`) and enum (`OrderStatus`: CREATED, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)
- **dto/** — Request/response DTOs with Jakarta validation annotations
- **event/** — Event data classes for Pub/Sub messages
- **exception/** — `OrderNotFoundException` and `@ControllerAdvice` exception handler

### Key Pattern: Transactional Event Publishing

`OrderService.createOrder()` uses `TransactionSynchronizationManager.registerSynchronization()` to publish Pub/Sub events only after the database transaction commits successfully.

### Spring Cloud Stream Bindings (application.yaml)

- `order-out-0` → `order-events` topic
- `hello-out-0` → `hello-topic`
- Default binder: `pubsub`
- Pub/Sub emulator configured at `localhost:8085` by default

### Test Profiles

- **Unit tests** (`OrderControllerTest`, `OrderServiceTest`): Use `@WebMvcTest` / Mockito, no Spring profile needed
- **Integration tests** (`OrderControllerIntegrationTests`): Use `@ActiveProfiles("integration")`, Testcontainers Pub/Sub emulator with `@DynamicPropertySource`, require Docker running
