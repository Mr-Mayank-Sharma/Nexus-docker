# Nexus — Production Docker Setup

Production-ready Docker orchestration for the **Nexus OMS & Supply Chain Platform**.

This repository contains everything needed to bring the **full Nexus stack** up on a
new machine with a single command. It is the canonical deployment source — when you
make Nexus live, update the Docker files here and re-pull on the target machine.

## What's in the stack

| Service      | Container        | Port  | Description                                   |
|--------------|------------------|-------|-----------------------------------------------|
| PostgreSQL   | `nexus-postgres` | 5433  | Primary database (pgvector/pg16)              |
| Redis        | `nexus-redis`    | 6379  | Cache & sessions                              |
| Kafka        | `nexus-kafka`    | 9092  | Event streaming / order lifecycle             |
| Backend      | `nexus-backend`  | 8080  | Spring Boot 3 OMS API (`/api/v1`)             |
| Frontend     | `nexus-frontend` | 80    | React + Vite SPA (nginx)                      |
| AI Ops       | `nexus-ai-ops`   | 5000  | Python ML — order routing                     |
| AI Intel     | `nexus-ai-intel` | 5001  | Python ML — demand & inventory                |
| Prometheus   | `nexus-prometheus`| 9090 | Metrics scraping                              |
| Grafana      | `nexus-grafana`  | 3001  | Dashboards & alerting                         |

## Quick Start (new machine)

### 1. Prerequisites

- Docker Engine 24+ and Docker Compose v2
- Git

### 2. Clone & configure

```bash
git clone https://github.com/Mr-Mayank-Sharma/Nexus-docker.git
cd Nexus-docker

# Create your environment file from the template
cp .env.example .env
# EDIT .env — set DB_PASSWORD, JWT_SECRET, GRAFANA_ADMIN_PASSWORD, and any
# carrier/SSO/OpenAI keys you need. Generate secrets with:
#   openssl rand -base64 48
```

### 3. Build & start the full stack

```bash
docker compose up -d --build
```

This builds the backend, frontend, and both AI services from source and starts
all infrastructure. First build takes several minutes (Maven + npm + pip).

### 4. Verify

```bash
# All services healthy
docker compose ps

# Backend health
curl http://localhost:8080/api/v1/actuator/health

# Frontend
open http://localhost

# Grafana (admin / your GRAFANA_ADMIN_PASSWORD)
open http://localhost:3001

# Prometheus
open http://localhost:9090
```

### 5. Stop / tear down

```bash
docker compose down          # stop containers (keep data)
docker compose down -v       # stop AND delete all data volumes
```

## Configuration

All configuration lives in `.env` (copied from `.env.example`). See
[`.env.example`](./.env.example) for the full list of variables and comments.

**Required (no defaults — must set):**

| Variable                | Purpose                          |
|-------------------------|----------------------------------|
| `DB_PASSWORD`           | PostgreSQL password              |
| `JWT_SECRET`            | JWT signing secret (≥32 chars)   |
| `GRAFANA_ADMIN_PASSWORD`| Grafana admin password           |

**Optional but recommended for production:**

| Variable            | Purpose                          |
|---------------------|----------------------------------|
| `OPENAI_API_KEY`    | Enable AI features               |
| `NEXUS_AI_ENABLED`  | `true` to enable AI              |
| `CORS_ORIGINS`      | Allowed frontend origins         |
| `SPRING_PROFILES_ACTIVE` | `prod` for hardened profile  |
| Carrier keys        | FedEx / UPS / USPS / DHL         |
| SSO keys            | Google / Microsoft / Okta / Auth0|

## Production hardening

The compose file ships with production defaults:

- **`SPRING_PROFILES_ACTIVE=prod`** — hardened Hikari pool, `ddl-auto=validate`,
  Flyway fail-fast, no anonymous sign-ups, graceful shutdown, no error bodies.
- **Healthchecks** on postgres, redis, kafka, backend — backend waits for
  postgres/redis healthy before starting.
- **Named volumes** for postgres, redis, kafka, prometheus, grafana — data
  survives container restarts.
- **`restart: unless-stopped`** on every service — auto-recovery on reboot/crash.
- **Prometheus + Grafana** pre-provisioned with the OMS dashboard and alert rules
  (error rate, low inventory, integration failures, latency, Kafka lag, service down).

## Updating for a live deployment

When you take Nexus live, edit the files here (ports, domains, TLS, resource
limits, secrets) and commit. On the target machine:

```bash
git pull
docker compose up -d --build
```

## Repository layout

```
Nexus-docker/
├── docker-compose.yml        # Full stack orchestration
├── .env.example              # All environment variables (copy to .env)
├── .gitignore                # Ignores .env, data, build artifacts
├── backend/
│   └── Dockerfile            # Spring Boot backend (multi-stage)
├── frontend/
│   ├── Dockerfile            # React SPA (multi-stage, nginx)
│   └── nginx.conf            # SPA + /api proxy config
├── ai-ops/
│   └── Dockerfile            # Python order-routing service
├── ai-intel/
│   └── Dockerfile            # Python demand/inventory service
├── monitoring/
│   ├── prometheus/
│   │   ├── prometheus.yml    # Scrape config
│   │   └── alerts.yml        # Alert rules
│   └── grafana/
│       ├── datasources/      # Provisioned Prometheus datasource
│       └── dashboards/       # OMS dashboard
└── scripts/
    ├── setup.sh              # One-shot: clone, .env, build, up
    └── healthcheck.sh        # Verify all services
```
