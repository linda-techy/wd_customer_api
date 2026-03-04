# Environment Deployment Checklist

Use this checklist before starting or deploying `wd_customer_api`.

## 1) Validate configuration files

Environment file mapping:
- local -> `.env`
- staging -> `.env.staging`
- production -> `.env.production`

Run structural checks (allows placeholders for template-derived files):

```powershell
.\scripts\check-env.ps1 -Environment local -AllowPlaceholderValues
.\scripts\check-env.ps1 -Environment staging -AllowPlaceholderValues
.\scripts\check-env.ps1 -Environment production -AllowPlaceholderValues
```

Run strict checks (no placeholders, required for real deploys):

```powershell
.\scripts\check-env.ps1 -Environment local
.\scripts\check-env.ps1 -Environment staging
.\scripts\check-env.ps1 -Environment production
```

## 2) Required production-grade checks

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` are set to the target environment DB.
- `JWT_SECRET` is set and at least 32 chars.
- `CUSTOMER_PORTAL_BASE_URL` points to the correct customer app URL.
- `cors.allowed-origins` matches allowed UI origins for that environment.
- If `EMAIL_ENABLED=true`, SMTP values are real and tested:
  - `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`

## 3) Start API with preflight

```powershell
.\scripts\start-api.ps1 -Environment local
.\scripts\start-api.ps1 -Environment staging
.\scripts\start-api.ps1 -Environment production
```

`start-api.ps1` runs fail-fast preflight by default and will stop on missing or placeholder critical values.
