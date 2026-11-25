# Arranca la API en Windows PowerShell con las variables de backend\.env.
# Uso (dentro de la carpeta backend):  .\correr.ps1
# Si Windows no deja ejecutar scripts:  powershell -ExecutionPolicy Bypass -File .\correr.ps1
#
# Lee cada línea NOMBRE=valor del .env (ignora comentarios), quita las comillas simples que rodean
# la URL de Neon y el JSON de Firebase, y las carga solo para este proceso. El .env nunca se sube.

$archivo = Join-Path $PSScriptRoot ".env"
if (-not (Test-Path $archivo)) {
    Write-Error "No existe backend\.env. Cópialo de .env.example y completa los valores."
    exit 1
}

Get-Content $archivo | Where-Object { $_ -match '^\s*[A-Za-z_][A-Za-z0-9_]*=' } | ForEach-Object {
    $nombre, $valor = $_ -split '=', 2
    $valor = $valor.Trim()
    if ($valor.Length -ge 2 -and $valor.StartsWith("'") -and $valor.EndsWith("'")) {
        $valor = $valor.Substring(1, $valor.Length - 2)
    }
    [Environment]::SetEnvironmentVariable($nombre.Trim(), $valor, 'Process')
}

Write-Host "Variables cargadas. Arrancando la API en el puerto $env:PORT..."
Set-Location $PSScriptRoot
.\gradlew.bat run
