# Backend (E-commerce)

Development and run instructions.

## Available profiles
- `dev` - development profile. Enables Swagger/OpenAPI UI and uses a dev fallback JWT secret if none provided.
- `local` - alias for local development, same behavior as `dev`.
- (no profile) - production-like default. Production requires a valid `JWT_SECRET` and will fail-fast if missing or insecure.

## How to run locally
Set a (temporary) `JWT_SECRET` and activate the `dev` profile. Example (Unix/macOS):

```bash
# set a sufficiently long secret (32+ chars recommended)
export JWT_SECRET=dev-secret-very-long-string-32chars!!
export SPRING_PROFILES_ACTIVE=dev
mvn -DskipTests spring-boot:run -f backend/pom.xml
```

Windows (cmd):

```cmd
set JWT_SECRET=dev-secret-very-long-string-32chars!!
set SPRING_PROFILES_ACTIVE=dev
mvn -DskipTests spring-boot:run -f backend/pom.xml
```

If you omit `JWT_SECRET` when running with `dev` or `local`, the application will start using a development fallback secret but will log a clear warning. Do NOT use the fallback secret in production.

## How to run production
Provide a secure `JWT_SECRET` (32+ random characters) and run without `dev` or `local` profile. Production will fail startup if `JWT_SECRET` is missing or insecure.

```bash
export JWT_SECRET="<32+ char secure secret>"
mvn -DskipTests spring-boot:run -f backend/pom.xml
```

## Swagger / OpenAPI
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs  (or /v3/api-docs)

Swagger UI and docs are only publicly accessible when running with the `dev` or `local` profiles (or when `SecurityConfig` is modified).

## Notes
- The application performs a startup check to ensure a JWT secret is configured in non-dev environments to avoid accidental insecure deployments.
- If you need to access docs in non-dev environments, use a valid authenticated token (`POST /api/auth/login`) and include `Authorization: Bearer <token>` when requesting `/api-docs`.
