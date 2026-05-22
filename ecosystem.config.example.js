// ─────────────────────────────────────────────────────────────────────────────
// PM2 launch template for the Customer API (wd-cust-api).
//
// Usage on the server:
//   cp ecosystem.config.example.js ecosystem.config.js
//   # edit ecosystem.config.js and fill in the REAL secret values
//   chmod 600 ecosystem.config.js
//   pm2 delete walldot-customer-api ; pm2 start ecosystem.config.js ; pm2 save
//
// `ecosystem.config.js` is gitignored — NEVER commit real secrets. This *.example
// file (placeholders only) is the tracked reference.
//
// Why a config file instead of `pm2 start "java ..." --name "..."`:
//   That inline form is fragile — a misplaced quote makes PM2's `--name` flag leak
//   into Java's arguments, so Spring parses `server.port="8081 --name ..."` and
//   crashes ("Failed to convert to type java.lang.Integer"). Here `name` is a PM2
//   field and `args` is a clean Java arg string, so there is nothing to mis-quote.
//
// IMPORTANT: this runs the "production" profile. The default "local" profile uses a
// dev JWT secret, DEBUG logging and localhost CORS — never run it on the server.
// ─────────────────────────────────────────────────────────────────────────────
module.exports = {
  apps: [
    {
      name: "walldot-customer-api",
      script: "java",
      interpreter: "none",
      cwd: "/home/ftpuser/var/www/app/walldotbuilders/wd-cust-api",
      args: [
        "-server",
        "-Xms256m", "-Xmx512m",
        "-XX:+UseSerialGC",
        "-XX:MaxMetaspaceSize=192m",
        "-XX:+ExitOnOutOfMemoryError",
        "-Dspring.profiles.active=production",
        "-jar", "/home/ftpuser/var/www/app/walldotbuilders/wd-cust-api/wd-cust-api.jar",
        "--server.port=8081"
      ].join(" "),
      autorestart: true,
      max_restarts: 10,
      env: {
        // Fill with REAL values on the server (the production YAML has dev-ish
        // defaults, so set these explicitly for a real deployment).
        DB_URL:        "jdbc:postgresql://HOST:5432/DBNAME",
        DB_USERNAME:   "postgres",
        DB_PASSWORD:   "REPLACE_ME",
        // JWT_SECRET MUST be identical across the customer API, portal API, and the
        // Next.js website, or cross-app token verification fails. Generate once:
        //   openssl rand -hex 32
        JWT_SECRET:    "REPLACE_ME_SAME_AS_PORTAL_AND_WEBSITE",
        MAIL_PASSWORD: "REPLACE_ME"
      }
    }
  ]
};
