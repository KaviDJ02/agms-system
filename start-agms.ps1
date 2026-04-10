param(
    [string]$EnvFile = ".env",
    [int]$DelaySeconds = 8
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$envPath = Join-Path $repoRoot $EnvFile

if (-not (Test-Path $envPath)) {
    throw "Env file not found: $envPath"
}

Write-Host "Loading environment variables from $envPath" -ForegroundColor Cyan

Get-Content $envPath | ForEach-Object {
    $line = $_.Trim()
    if ([string]::IsNullOrWhiteSpace($line)) { return }
    if ($line.StartsWith("#")) { return }

    $parts = $line -split "=", 2
    if ($parts.Count -ne 2) { return }

    $name = $parts[0].Trim()
    $value = $parts[1].Trim().Trim('"')

    if (-not [string]::IsNullOrWhiteSpace($name)) {
        [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
}

$requiredVars = @(
    "JWT_SECRET",
    "IOT_USERNAME",
    "IOT_PASSWORD",
    "AGMS_INTERNAL_SUBJECT"
)

$missing = @()
foreach ($v in $requiredVars) {
    $current = [System.Environment]::GetEnvironmentVariable($v, "Process")
    if ([string]::IsNullOrWhiteSpace($current)) {
        $missing += $v
    }
}

if ($missing.Count -gt 0) {
    throw "Missing required .env variables: $($missing -join ', ')"
}

$services = @(
    @{ Name = "Config Server"; Path = "config-server" },
    @{ Name = "Eureka Server"; Path = "eureka-server" },
    @{ Name = "Zone Service"; Path = "zone-service" },
    @{ Name = "Automation Service"; Path = "automation-service" },
    @{ Name = "Sensor Service"; Path = "sensor-service" },
    @{ Name = "Crop Service"; Path = "crop-service" },
    @{ Name = "API Gateway"; Path = "api-gateway" }
)

$envInject = @(
    "`$env:JWT_SECRET = '$([System.Environment]::GetEnvironmentVariable("JWT_SECRET", "Process"))'",
    "`$env:IOT_USERNAME = '$([System.Environment]::GetEnvironmentVariable("IOT_USERNAME", "Process"))'",
    "`$env:IOT_PASSWORD = '$([System.Environment]::GetEnvironmentVariable("IOT_PASSWORD", "Process"))'",
    "`$env:AGMS_INTERNAL_SUBJECT = '$([System.Environment]::GetEnvironmentVariable("AGMS_INTERNAL_SUBJECT", "Process"))'"
) -join "; "

foreach ($svc in $services) {
    $serviceDir = Join-Path $repoRoot $svc.Path
    if (-not (Test-Path $serviceDir)) {
        Write-Warning "Skipping $($svc.Name): path not found ($serviceDir)"
        continue
    }

    $cmd = "$envInject; Set-Location '$serviceDir'; .\mvnw.cmd spring-boot:run"

    Write-Host "Starting $($svc.Name) in new terminal..." -ForegroundColor Green
    Start-Process powershell -ArgumentList @("-NoExit", "-Command", $cmd)

    Start-Sleep -Seconds $DelaySeconds
}

Write-Host "All launch commands sent. Check each service terminal for startup status." -ForegroundColor Yellow
