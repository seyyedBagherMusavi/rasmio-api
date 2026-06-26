# Rasmio API Proxy

A Spring Boot proxy for selected Rasmio APIs under the `com.nicico.rasmio` package. Retrofit is used for upstream Rasmio calls, Lombok reduces boilerplate, and responses are stored as JSON documents in MongoDB and reused on later requests to reduce calls to Rasmio.

## Supported endpoints

| Local endpoint | Upstream Rasmio endpoint | Description |
| --- | --- | --- |
| `GET /api/company/{companyId}/info` | `GET /Company/{companyId}/Info` | Company registration/base information |
| `GET /api/company/{companyId}/people` | `GET /Company/{companyId}/People` | Official company members |

## Cache behavior

- By default, the service returns a cached MongoDB response when one exists for the same endpoint, company id, and requested `fields` set.
- If a new `fields` value is requested, it is treated as a different cache key and the proxy fetches Rasmio again.
- Use `refresh=true` to force an upstream request and replace the stored response.
- Responses include `X-Cache: HIT` or `X-Cache: MISS`.
- Every incoming request is logged with client IP, method, URI, query string, response status, and duration.
- Rasmio rate-limit headers (`X-TodayLimit`, `X-UsedToday`, `X-TotalLimit`, `X-UsedTotal`) are saved and forwarded when available.

## Configuration

Environment variables:

| Variable | Default | Description |
| --- | --- | --- |
| `RASMIO_API_KEY` | empty | Rasmio API key sent as `X-Key` |
| `RASMIO_BASE_URL` | `https://api.rasm.io/API` | Upstream API base URL |
| `MONGODB_URI` | `mongodb://localhost:27017/rasmio_proxy` | MongoDB connection string |
| `RASMIO_CONNECT_TIMEOUT` | `5s` | Upstream connection timeout |
| `RASMIO_READ_TIMEOUT` | `30s` | Upstream read timeout |

## Run locally

```bash
export RASMIO_API_KEY='your-api-key'
export MONGODB_URI='mongodb://localhost:27017/rasmio_proxy'
mvn spring-boot:run
```

Example:

```bash
curl 'http://localhost:8080/api/company/14009396050/info'
curl 'http://localhost:8080/api/company/14009396050/people?fields=id,title'
curl 'http://localhost:8080/api/company/14009396050/info?refresh=true'
```
