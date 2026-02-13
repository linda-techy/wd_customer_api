# Security Guidelines

## Rotating Secrets

If secrets have been exposed (e.g., committed to git), follow these steps immediately:

### 1. Generate New Secrets

```bash
# Generate new JWT secret
openssl rand -hex 32

# For other secrets, use your provider's secret rotation tools
```

### 2. Update Environment Variables

Update `.env` with new values:

```properties
JWT_SECRET=<new_generated_secret>
DB_PASSWORD=<new_database_password>
```

### 3. Update Database Passwords

Connect to your database and change the password:

```sql
ALTER USER your_username WITH PASSWORD 'new_secure_password';
```

### 4. Restart Services

After updating secrets:

```bash
# Stop the application
# Update .env file
# Restart the application
mvn spring-boot:run
```

### 5. Invalidate Old Sessions

When rotating JWT secrets, all existing user sessions will be invalidated automatically. Users will need to log in again.

## Removing Secrets from Git History

If secrets were committed to git:

### Option 1: Using git filter-branch (for local cleanup)

```bash
# Remove specific file from all commits
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch .env" \
  --prune-empty --tag-name-filter cat -- --all

# Force push (ONLY if you haven't shared the repository)
git push origin --force --all
```

### Option 2: Using BFG Repo-Cleaner (recommended)

```bash
# Install BFG
# https://rtyley.github.io/bfg-repo-cleaner/

# Remove all .env files from history
bfg --delete-files .env

# Clean up
git reflog expire --expire=now --all
git gc --prune=now --aggressive
```

### Option 3: Fresh Repository (safest for already-shared repos)

1. Create a new repository
2. Copy files (excluding .env files)
3. Commit to new repository
4. Update remote URL
5. Archive old repository

## Security Checklist

### Before Deployment

- [ ] All `.env` files are in `.gitignore`
- [ ] `.env.example` exists with no real secrets
- [ ] All secrets are environment-specific (dev ≠ staging ≠ production)
- [ ] JWT secrets are at least 32 characters (256 bits)
- [ ] Database passwords are strong (12+ characters, mixed case, numbers, symbols)
- [ ] `DDL_AUTO` is set to `validate` (never `update` or `create`)
- [ ] `SHOW_SQL` is set to `false`
- [ ] Logging level is `INFO` or `WARN` (not `DEBUG`)
- [ ] Error messages don't expose internal details (`ERROR_INCLUDE_MESSAGE=never`)

### Regular Maintenance

- [ ] Rotate JWT secrets every 90 days
- [ ] Rotate database passwords every 90 days
- [ ] Review and update dependencies monthly
- [ ] Monitor for security vulnerabilities
- [ ] Review access logs regularly

## Incident Response

If you discover a security incident:

1. **Immediately** rotate all affected credentials
2. Review logs for unauthorized access
3. Notify relevant stakeholders
4. Document the incident
5. Implement measures to prevent recurrence

## Reporting Security Issues

If you discover a security vulnerability, please report it to:
- Email: [Your Security Contact Email]
- Do NOT open a public GitHub issue

## Additional Resources

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security Best Practices](https://spring.io/guides/topicals/spring-security-architecture)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
