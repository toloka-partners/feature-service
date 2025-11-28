# feature-service
The feature-service microservice manages products, releases and features.

## TechStack
* Java, Spring Boot
* PostgreSQL, Flyway, Spring Data JPA
* Spring Security OAuth 2
* Apache Kafka, RabbitMQ (Event Messaging)
* Maven, JUnit 5, Testcontainers

## Prerequisites
* JDK 24 or later
* Docker ([installation instructions](https://docs.docker.com/engine/install/))
* [IntelliJ IDEA](https://www.jetbrains.com/idea/)
* PostgreSQL, Keycloak, Kafka, and RabbitMQ
 
Refer [docker-compose based infra setup](https://github.com/feature-tracker/docker-infra) for running dependent services.

## How to get started?

```shell
$ git clone https://github.com/feature-tracker/feature-service.git
$ cd feature-service

# Run tests
$ ./mvnw verify

# Format code
$ ./mvnw spotless:apply

# Run application
# Once the dependent services (PostgreSQL, Keycloak, Kafka, RabbitMQ, etc) are started,
# you can run/debug FeatureServiceApplication.java from your IDE.
```

## RabbitMQ Integration

The feature service integrates with RabbitMQ as an external messaging system to publish feature events for external consumption.

### Event Flow

1. **Internal Events**: When features are created, updated, or deleted, the application publishes internal Spring application events
2. **Event Relay**: The `FeatureEventListener` captures these internal events after successful database transaction commits
3. **RabbitMQ Publishing**: Events are transformed and published to RabbitMQ exchange using the `RabbitMQEventPublisher`
4. **Error Handling**: Failed publications trigger retry logic with exponential backoff, and permanently failed messages are routed to a dead letter queue

### Configuration

#### Environment Variables

```bash
# RabbitMQ Connection
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
RABBITMQ_VIRTUAL_HOST=

# Optional: Override default configuration
APP_RABBITMQ_EXCHANGE_NAME=feature.events
APP_RABBITMQ_RETRY_MAX_ATTEMPTS=3
```

#### Application Properties

The following properties can be configured in `application.properties`:

```properties
# RabbitMQ Connection Settings
app.rabbitmq.host=${RABBITMQ_HOST:localhost}
app.rabbitmq.port=${RABBITMQ_PORT:5672}
app.rabbitmq.username=${RABBITMQ_USERNAME:guest}
app.rabbitmq.password=${RABBITMQ_PASSWORD:guest}
app.rabbitmq.virtual-host=${RABBITMQ_VIRTUAL_HOST:}

# Exchange Configuration
app.rabbitmq.exchange.name=feature.events
app.rabbitmq.exchange.type=topic
app.rabbitmq.exchange.durable=true

# Routing Keys
app.rabbitmq.exchange.routing-keys.feature-created=feature.created
app.rabbitmq.exchange.routing-keys.feature-updated=feature.updated
app.rabbitmq.exchange.routing-keys.feature-deleted=feature.deleted

# Retry Configuration
app.rabbitmq.retry.max-attempts=3
app.rabbitmq.retry.initial-interval=1000
app.rabbitmq.retry.multiplier=2.0
app.rabbitmq.retry.max-interval=10000

# Dead Letter Configuration
app.rabbitmq.dead-letter.exchange-name=feature.events.dlx
app.rabbitmq.dead-letter.queue-name=feature.events.dlq
app.rabbitmq.dead-letter.routing-key=feature.failed
```

### Event Types

The service publishes the following event types to RabbitMQ:

1. **FeatureCreated** - Published when a new feature is created
   - Routing Key: `feature.created`
   - Payload: Feature details (id, code, title, description, status, etc.)

2. **FeatureUpdated** - Published when an existing feature is modified
   - Routing Key: `feature.updated`
   - Payload: Updated feature details including timestamps

3. **FeatureDeleted** - Published when a feature is deleted
   - Routing Key: `feature.deleted`
   - Payload: Deleted feature details with deletion metadata

### Message Format

All messages are published with the following wrapper format:

```json
{
  "eventType": "FeatureCreated|FeatureUpdated|FeatureDeleted",
  "payload": {
    "id": 1,
    "code": "FEAT-001",
    "title": "Feature Title",
    "description": "Feature Description",
    "status": "NEW",
    "releaseCode": "REL-001",
    "assignedTo": "user@example.com",
    "createdBy": "creator@example.com",
    "createdAt": "2023-12-01T10:00:00Z",
    "updatedBy": "updater@example.com",
    "updatedAt": "2023-12-01T11:00:00Z"
  },
  "timestamp": "2023-12-01T11:00:00Z"
}
```

### Error Handling and Monitoring

#### Retry Mechanism
- **Max Attempts**: Configurable (default: 3)
- **Backoff Strategy**: Exponential backoff with configurable multiplier
- **Initial Interval**: 1 second (configurable)
- **Max Interval**: 10 seconds (configurable)

#### Dead Letter Queue
- Failed messages after max retry attempts are routed to `feature.events.dlq`
- Dead letter messages include original payload and error context
- Monitor this queue for permanently failed events

#### Logging
- All publishing attempts are logged with event context
- Failures include error details and retry information
- Connection failures are handled gracefully without blocking the application

### RabbitMQ Setup

#### Using Docker

```bash
# Start RabbitMQ with management plugin
docker run -d --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=guest \
  -e RABBITMQ_DEFAULT_PASS=guest \
  rabbitmq:3.13-management

# Access management UI at http://localhost:15672
# Username: guest, Password: guest
```

#### Exchange and Queue Setup

The application automatically creates the required exchanges and queues on startup:

- **Main Exchange**: `feature.events` (topic exchange)
- **Dead Letter Exchange**: `feature.events.dlx` (topic exchange)
- **Dead Letter Queue**: `feature.events.dlq`

### Testing

#### Unit Tests
- `RabbitMQEventPublisherTest`: Tests event publishing logic with mocked dependencies
- `FeatureEventListenerTest`: Tests event listener functionality

#### Integration Tests
- `RabbitMQIntegrationTest`: End-to-end tests using Testcontainers RabbitMQ
- Verifies complete event flow from feature creation to RabbitMQ message publication

### Troubleshooting

#### Common Issues

1. **Connection Refused**
   - Verify RabbitMQ is running and accessible
   - Check host, port, and credentials configuration
   - Ensure firewall allows connections on port 5672

2. **Messages Not Published**
   - Check application logs for error messages
   - Verify exchange and routing key configuration
   - Ensure transactions are committing successfully

3. **High Dead Letter Queue Volume**
   - Monitor RabbitMQ connection stability
   - Check for serialization issues in event payloads
   - Review retry configuration settings

#### Monitoring Commands

```bash
# Check RabbitMQ status
docker exec rabbitmq rabbitmqctl status

# List exchanges
docker exec rabbitmq rabbitmqctl list_exchanges

# List queues
docker exec rabbitmq rabbitmqctl list_queues

# Monitor message rates
docker exec rabbitmq rabbitmqctl list_queues name messages_ready messages_unacknowledged
```
