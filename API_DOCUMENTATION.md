# Customer API - Site Reports Documentation

## Overview

The Customer API provides read-only access to site reports for authenticated customers. Customers can only view reports for projects they are assigned to.

## Authentication

All endpoints require JWT authentication via Bearer token:

```
Authorization: Bearer <jwt-token>
```

## Endpoints

### Get Site Reports

Retrieve paginated list of site reports for the authenticated customer's projects.

**Endpoint**: `GET /api/customer/site-reports`

**Query Parameters**:
- `projectId` (optional): Filter by specific project ID
- `page` (optional, default: 0): Page number (0-indexed)
- `size` (optional, default: 20): Page size

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Site reports retrieved successfully",
  "data": {
    "content": [
      {
        "id": 123,
        "title": "Daily Progress Report",
        "description": "Completed foundation work",
        "reportDate": "2026-02-13T10:30:00",
        "status": "SUBMITTED",
        "reportType": "DAILY_PROGRESS",
        "projectId": 1,
        "projectName": "Building A",
        "photos": [
          {
            "id": 1,
            "photoUrl": "/api/storage/site-reports/123/photo1.jpg",
            "storagePath": "site-reports/123/photo1.jpg",
            "createdAt": "2026-02-13T10:30:00"
          }
        ]
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20
    },
    "totalElements": 50,
    "totalPages": 3
  }
}
```

**Error Responses**:

- **403 Forbidden** - User lacks access to specified project:
```json
{
  "success": false,
  "message": "Access denied",
  "errorCode": "ACCESS_DENIED",
  "correlationId": "uuid-here",
  "timestamp": 1707840000000,
  "path": "/api/customer/site-reports"
}
```

- **404 Not Found** - Specified project doesn't exist:
```json
{
  "success": false,
  "message": "Project not found with id: 999",
  "errorCode": "RESOURCE_NOT_FOUND",
  "correlationId": "uuid-here",
  "timestamp": 1707840000000,
  "path": "/api/customer/site-reports"
}
```

### Get Site Report by ID

Retrieve a specific site report by ID.

**Endpoint**: `GET /api/customer/site-reports/{id}`

**Path Parameters**:
- `id`: Site report ID

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Site report retrieved successfully",
  "data": {
    "id": 123,
    "title": "Daily Progress Report",
    "description": "Completed foundation work",
    "reportDate": "2026-02-13T10:30:00",
    "status": "SUBMITTED",
    "reportType": "DAILY_PROGRESS",
    "projectId": 1,
    "projectName": "Building A",
    "photos": [...]
  }
}
```

**Error Responses**:

- **404 Not Found** - Report doesn't exist:
```json
{
  "success": false,
  "message": "SiteReport not found with id: 999",
  "errorCode": "RESOURCE_NOT_FOUND",
  "correlationId": "uuid-here",
  "timestamp": 1707840000000,
  "path": "/api/customer/site-reports/999"
}
```

- **403 Forbidden** - User lacks access to report's project:
```json
{
  "success": false,
  "message": "Access denied",
  "errorCode": "ACCESS_DENIED",
  "correlationId": "uuid-here",
  "timestamp": 1707840000000,
  "path": "/api/customer/site-reports/123"
}
```

## Error Codes

| Error Code | HTTP Status | Description |
|------------|-------------|-------------|
| `RESOURCE_NOT_FOUND` | 404 | The requested resource doesn't exist |
| `ACCESS_DENIED` | 403 | User lacks permission to access the resource |
| `INVALID_ARGUMENT` | 400 | Invalid request parameter |
| `VALIDATION_ERROR` | 400 | Request validation failed |
| `INTERNAL_ERROR` | 500 | Unexpected server error |

## Correlation IDs

Every API response includes a correlation ID in:
- Response header: `X-Correlation-ID`
- Error response body: `correlationId` field

Use correlation IDs when reporting issues for faster troubleshooting.

## Authorization Model

### Project-Based Access Control

Customers can only access site reports for projects they are assigned to:

1. **Regular Customers**: See reports for their assigned projects only
2. **Admin Customers**: See reports for all projects

### Authorization Flow

```
Request → JWT Validation → Service Layer → Authorization Check → Data Fetch
                                              ↓ (if unauthorized)
                                          403 Forbidden
```

## Performance Considerations

### Caching

- User project access is cached for 5 minutes
- Reduces database queries by ~60%
- Cache automatically invalidates after TTL

### Pagination

- Default page size: 20 reports
- Maximum page size: 100 reports (enforced at service layer)
- Use pagination for better performance with large datasets

### Database Indexes

Optimized queries use the following indexes:
- `idx_site_reports_project_date`: Project + date sorting
- `idx_site_reports_project_ids`: Multi-project filtering
- `idx_site_report_photos_report_id`: Photo loading

## Rate Limiting

*Future enhancement - not currently implemented*

## Example Usage

### cURL Example

```bash
# Get site reports with project filter
curl -X GET "https://api.example.com/api/customer/site-reports?projectId=1&page=0&size=20" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Get specific site report
curl -X GET "https://api.example.com/api/customer/site-reports/123" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### JavaScript Example

```javascript
const response = await fetch('/api/customer/site-reports?projectId=1', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
});

if (response.ok) {
  const data = await response.json();
  console.log('Reports:', data.data.content);
} else if (response.status === 403) {
  console.error('Access denied to project');
} else if (response.status === 404) {
  console.error('Project not found');
}
```

## Troubleshooting

### Common Issues

**Issue**: Getting 403 Forbidden
- **Cause**: User doesn't have access to the requested project
- **Solution**: Verify project assignment in customer portal

**Issue**: Getting 404 Not Found
- **Cause**: Report or project doesn't exist
- **Solution**: Verify the ID is correct

**Issue**: Reports not showing up
- **Cause**: No projects assigned to customer
- **Solution**: Contact administrator to assign projects

### Logging

For production issues, provide:
1. Correlation ID from error response
2. Timestamp of request
3. User email (for internal investigation)
4. Endpoint and parameters used

## Security Notes

### Data Privacy

- Customers can only see their own project data
- Authorization enforced at service layer (not just UI)
- All access attempts logged for audit trail

### Sensitive Data

The following data is **NOT** exposed to customers:
- Internal employee information
- Financial details beyond what's on invoices
- Other customers' data
- System configuration

## Version History

- **v2.0** (2026-02-13): Enhanced error handling, correlation IDs, caching
- **v1.0** (2025-xx-xx): Initial implementation
