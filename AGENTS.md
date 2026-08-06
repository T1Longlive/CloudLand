# AGENTS.md

Cloudland: Spring Boot 2.7.14 + Java 8 + Vue 2.6 + MySQL + Redis. Land/product ordering platform.

## Build & Run

```bash
# Backend (port 9090)
mvn clean package -DskipTests    # no backend tests exist, mvn test is a no-op
mvn spring-boot:run              # main class: com.cloudland.CloudlandApplication

# Frontend (port 8080)
cd vue && npm install && npm run serve
npm run build                    # production build

# Full stack via Docker
docker-compose up -d             # nginx:80, backend:9090, mysql:3306, redis:6379

# Playwright E2E (requires backend + frontend + mysql + redis all running)
cd vue && npx playwright test                     # all tests
npx playwright test tests/login.spec.js           # single file
```

## Architecture

```
src/main/java/com/cloudland/
├── CloudlandApplication.java    # @SpringBootApplication + @EnableScheduling + @MapperScan
├── controller/                  # REST endpoints; result/ has Result.java, Code.java
├── service/ + service/impl/     # business logic interfaces + implementations
├── mapper/                      # MyBatis-Plus mappers; XML in resources/com/cloudland/mapper/
├── pojo/                        # entities (User, Land, Product, Order2, Msg, Trolley)
├── pojo/vo/                     # view objects (LandVO, OrderVO, ProductVO, TrolleyVo)
├── config/                      # WebMvcConfig, RedisConfig, AlipayConfig, etc.
├── Interceptor/                 # capital I — MyInterceptor.java
└── util/                        # JwtUtils, EmailUtils, FileUtil, OrderExporter, OrderClear

vue/src/
├── config/app.js               # APP_CONFIG (apiBaseUrl, resourceUrls) — use this, never hardcode URLs
├── constants/code.js            # business status codes, isSuccess()/isFailure() helpers
├── request/axiosInstance.js     # axios instance, baseURL from APP_CONFIG
├── request/interceptor.js       # auto-attaches token header, handles 401/402/403, refreshes token
├── utils/auth.js                # getStoredToken(), fetchCurrentUser(), loginWithPassword()
├── router/index.js              # routes + beforeEach auth guard
├── views/frontend/              # customer-facing pages
├── views/backend/               # admin/staff pages
└── components/                  # Top, Top2 shared nav components
```

Frontend-specific details: see `vue/CLAUDE.md`.

## Configuration

Backend context-path is `/api` — all endpoints are prefixed: `http://localhost:9090/api/...`.

Local dev override: create `config/application-local.yml` (gitignored, loaded via `spring.config.import`). Key env vars: `DB_URL`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PASSWORD`, `JWT_SIGN_KEY`, `FILE_STORAGE_ROOT` (default `D:/CloudLandFile`), `CORS_ALLOWED_ORIGINS`.

Frontend env: `vue/.env.local` sets `VUE_APP_API_BASE_URL` (dev uses `http://localhost:9090/api`, prod uses `http://8.137.114.176/api`).

## Auth & API Quirks

- Token header is `token`, not `Authorization`. Custom CORS headers: `token`, `remember`, `frond`, `code`, `contact`, `forgetPassword`.
- `POST /user/login` with an existing token (no password) acts as token validation + refresh. The `frond` and `remember` headers control behavior. Response includes refreshed token in `updatedToken` response header (lowercase).
- HTTP status codes carry auth signals: 401 = unauthenticated, 402 = role changed, 403 = account disabled. These are NOT standard HTTP semantics — the interceptor in `interceptor.js` handles them.
- MyInterceptor skips URLs containing: `login`, `register`, `code`, `forgetPassword`. Also skips `/resource/**`, `/land/page`, `/land/{id}`, `/product/page`, `/product/{id}`, `/msg`, `/msg/mail`.
- Business status codes (in `response.data.code`): 10000-10999 = CRUD, 20000-20999 = auth, 30000-30999 = email. Defined in `controller/result/Code.java` and mirrored in `vue/src/constants/code.js`.
- File upload endpoints use `multipart/form-data` with JSON strings (not objects) for data params.

## File Storage

Files stored outside project at `FILE_STORAGE_ROOT` (`D:/CloudLandFile` default):
- `{root}/UserIcon/` → `/resource/userFile/**`
- `{root}/LandFile/` → `/resource/landFile/**`
- `{root}/Product/` → `/resource/productFile/**`

## Testing

Playwright config: `vue/playwright.config.js`. Tests: `vue/tests/`. Helpers: `vue/tests/helpers/` (redis.js uses `ioredis` to write verification codes directly, bypassing email; cleanup.js uses admin APIs). Projects: webkit + Microsoft Edge (Edge has slowMo: 800ms). Workers: 1 (serial). Retries: 1 locally, 2 on CI.

## Deploy

Production: `scripts/deploy.sh` — git hard-reset, docker compose build, stops host nginx (port 80 conflict), starts containers, prunes images. CI: `.gitee/workflows/cloudland-deploy.yml` triggers on push to `main`, runs `scripts/deploy.sh` via SSH.

## Known Traps

- README says "Bootstrap 5" — actual UI is **Element UI + @opentiny/vue**. No Bootstrap in dependencies.
- pom.xml groupId is `com.couldland` (typo) but actual Java package is `com.cloudland`.
- Order entity is `Order2` (not `Order`) to avoid SQL keyword conflict.
- `Interceptor/` directory has a capital I — inconsistent with Java conventions.
- No backend unit tests exist. Only frontend Playwright E2E tests.
- `config/application-local.yml` contains real credentials (mail password, Alipay keys) — never commit this file.
