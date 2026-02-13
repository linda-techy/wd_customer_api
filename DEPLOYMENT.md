# WD Customer API Deployment Guide

## Production Deployment Runbook

### Pre-Deployment Checklist

- [ ] All tests passing (run `test_api_endpoints.sh`)
- [ ] Environment variables configured
- [ ] `.env` file created from `.env.example`
- [ ] JWT secret matches portal API
- [ ] Database connection verified (uses same DB as portal API)
- [ ] Storage path matches portal API
- [ ] SSL certificates updated (if applicable)
- [ ] Monitoring configured

### Environment Setup

#### 1. Environment Variables

Copy `.env.example` to `.env` and configure:

```bash
cp .env.example .env
nano .env
```

**Critical Variables:**
- `JWT_SECRET`: **MUST match wd_portal_api** for token compatibility
- `DB_URL`: **MUST be same database as wd_portal_api**
- `DB_USERNAME` / `DB_PASSWORD`: Database credentials
- `STORAGE_BASE_PATH`: **MUST match wd_portal_api** for file sharing

#### 2. Verify Database Connection

```bash
# Test connection (should use same DB as portal API)
psql -U postgres -d wdTestDB -c "SELECT COUNT(*) FROM customer_projects;"
```

#### 3. Build Application

```bash
# Clean and build
./mvnw clean package -DskipTests

# Verify build
ls -lh target/cust-api-*.jar
```

### Deployment Steps

#### Option A: Manual Deployment

1. **Stop Current Service**
```bash
sudo systemctl stop wd-customer-api
```

2. **Backup Current Version**
```bash
sudo cp /opt/api/wd-customer-api.jar /opt/api/wd-customer-api.jar.backup-$(date +%Y%m%d-%H%M%S)
```

3. **Deploy New Version**
```bash
sudo cp target/cust-api-*.jar /opt/api/wd-customer-api.jar
sudo chown apiuser:apigroup /opt/api/wd-customer-api.jar
```

4. **Update Environment**
```bash
sudo cp .env /opt/api/.env
sudo chown apiuser:apigroup /opt/api/.env
sudo chmod 600 /opt/api/.env
```

5. **Start Service**
```bash
sudo systemctl start wd-customer-api
```

6. **Verify Startup**
```bash
sudo systemctl status wd-customer-api
sudo journalctl -u wd-customer-api -f
```

7. **Health Check**
```bash
curl http://localhost:8080/actuator/health
```

#### Option B: Docker Deployment

1. **Build Docker Image**
```bash
docker build -t wd-customer-api:latest .
```

2. **Run Container**
```bash
docker run -d \
  --name wd-customer-api \
  --env-file .env \
  -p 8080:8080 \
  -v /path/to/storage:/storage \
  --restart unless-stopped \
  wd-customer-api:latest
```

3. **Verify Container**
```bash
docker logs -f wd-customer-api
docker exec wd-customer-api curl http://localhost:8080/actuator/health
```

### Post-Deployment Verification

1. **Run Test Suite**
```bash
export JWT_TOKEN="your-test-customer-token"
./test_api_endpoints.sh
```

2. **Check Key Endpoints**
```bash
# Health check
curl http://localhost:8080/actuator/health

# Dashboard (customer-specific)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/dashboard
```

3. **Verify Authorization**
```bash
# Test that customers can only see their own data
# Should return only customer's projects
curl -H "Authorization: Bearer $CUSTOMER_TOKEN" http://localhost:8080/api/customer/projects
```

4. **Monitor Logs**
```bash
# Check for authorization denials
tail -f /var/log/wd-customer-api/application.log | grep "UnauthorizedException"

# Check correlation IDs
tail -f /var/log/wd-customer-api/application.log | grep correlationId
```

### Rollback Procedure

If issues detected:

1. **Stop Service**
```bash
sudo systemctl stop wd-customer-api
```

2. **Restore Previous Version**
```bash
sudo cp /opt/api/wd-customer-api.jar.backup-YYYYMMDD-HHMMSS /opt/api/wd-customer-api.jar
```

3. **Restart Service**
```bash
sudo systemctl start wd-customer-api
```

### Monitoring

**Key Metrics to Monitor:**
- HTTP 500 error rate (should be near 0%)
- HTTP 403 error rate (authorization denials)
- HTTP 404 error rate
- Response time P95 (target: < 500ms)
- Cache hit rate (target: > 80%)
- Authorization check failures

**Security Monitoring:**
- Track failed authorization attempts
- Monitor for unauthorized data access attempts
- Alert on suspicious patterns

### Troubleshooting

#### Issue: Authentication Token Mismatch

**Symptom:** Tokens from portal app don't work in customer API

**Solution:**
1. Verify JWT_SECRET matches between both APIs
2. Check token expiration settings are consistent
3. Ensure both APIs use same JWT library version

#### Issue: Customer Seeing Wrong Data

**Symptom:** Customer can see projects they shouldn't access

**Solution:**
1. Check AuthorizationService.checkProjectAccess logs
2. Verify getProjectsForUser returns correct project list
3. Review cache expiration (may be serving stale data)
4. Clear cache: restart service or wait for 5-minute TTL

#### Issue: File Access Errors

**Symptom:** Customer can't view documents/photos

**Solution:**
1. Verify STORAGE_BASE_PATH matches portal API
2. Check file permissions (apiuser must have read access)
3. Verify file paths in database are correct

### Security Considerations

**Customer Data Isolation:**
- All API endpoints MUST verify customer authorization
- Never expose company-internal data
- Always filter by customer's accessible projects

**Data Privacy:**
- Customers can only see their own projects
- No access to financial data
- No access to other customers' information

**Audit Trail:**
- All authorization failures are logged
- Correlation IDs track all requests
- Failed access attempts monitored

### Emergency Procedures

**Critical Security Issue:**
1. Immediately revoke affected JWT tokens
2. Rotate JWT secret (requires coordinated deployment with portal API)
3. Audit access logs for unauthorized access
4. Notify security team

**Data Exposure:**
1. Identify scope of exposure
2. Notify affected customers
3. Review and strengthen authorization checks
4. Conduct security audit

### Contact Information

**On-Call Engineer:** [Your contact]
**Security Team:** [Security contact]
**Database Admin:** [DBA contact]
