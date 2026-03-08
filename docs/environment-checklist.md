# Environment Deployment Checklist

Use this checklist before starting or deploying `wd_customer_api`.

## 1) Validate configuration variables

The application consumes configuration using **Spring Profiles** mapping (`application.yml` + environment-specific yml files).

Ensure you have mapped your variables correctly on the deployment server:
- `SPRING_PROFILES_ACTIVE=production`

## 2) Required production-grade checks

Ensure the following variables are strictly provided securely on your production environment:
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` are set to the target environment DB.
- `JWT_SECRET` is set and at least 32 chars.
- `CUSTOMER_PORTAL_BASE_URL` points to the correct customer app URL (e.g. `https://app.walldotbuilders.com`).
- `CORS_ALLOWED_ORIGINS` matches allowed UI origins for that environment.
- If `EMAIL_ENABLED=true`, SMTP values are real and tested:
  - `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`

## 3) Start API

```bash
# Execute your compiled JAR file
java -jar cust-api-0.0.1-SNAPSHOT.jar
```

*Note: The application will fail-fast at startup if `application-production.yml` attempts to boot without `DB_PASSWORD` or `JWT_SECRET` configured correctly as system environment variables.*
