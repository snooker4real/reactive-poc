# Reactive Java PoC with Quarkus + Mutiny

This project is a practical PoC of:

> Reactive Java Doesn’t Have to Be Hard: Quarkus Makes It Simple

It demonstrates:

- Reactive REST endpoints using `Uni` and `Multi`
- Reactive PostgreSQL access with Hibernate Reactive Panache
- Reactive Kafka messaging with `MutinyEmitter`
- Backpressure handling with Mutiny (`onOverflow().dropPreviousItems()`)

## Run

Prerequisite: Docker running locally (Quarkus Dev Services will start PostgreSQL and Kafka automatically).

```bash
./mvnw quarkus:dev
```

## Endpoints

### Fruit CRUD (reactive DB)

- `GET /api/fruits`
- `GET /api/fruits/{id}`
- `POST /api/fruits` with body `{ "name": "Mango" }`
- `DELETE /api/fruits/{id}`

### Price messaging (reactive Kafka)

- `POST /api/prices/{price}` publishes a price event to Kafka
- `GET /api/prices/last` returns the latest consumed price

### Backpressure stream (Mutiny Multi + SSE)

- `GET /api/prices/stream?intervalMs=10&count=200`

The stream uses `onOverflow().dropPreviousItems()` to keep consumers responsive under producer pressure.

## Quick Try

```bash
# List seeded fruits
curl -s http://localhost:8080/api/fruits

# Create a fruit
curl -s -X POST http://localhost:8080/api/fruits \
  -H 'Content-Type: application/json' \
  -d '{"name":"Mango"}'

# Publish price events
curl -s -X POST http://localhost:8080/api/prices/101.25
curl -s http://localhost:8080/api/prices/last

# Watch stream (Ctrl+C to stop)
curl -N http://localhost:8080/api/prices/stream?intervalMs=5\&count=50
```
