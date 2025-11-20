# J-Proxy

A lightweight HTTP proxy and mock server written in Java. Perfect for development, testing, and API simulation.

## Features

- **Mock Server Mode**: Serve mock responses from JSON config files
- **Proxy Mode**: Forward requests to real APIs with intelligent caching
- **LRU Caching**: Cache GET requests with configurable TTL
- **Rate Limiting**: Token bucket algorithm with multiple strategies
- **Request Logging**: JSON-formatted request logs
- **Statistics**: Real-time cache and rate limit metrics

## Quick Start

### Build
```bash
mvn clean package
```

### Run Mock Server
```bash
java -jar target/j-proxy-1.0.0.jar --mode mock --config config/mocks.json --port 8080
```

### Run Proxy Server
```bash
java -jar target/j-proxy-1.0.0.jar --mode proxy --target https://api.example.com --port 8080
```

## Configuration

### Mock Mode

Create a `config/mocks.json` file:
```json
{
  "endpoints": [
    {
      "path": "/users/123",
      "method": "GET",
      "response": {
        "status": 200,
        "headers": {
          "Content-Type": "application/json"
        },
        "body": {
          "id": 123,
          "name": "John Doe"
        }
      }
    }
  ]
}
```

### Proxy Mode

Create a `config/proxy-config.json` file:
```json
{
  "target_url": "https://api.example.com",
  "timeout": 30000,
  "cache_enabled": true,
  "cache_max_size": 100,
  "cache_ttl_seconds": 300,
  "rate_limit_enabled": true,
  "rate_limit_max_requests": 60,
  "rate_limit_window_seconds": 60,
  "rate_limit_key_strategy": "url"
}
```

## CLI Options
```
Usage: jproxy [-hV] [-c=<configPath>] [-m=<mode>] [-p=<port>] [-t=<targetUrl>]

Options:
  -p, --port=<port>       Port to listen on (default: 8080)
  -m, --mode=<mode>       Mode: mock or proxy (default: mock)
  -c, --config=<configPath> Path to config file
  -t, --target=<targetUrl> Target URL for proxy mode
  -h, --help              Show this help message
  -V, --version           Print version information
```

## Response Headers

### Caching
- `X-Cache: HIT|MISS` - Whether response was served from cache

### Rate Limiting
- `X-RateLimit-Limit` - Maximum requests allowed
- `X-RateLimit-Remaining` - Requests remaining in window
- `X-RateLimit-Reset` - Timestamp when limit resets
- `Retry-After` - Seconds to wait (when rate limited)

## Statistics Endpoint

Access real-time statistics at `/__stats`:
```bash
curl http://localhost:8080/__stats
```

Example response:
```json
{
  "service": "J-Proxy",
  "version": "1.0.0",
  "timestamp": 1234567890,
  "cache": {
    "enabled": true,
    "hits": 45,
    "misses": 10,
    "hit_rate_percent": "81.82",
    "size": 8,
    "max_size": 100
  },
  "rate_limit": {
    "enabled": true
  }
}
```

## Testing

### Run Unit Tests
```bash
mvn test
```

### Run Integration Tests
```bash
# Start server in mock mode
java -jar target/j-proxy-1.0.0.jar --mode mock --config config/mocks.json

# In another terminal, run tests
./scripts/integration-test.sh
```

### Test Rate Limiting
```bash
./scripts/test-rate-limit.sh
```

## Examples

### Mock a REST API
```bash
# Start mock server
java -jar target/j-proxy-1.0.0.jar --mode mock --config config/mocks.json

# Test endpoints
curl http://localhost:8080/users/123
curl http://localhost:8080/users
curl -X POST http://localhost:8080/users -d '{"name":"test"}'
```

### Proxy with Caching
```bash
# Start proxy
java -jar target/j-proxy-1.0.0.jar --mode proxy --target https://jsonplaceholder.typicode.com

# First request - cache MISS
curl -v http://localhost:8080/posts/1 2>&1 | grep X-Cache

# Second request - cache HIT
curl -v http://localhost:8080/posts/1 2>&1 | grep X-Cache
```

### Test Rate Limiting
```bash
# Configure rate limit: 10 requests per 60 seconds
# Then make rapid requests
for i in {1..15}; do
    curl -i http://localhost:8080/posts/1 | grep -E "HTTP|X-RateLimit"
done
```

## Project Structure
```
j-proxy/
├── src/
│   ├── main/java/com/jproxy/
│   │   ├── Main.java                 # CLI entry point
│   │   ├── ProxyServer.java          # HTTP server
│   │   ├── handler/                  # Request handlers
│   │   ├── core/                     # Cache & rate limiter
│   │   ├── model/                    # Data models
│   │   └── util/                     # Utilities
│   ├── main/resources/
│   │   ├── logback.xml              # Logging config
│   │   └── default-mocks.json       # Default mocks
│   └── test/                         # Unit tests
├── config/                           # User configs
├── logs/                             # Log files
└── scripts/                          # Test scripts
```

## Requirements

- Java 8+
- Maven 3.6+

## Dependencies

- Apache HttpClient 4.5.14
- Jackson 2.15.2
- Picocli 4.7.5
- SLF4J + Logback 1.2.12
- JUnit 5.9.3
- Mockito 4.11.0

## License

MIT License - see LICENSE file for details

## Contributing

Contributions welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Add tests for new features
4. Submit a pull request

## Roadmap

- [ ] HTTPS support
- [ ] WebSocket proxying
- [ ] Path parameter matching (/users/{id})
- [ ] Request/response transformation
- [ ] Plugin system
- [ ] GUI dashboard
- [ ] Docker container
- [ ] Kubernetes deployment

## Author

Built as a learning project to demonstrate production-quality Java development.