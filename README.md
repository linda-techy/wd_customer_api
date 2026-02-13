# Customer API

REST API for customer-facing features including authentication, project management, payments, and site reports.

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+
- Access to the database server

## Setup

### 1. Environment Configuration

Copy the example environment file and configure with your values:

```bash
cp .env.example .env
```

Edit `.env` and set your actual values:

```properties
# Database Configuration
DB_URL=jdbc:postgresql://your-host:5432/your_database
DB_USERNAME=your_username
DB_PASSWORD=your_secure_password

# JWT Configuration - Generate with: openssl rand -hex 32
JWT_SECRET=your_generated_secret_key
JWT_ACCESS_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000

# File Storage Path
STORAGE_BASE_PATH=/path/to/storage
```

### 2. Database Setup

Create the database:

```sql
CREATE DATABASE your_database_name;
```

### 3. Build and Run

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The API will start on `http://localhost:8080` (or the port specified in `SERVER_PORT`).

## Configuration

### Production Settings

For production deployment, set these environment variables:

```bash
DDL_AUTO=validate                # Never use 'update' in production
SHOW_SQL=false                   # Disable SQL logging
ERROR_INCLUDE_MESSAGE=never      # Hide internal error details
LOGGING_LEVEL_SECURITY=INFO      # Use INFO level logging
LOGGING_LEVEL_APP=INFO           # Use INFO level logging
```

### Security Best Practices

1. **Never commit `.env` files** - They contain sensitive credentials
2. **Rotate secrets regularly** - See `SECURITY.md` for instructions
3. **Use strong passwords** - Minimum 12 characters with mixed case, numbers, and symbols
4. **Generate JWT secrets properly** - Use `openssl rand -hex 32` or similar
5. **Use environment-specific configs** - Different secrets for dev/staging/production

## API Documentation

Once running, access the API at:
- Base URL: `http://localhost:8080`
- Health Check: `http://localhost:8080/actuator/health`

### Key Endpoints

- `POST /auth/login` - User authentication
- `POST /auth/register` - User registration
- `POST /auth/forgot-password` - Request password reset
- `POST /auth/reset-password` - Reset password with code
- `POST /auth/refresh-token` - Refresh access token
- `GET /auth/me` - Get current user info

## Troubleshooting

### Database Connection Issues

If you see connection errors:
1. Verify database is running
2. Check `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in `.env`
3. Ensure database exists
4. Check network connectivity to database server

### JWT Secret Issues

If you see JWT validation errors:
1. Ensure `JWT_SECRET` is at least 32 characters
2. Generate a new secret: `openssl rand -hex 32`
3. Restart the application after changing the secret

## Development

### Code Style

- Use explicit imports (no wildcards)
- Use SLF4J logger (no System.out.println)
- Always log exceptions with context
- Follow Spring Boot best practices

## Security

See `SECURITY.md` for security guidelines and incident response procedures.
