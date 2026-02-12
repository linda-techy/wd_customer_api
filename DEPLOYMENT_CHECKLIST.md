# Site Report Error Handling - Deployment Checklist

## Pre-Deployment Verification

### Code Quality ✅
- [x] All unit tests passing
- [x] No linter errors
- [x] Code reviewed
- [x] Documentation complete

### Database Preparation
- [ ] Backup production database
- [ ] Test migration on staging database
- [ ] Verify indexes created successfully
- [ ] Validate no data migration required

### Configuration Review
- [ ] Review application.yml for cache settings
- [ ] Verify logging configuration includes MDC pattern
- [ ] Check database connection pool settings
- [ ] Validate JWT authentication configuration

### Testing on Staging
- [ ] Deploy to staging environment
- [ ] Run smoke tests
- [ ] Validate 404 responses for missing resources
- [ ] Validate 403 responses for unauthorized access
- [ ] Validate 200 responses for authorized access
- [ ] Check correlation IDs in response headers
- [ ] Verify correlation IDs in log files
- [ ] Test cache behavior (hit/miss)
- [ ] Load test with 100 concurrent users
- [ ] Verify database query count reduced

## Deployment Steps

### Step 1: Database Migration
```bash
# Run Flyway migration
./gradlew flywayMigrate -Dflyway.url=jdbc:postgresql://prod-db:5432/wdTestDB

# Verify indexes created
psql -d wdTestDB -c "\d site_reports"
psql -d wdTestDB -c "\d site_report_photos"
```

### Step 2: Application Deployment
```bash
# Build application
./gradlew clean build -x test

# Stop current application
systemctl stop wd-customer-api

# Backup current deployment
cp /opt/wd-customer-api/app.jar /opt/wd-customer-api/app.jar.backup

# Deploy new version
cp build/libs/wd-customer-api.jar /opt/wd-customer-api/app.jar

# Start application
systemctl start wd-customer-api

# Check logs
tail -f /var/log/wd-customer-api/application.log
```

### Step 3: Smoke Tests
```bash
# Test health endpoint
curl http://localhost:8080/actuator/health

# Test authentication
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123"}'

# Test site reports endpoint (with token)
curl http://localhost:8080/api/customer/site-reports \
  -H "Authorization: Bearer ${TOKEN}"

# Verify correlation ID in response
curl -i http://localhost:8080/api/customer/site-reports \
  -H "Authorization: Bearer ${TOKEN}" | grep X-Correlation-ID
```

### Step 4: Validation
- [ ] Check application started successfully
- [ ] Verify database connections established
- [ ] Confirm cache manager initialized
- [ ] Validate correlation ID filter active
- [ ] Test API endpoints respond correctly

## Monitoring Setup

### Metrics to Track
```
- http_server_requests_seconds{uri="/api/customer/site-reports",status="200"}
- http_server_requests_seconds{uri="/api/customer/site-reports",status="404"}
- http_server_requests_seconds{uri="/api/customer/site-reports",status="403"}
- http_server_requests_seconds{uri="/api/customer/site-reports",status="500"}
- cache_gets{cache="userProjects",result="hit"}
- cache_gets{cache="userProjects",result="miss"}
```

### Alert Configuration
```yaml
alerts:
  - name: High 500 Error Rate
    condition: rate(http_5xx[5m]) > 0.01
    severity: critical
    
  - name: Slow API Responses
    condition: http_request_duration_p95 > 2s
    severity: warning
    
  - name: Low Cache Hit Rate
    condition: cache_hit_rate < 0.7
    severity: warning
```

### Log Monitoring
```bash
# Monitor for errors
tail -f /var/log/wd-customer-api/application.log | grep ERROR

# Monitor for unauthorized access
tail -f /var/log/wd-customer-api/application.log | grep "Unauthorized access"

# Monitor for resource not found
tail -f /var/log/wd-customer-api/application.log | grep "Resource not found"

# Track correlation IDs
tail -f /var/log/wd-customer-api/application.log | grep -o '\[.*\]' | head
```

## Post-Deployment Validation

### Immediate Checks (First 15 minutes)
- [ ] Application responding to health checks
- [ ] No 500 errors in logs
- [ ] Correlation IDs present in logs
- [ ] Database queries executing successfully
- [ ] Cache initialized and operational

### Short-term Monitoring (First 2 hours)
- [ ] Error rate stable or improved
- [ ] Response times within acceptable range
- [ ] No memory leaks detected
- [ ] Database connection pool healthy
- [ ] Cache hit rate tracking toward target

### Medium-term Validation (First 24 hours)
- [ ] Overall error rate decreased
- [ ] 404/403 errors properly categorized
- [ ] No increase in 500 errors
- [ ] Performance metrics stable
- [ ] Cache hit rate >70%
- [ ] User feedback positive

## Rollback Plan

### Rollback Criteria
Rollback if ANY of these occur:
- HTTP 500 error rate increases by >10%
- Any critical functionality broken
- Performance degrades by >20%
- Database errors occur
- Authorization bypass detected

### Rollback Steps
```bash
# 1. Stop application
systemctl stop wd-customer-api

# 2. Restore previous version
cp /opt/wd-customer-api/app.jar.backup /opt/wd-customer-api/app.jar

# 3. Rollback database (if needed - should NOT be needed)
# This implementation has no schema changes, only indexes
# Indexes can remain - they won't hurt

# 4. Start application
systemctl start wd-customer-api

# 5. Verify rollback successful
curl http://localhost:8080/actuator/health

# 6. Notify team
echo "Rollback completed at $(date)" | mail -s "Deployment Rollback" team@example.com
```

## Success Criteria

### Functional Success ✅
- [ ] All endpoints return correct status codes
- [ ] 404 only for missing resources
- [ ] 403 only for unauthorized access
- [ ] 500 only for genuine server errors
- [ ] Correlation IDs in all responses
- [ ] Error messages are clear and consistent

### Performance Success ✅
- [ ] P95 response time < 500ms
- [ ] Database query count reduced 40-60%
- [ ] Cache hit rate > 70% after warm-up
- [ ] No memory leaks detected
- [ ] CPU usage stable

### Observability Success ✅
- [ ] All errors logged with correlation IDs
- [ ] Authorization failures audited
- [ ] No stack traces in API responses
- [ ] Structured logging operational
- [ ] Metrics collection functional

## Communication Plan

### Stakeholders to Notify

**Before Deployment**:
- Development team
- QA team
- DevOps team
- Product owner

**After Deployment**:
- Development team (deployment complete)
- Support team (what to monitor)
- Product owner (success metrics)
- Customers (if any API changes)

### Communication Template

```
Subject: Site Reports Error Handling - Deployment Complete

Team,

The site reports error handling improvements have been deployed to production.

What Changed:
- Proper HTTP status codes (404, 403, 500)
- Correlation IDs for request tracing
- Performance optimization (caching, indexes)
- Enhanced error logging

What to Monitor:
- Error rates (should improve)
- Response times (should improve)
- Cache hit rate (target >70%)

Rollback Plan:
- Available if critical issues occur
- Estimated time: 5 minutes

Correlation ID Header:
- X-Correlation-ID now in all responses
- Use for troubleshooting

Support:
- Check IMPLEMENTATION_SUMMARY.md for details
- Report issues with correlation ID

Status: ✅ Deployed successfully
Next Check: [timestamp + 2 hours]

Thanks,
[Your Name]
```

## Emergency Contacts

- **DevOps Lead**: [contact]
- **Backend Lead**: [contact]
- **On-Call Engineer**: [contact]
- **Database Admin**: [contact]

## Documentation Links

- Implementation Summary: `IMPLEMENTATION_SUMMARY.md`
- API Documentation: `API_DOCUMENTATION.md`
- Database Schema: `DATABASE_SCHEMA.md`
- Technical Plan: See attached plan file

## Sign-Off

### Deployment Approval
- [ ] Technical Lead: _________________ Date: _______
- [ ] QA Lead: _________________ Date: _______
- [ ] DevOps Lead: _________________ Date: _______

### Post-Deployment Sign-Off
- [ ] Smoke Tests Passed: _________________ Date: _______
- [ ] Monitoring Configured: _________________ Date: _______
- [ ] Documentation Updated: _________________ Date: _______
- [ ] Stakeholders Notified: _________________ Date: _______

---

**Deployment Status**: Ready for production deployment
**Risk Assessment**: LOW (with proper testing and monitoring)
**Estimated Downtime**: < 2 minutes
**Rollback Time**: < 5 minutes
