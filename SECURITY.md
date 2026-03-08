# Security Guidelines

## Rotating Secrets

If secrets have been exposed (e.g., printed to logs or accidentally shared), follow these steps immediately:

### 1. Generate New Secrets

```bash
# Generate new JWT secret
openssl rand -hex 32

# For other secrets, use your provider's secret rotation tools
```

### 2. Update Environment Variables

Update your hosting provider's environment variables (e.g. AWS Secrets Manager, Heroku Config Vars, System Environment Variables) with the new values:
- `JWT_SECRET`
- `DB_PASSWORD`

### 3. Update Database Passwords

Connect to your database and change the password:

```sql
ALTER USER your_username WITH PASSWORD 'new_secure_password';
```

### 4. Restart Services

After updating secrets on your hosting environment, restart the application to pull the new variables.

### 5. Invalidate Old Sessions

When rotating JWT secrets, all existing user sessions will be invalidated automatically as old tokens will no longer be verified. Users will need to log in again.

## Security Checklist

### Before Deployment

- [ ] All production secrets are provided securely as system environment variables.
- [ ] No hardcoded tokens, passwords, or secrets exist in the source code.
- [ ] `SPRING_PROFILES_ACTIVE` is explicitly set to `production`.
- [ ] JWT secrets are at least 32 characters (256 bits).
- [ ] Database passwords are strong (12+ characters, mixed case, numbers, symbols).
- [ ] Ensure that `application-production.yml` strictly enforces these behaviors.

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
