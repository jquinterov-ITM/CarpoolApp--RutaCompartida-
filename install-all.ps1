# Script para instalar APK en todos los emuladores conectados

$apkPath = "app\build\outputs\apk\debug\app-debug.apk"
$maxRetries = 5
$retryCount = 0

Write-Host "Esperando dispositivos..." -ForegroundColor Cyan

do {
    $devices = adb devices | Select-String "device$" | ForEach-Object { $_.Line.Split()[0] }
    
    if ($devices.Count -eq 0) {
        $retryCount++
        if ($retryCount -ge $maxRetries) {
            Write-Host "No hay dispositivos después de $maxRetries intentos." -ForegroundColor Red
            exit 1
        }
        Write-Host "Intento $retryCount/$maxRetries - Esperando dispositivos..." -ForegroundColor Yellow
        Start-Sleep -Seconds 5
    }
} while ($devices.Count -eq 0)

Write-Host "Encontrados $($devices.Count) dispositivo(s):" -ForegroundColor Green
$devices | ForEach-Object { Write-Host "  - $_" }

Write-Host "`nInstalando APK en todos los dispositivos..." -ForegroundColor Cyan

foreach ($device in $devices) {
    Write-Host "`n[$device] Instalando..." -ForegroundColor Yellow
    adb -s $device install -r $apkPath
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "[$device] Instalado exitosamente" -ForegroundColor Green
    } else {
        Write-Host "[$device] Error al instalar" -ForegroundColor Red
    }
}

Write-Host "`nInstalación completada." -ForegroundColor Cyan
