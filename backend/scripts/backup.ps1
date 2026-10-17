<#
  Copia de seguridad del esquema `app` (ofertas y reservas) con pg_dump dentro de Docker: no hace falta instalar PostgreSQL.

  Uso:
    $env:BACKUP_DB_URL = "postgresql://postgres.<ref>:<contraseña>@aws-0-<región>.pooler.supabase.com:5432/postgres"
    ./scripts/backup.ps1

  BACKUP_DB_URL es la cadena del «Session pooler» del panel de Supabase (Connect). Lleva la contraseña: se pone solo como variable
  de entorno, nunca en un archivo del repositorio. Deja backend/backups/turismo-AAAA-MM-DD-HHmm.dump (formato personalizado de
  pg_dump; contiene correos, por eso `backups/` está en .gitignore). Se restaura con pg_restore (ver backend/README.md).
#>
param([string]$Url = $env:BACKUP_DB_URL)

if (-not $Url) { throw "Define BACKUP_DB_URL con la cadena de conexión (Session pooler) del proyecto." }

$dir = Join-Path $PSScriptRoot "..\backups"
New-Item -ItemType Directory -Force $dir | Out-Null
$dir = (Resolve-Path $dir).Path
$file = "turismo-" + (Get-Date -Format "yyyy-MM-dd-HHmm") + ".dump"

docker run --rm -v "${dir}:/backup" -e "PGURL=$Url" -e "FILE=$file" postgres:17-alpine sh -c 'pg_dump "$PGURL" --schema=app --format=custom --no-owner --file="/backup/$FILE"'
if ($LASTEXITCODE -ne 0) { throw "pg_dump falló (código $LASTEXITCODE)." }

$size = [math]::Round((Get-Item (Join-Path $dir $file)).Length / 1KB, 1)
"Copia guardada: backups/$file ($size KB)"
