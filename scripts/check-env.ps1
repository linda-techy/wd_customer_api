param(
    [ValidateSet("local", "staging", "production")]
    [string]$Environment = "local",
    [switch]$AllowPlaceholderValues
)

$ErrorActionPreference = "Stop"

function Load-EnvFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FilePath
    )

    if (-not (Test-Path $FilePath)) {
        throw "Environment file not found: $FilePath"
    }

    Get-Content -Path $FilePath | ForEach-Object {
        $line = $_.Trim()
        if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("#")) {
            return
        }

        $parts = $line -split "=", 2
        if ($parts.Count -ne 2) {
            return
        }

        $name = $parts[0].Trim()
        $value = $parts[1].Trim().Trim("'`"")
        if (-not [string]::IsNullOrWhiteSpace($name)) {
            Set-Item -Path "Env:$name" -Value $value
        }
    }
}

function Is-PlaceholderValue {
    param(
        [string]$Value
    )

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return $true
    }

    $normalized = $Value.Trim().ToLowerInvariant()
    return $normalized.StartsWith("your_") `
        -or $normalized.StartsWith("replace_with") `
        -or $normalized.Contains("changeme") `
        -or $normalized.Contains("<") `
        -or $normalized.Contains("placeholder")
}

function Assert-EnvValue {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name,
        [switch]$AllowPlaceholder
    )

    $value = [Environment]::GetEnvironmentVariable($Name, "Process")
    if ([string]::IsNullOrWhiteSpace($value)) {
        throw "Missing required env var: $Name"
    }
    if (-not $AllowPlaceholder -and (Is-PlaceholderValue -Value $value)) {
        throw "Env var '$Name' contains placeholder text. Replace it with a real value."
    }
}

function Resolve-EnvFilePath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RepoRoot,
        [Parameter(Mandatory = $true)]
        [string]$EnvironmentName
    )

    if ($EnvironmentName -eq "local") {
        return (Join-Path $RepoRoot ".env")
    }

    return (Join-Path $RepoRoot ".env.$EnvironmentName")
}

$repoRoot = Split-Path -Path $PSScriptRoot -Parent
$envFile = Resolve-EnvFilePath -RepoRoot $repoRoot -EnvironmentName $Environment

Write-Host "Validating environment file: $envFile"
Load-EnvFile -FilePath $envFile

$requiredVars = @(
    "DB_URL",
    "DB_USERNAME",
    "DB_PASSWORD",
    "JWT_SECRET",
    "STORAGE_BASE_PATH",
    "SERVER_PORT",
    "CUSTOMER_PORTAL_BASE_URL",
    "cors.allowed-origins"
)

foreach ($name in $requiredVars) {
    Assert-EnvValue -Name $name -AllowPlaceholder:$AllowPlaceholderValues
}

if (-not $AllowPlaceholderValues -and $env:JWT_SECRET.Length -lt 32) {
    throw "JWT_SECRET must be at least 32 characters."
}

if (-not $env:CUSTOMER_PORTAL_BASE_URL.StartsWith("http")) {
    throw "CUSTOMER_PORTAL_BASE_URL must start with http/https."
}

$emailEnabledRaw = [Environment]::GetEnvironmentVariable("EMAIL_ENABLED", "Process")
if ([string]::IsNullOrWhiteSpace($emailEnabledRaw)) {
    $emailEnabledRaw = "false"
}
$emailEnabled = $emailEnabledRaw.ToLowerInvariant()
if ($emailEnabled -eq "true") {
    $smtpVars = @(
        "MAIL_HOST",
        "MAIL_PORT",
        "MAIL_USERNAME",
        "MAIL_PASSWORD",
        "MAIL_SMTP_AUTH"
    )
    foreach ($name in $smtpVars) {
        Assert-EnvValue -Name $name -AllowPlaceholder:$AllowPlaceholderValues
    }
}

Write-Host "Environment validation passed for: $Environment"
