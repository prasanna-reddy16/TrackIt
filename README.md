# TrackIt - real-time vehicle tracking

Live bus/cab tracking with a built-in GPS simulator (Bengaluru routes), geofence alerts, ETA, nearby search and trip replay.

## Architecture

```
GPS device / Simulator --> POST /api/location  (X-API-KEY)
                              |
                        LocationService.ingest
              +---------------+-----------------------------+
              | Redis GEO + hash  (latest position)         |  nearby search = GEORADIUS
              | in-memory buffer -> Postgres every 5 s       |  batched trip history
              | geofence check (500 m) -> alert (dedup 10 m) |
              +---------------+-----------------------------+
                              | publish JSON
                        Redis pub/sub channel "tracking"
                              |   (every app instance subscribes)
                        STOMP /topic/vehicles, /topic/route/{id}, /topic/alerts/{id}
                              |
                        Leaflet + OpenStreetMap map in the browser
```

* **Scales horizontally** - WebSocket fan-out goes through Redis pub/sub, so any instance can receive a GPS ping and every instance pushes it to its own browsers.
* **Redis GEO** for "buses near me"; **Postgres** for history/replay with an index on (vehicle, time).
* **Batched writes** - history inserts are buffered and saved in bulk.
* **Simulator** - `trackit.simulator.*` in `application.yml`. 2 routes x 5 buses by default; raise `vehicles-per-route` to load test.

## Run

```bash
docker compose up -d      # Postgres + Redis
mvn spring-boot:run       # http://localhost:8080
mvn test
# or: docker compose --profile full up --build
```

## API

| Method | Path | Notes |
|---|---|---|
| GET | /api/routes | routes with ordered stops |
| GET | /api/vehicles/live | latest state of all vehicles |
| GET | /api/vehicles/nearby?lat=&lng=&radiusKm= | Redis GEORADIUS |
| GET | /api/vehicles/{id}/eta?stopId= | straight-line ETA |
| GET | /api/vehicles/{id}/history?minutes= | trip replay points |
| POST | /api/location | `X-API-KEY`; `{vehicleId, lat, lng, speedKmph}` |
| POST | /api/routes, /api/vehicles | `X-API-KEY` |
| WS | /ws (SockJS/STOMP) | `/topic/vehicles`, `/topic/route/{id}`, `/topic/alerts/{id}` |

Real device example:
```bash
curl -X POST localhost:8080/api/location -H 'X-API-KEY: dev-key' -H 'Content-Type: application/json' \
  -d '{"vehicleId":"KA01-R1-01","lat":12.9756,"lng":77.6068,"speedKmph":28}'
```

## Measure real numbers for your resume
Run two instances (`--server.port=8081`), connect browsers to both, and watch updates arrive on each. Raise `vehicles-per-route` to 50 and note CPU/latency before quoting "100+ vehicles".

## Known limits / next steps
Plain Postgres (no PostGIS); add it if you want spatial queries on routes. ETA is straight-line, not road-based. Only write endpoints use an API key. Add auth/roles for production use.
