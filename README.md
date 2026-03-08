# Customer API

REST API for customer-facing features including authentication, project management, payments, and site reports.

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+
- Access to the database server

## Setup

### 1. Environment Configuration

The application uses **Spring Profiles** to manage environments (`local` and `production`) alongside strict environment variables.

Configuration files:
- `application.yml` (Common configurations)
- `application-local.yml` (Developer defaults)
- `application-production.yml` (Production - requires actual system environment variables)

To switch environments, set the `SPRING_PROFILES_ACTIVE` environment variable (e.g., `local` or `production`). 

**For Production:**
All sensitive data must be passed strictly as system environment variables:
```bash
# Database Configuration
DB_URL=jdbc:postgresql://your-host:5432/your_database
DB_USERNAME=your_username
DB_PASSWORD=your_secure_password

# JWT Configuration - Generate with: openssl rand -hex 32
JWT_SECRET=your_generated_secret_key
```

### 2. Database Setup

Create the database:

```sql
CREATE DATABASE your_database_name;
```

### 3. Build and Run

#### Local Development
By default, the application runs using the `local` profile.

```bash
# Build the project
mvn clean install -DskipTests

# Run the application
mvn spring-boot:run
```

#### Production Deployment

For deploying to a VPS (e.g., using `pm2` to keep it running):

```bash
# Set up the production profile and start the app via pm2
SPRING_PROFILES_ACTIVE=production pm2 start "java -jar target/cust-api-0.0.1-SNAPSHOT.jar" --name "wd-customer-api"

# Save the pm2 state so it restarts on server reboot
pm2 save
pm2 startup
```

The API will start on `http://localhost:8081` (or the port specified in `SERVER_PORT`).

## Configuration

### Production Settings

The `application-production.yml` file is automatically optimized for production:
- Disables SQL logging
- Uses `validate` for DDL generation to prevent accidental database overwrites
- Hides stack traces and explicit error messages
- Logs only at `INFO` level

### Security Best Practices

1. **Rotate secrets regularly** - See `SECURITY.md` for instructions
2. **Use strong passwords** - Minimum 12 characters with mixed case, numbers, and symbols
3. **Generate JWT secrets properly** - Use `openssl rand -hex 32` or similar
4. **Pass configuration via Environment Variables** - Never hardcode production secrets in source control.

## API Documentation

Once running, access the API at:
- Base URL: `http://localhost:8081`
- Health Check: `http://localhost:8081/actuator/health`

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
2. Check `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` variables
3. Ensure database exists
4. Check network connectivity to database server

### JWT Secret Issues

If you see JWT validation errors starting up in production:
1. Ensure `JWT_SECRET` variable is provided
2. Generate a new secret: `openssl rand -hex 32`
3. Restart the application

## Development

### Code Style

- Use explicit imports (no wildcards)
- Use SLF4J logger (no System.out.println)
- Always log exceptions with context
- Follow Spring Boot best practices

## Security

See `SECURITY.md` for security guidelines and incident response procedures.
