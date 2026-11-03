# Firma del APK de publicación

La app **no se publica** (ni Google Play ni App Bundle): se entrega como un **APK firmado** que se instala a mano. Un APK sin firmar no se puede instalar, así que la versión `release` (R8) se firma con una clave propia. La clave **nunca** va en el repositorio ni en el APK.

## Cómo está montado

- `app/build.gradle.kts` crea la firma `release` solo si encuentra la ruta del almacén y su contraseña; si no, `release` sale sin firmar (`app-release-unsigned.apk`) y ninguna otra compilación se ve afectada (`debug` y `releaseCheck` usan la clave de depuración).
- Los datos se leen de **variables de entorno** o de **`local.properties`** (ignorado por git), en este orden:

| Variable de entorno | Clave en `local.properties` | Qué es |
|---|---|---|
| `TURISMO_KEYSTORE_FILE` | `release.storeFile` | Ruta del `.jks` |
| `TURISMO_KEYSTORE_PASSWORD` | `release.storePassword` | Contraseña del almacén |
| `TURISMO_KEY_ALIAS` | `release.keyAlias` | Alias (por defecto `turismovalencia`) |
| `TURISMO_KEY_PASSWORD` | `release.keyPassword` | Contraseña de la clave (por defecto, la del almacén) |

En `local.properties` la ruta se escribe **con `/`** (`C:/Users/camvf/claves/turismo-valencia.jks`): la barra invertida es un carácter de escape en ese formato y estropea la ruta.

## Crear la clave (una sola vez, en el ordenador personal)

Se hace en una terminal propia, porque `keytool` pregunta las contraseñas y no deben pasar por ningún asistente ni por el historial del repositorio.

```powershell
New-Item -ItemType Directory -Force "$env:USERPROFILE\claves"
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair -v -keystore "$env:USERPROFILE\claves\turismo-valencia.jks" -alias turismovalencia -keyalg RSA -keysize 2048 -validity 10000
```

- Pide una contraseña del almacén (mínimo 6 caracteres) y los datos del certificado (nombre, organización, país: valen los que quieras; solo se ven al inspeccionar el certificado).
- Para la clave, pulsa Intro para usar la misma contraseña: el formato PKCS12 de los JDK modernos no admite dos contraseñas distintas, así que una sola evita confusiones.
- `-validity 10000` son unos 27 años.

## Dónde guardarla

- **El `.jks`, dos copias**, fuera del proyecto: el gestor de contraseñas (si admite adjuntos) y un USB o un almacenamiento privado cifrado.
- **La contraseña, el alias y el nombre del archivo** en el gestor de contraseñas. Sin el archivo o sin la contraseña no se puede recuperar la clave.
- **Nunca** en git, en el APK, en `strings.xml`, en `BuildConfig` ni en un ordenador compartido (el de clase). El APK ya firmado se puede llevar a cualquier sitio: no contiene la clave.
- Si hay que compilar en otro equipo, se copia el `.jks` a un USB, se usa y se borra la copia al terminar.
- Perder la clave no es grave mientras la app no se publique: se crea otra y se firma de nuevo. Solo impide actualizar encima de una instalación firmada con la clave anterior (hay que desinstalarla antes).

## Compilar y comprobar el APK firmado

Con las variables de entorno solo para esa sesión (así no se guarda ninguna contraseña en disco):

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:TURISMO_KEYSTORE_FILE = "$env:USERPROFILE\claves\turismo-valencia.jks"
$env:TURISMO_KEYSTORE_PASSWORD = Read-Host "Contraseña del almacén"
.\gradlew.bat assembleRelease
```

O, para no repetirlo, en `local.properties` (que git ignora):

```properties
release.storeFile=C:/Users/camvf/claves/turismo-valencia.jks
release.storePassword=…
```

El APK sale en `app/build/outputs/apk/release/app-release.apk`. Se comprueba con `apksigner` (viene con las build-tools del SDK):

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\apksigner.bat" verify --verbose --print-certs app\build\outputs\apk\release\app-release.apk
```

Debe decir `Verifies`, `Verified using v2 scheme … true` y mostrar el certificado propio. Con `minSdk` 24, Android Gradle Plugin firma con v2 y no con v1 (que solo hace falta por debajo de la API 24).

Para instalarlo en un móvil: `adb install -r app\build\outputs\apk\release\app-release.apk` (con depuración USB) o copiar el archivo al móvil y abrirlo (hay que permitir «instalar apps desconocidas»). Si el móvil ya tiene la versión de depuración, hay que desinstalarla antes: las dos firmas son distintas.

La versión `release` apunta a la nube de Supabase (la dirección pública del backend) y, a diferencia de `releaseCheck`, no admite HTTP sin cifrar: las reservas que se hagan con ella son reales (se cancelan desde la app).
