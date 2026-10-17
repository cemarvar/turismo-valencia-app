# Backend de Turismo Valencia (Supabase)

La base de datos es un **PostgreSQL gestionado por Supabase** (esquema privado `app`) y la única puerta de entrada es una
**función de borde** (`api-v1`, TypeScript sobre Deno) que ofrece la API a la app. No hay servidor propio que mantener ni clave de
Supabase dentro del APK. La API está descrita en [`docs/api.md`](../docs/api.md).

```text
backend/
├─ package.json          la CLI de Supabase (fijada) y atajos: npm start, npm test, npm run reset…
├─ supabase/
│  ├─ config.toml        configuración local (servicios que no usamos, desactivados; api-v1 pública)
│  ├─ migrations/        esquema, funciones SQL y ofertas iniciales, versionados
│  ├─ seed.sql           datos SOLO para pruebas locales (una oferta con aforo pequeño)
│  └─ functions/         api-v1 (rutas, validación, ofertas, reservas) y _shared (HTTP, base de datos, límites)
├─ contract/examples/    JSON reales de la API: los usan las pruebas del servidor y las de la app
├─ tests/test_api.py     pruebas de la API por HTTP (Python, sin instalar nada)
└─ scripts/              copia de seguridad y borrado de los datos de un correo
```

## Cómo está hecho (y por qué)

- **Las reglas viven en la base de datos.** Validar la reserva, no duplicarla, contar plazas y dar forma al JSON lo hacen funciones SQL
  (`create_reservation`, `list_offers`…) dentro de una transacción; la función de borde solo traduce HTTP (rutas, cabeceras, formas
  del cuerpo, límites). Así dos reservas simultáneas no pueden pasarse del aforo, y un reintento no duplica nada.
- **La base de datos está cerrada.** Todo está en el esquema `app`, que la API automática de Supabase no expone; además cada tabla
  tiene la seguridad por filas activada y sin políticas, y los roles `anon` y `authenticated` no tienen ningún permiso. La función se
  conecta por su cuenta (`SUPABASE_DB_URL`). Una prueba lo comprueba con la clave pública.
- **Ninguna clave en el APK.** La función es pública (`verify_jwt = false`) y la app solo conoce su dirección.
- **Sin cuentas.** El móvil genera un UUID (`X-Device-Id`) y cada intento de reservar lleva otro (`Idempotency-Key`).
- **Límites de peticiones** por IP, dispositivo y correo (tabla de contadores con un resumen con sal, sin guardar IPs ni correos).
- **El precio y el localizador los pone el servidor**, nunca el cliente.

## Requisitos

Node 20 o superior (para la CLI), **Docker Desktop en marcha** (el entorno local son contenedores) y Python 3 (pruebas).

## Uso diario

```bash
cd backend
npm install      # una vez
npm start        # arranca Supabase en local (la primera vez descarga unos 3 GB de imágenes)
npm test         # las pruebas de la API (la oferta de pruebas solo existe en local)
npm run reset    # borra la base de datos local y la reconstruye con las migraciones y seed.sql
npm stop
```

En local la API está en `http://127.0.0.1:54321/functions/v1/api-v1`. Para mirar la base de datos:
`docker exec -it supabase_db_turismo-valencia psql -U postgres`.

### La app de depuración contra el backend local

Por defecto la app usa la API publicada. Para que use la local (la oferta de pruebas `prueba-aforo` solo existe ahí, así que se ve que el
catálogo es el del servidor) desde el emulador o un móvil por USB:

```bash
adb reverse tcp:54321 tcp:54321
./gradlew installDebug -PapiBaseUrl=http://127.0.0.1:54321/functions/v1/api-v1/
```

Con `adb reverse` la app llega al ordenador por `127.0.0.1`; la dirección `10.0.2.2` del emulador se queda sin respuesta por la red Wi-Fi
virtual. Solo la versión de depuración admite HTTP sin cifrar, y únicamente hacia el ordenador (`app/src/debug/res/xml`). Para volver a la
nube, `./gradlew installDebug` sin `-PapiBaseUrl`.

## Cambiar el esquema

1. `npx supabase migration new nombre` y escribir el SQL en el archivo nuevo.
2. `npm run reset` y `npm test`.
3. **Nunca se edita una migración ya publicada**: se añade otra. Para cambiar una función, una migración nueva con
   `create or replace function` (si cambia su firma, antes un `drop function`).

Las ofertas se cambian con una migración (`insert` o `update`) o con una consulta en el editor SQL del panel; los aforos
(`offer_slots.capacity`) de las ofertas iniciales son valores de demostración.

## Publicar en Supabase (la primera vez)

1. Crea el proyecto en <https://supabase.com/dashboard>: región de la Unión Europea (Frankfurt o París) y plan gratuito. Guarda la
   contraseña de la base de datos en un gestor de contraseñas: **no va en el repositorio ni se le pasa a nadie**.
2. En `backend/`: `npm run login` (abre el navegador; al autorizar enseña un código de 8 caracteres que se pega en el terminal) y
   `npm run link -- --project-ref <ref>`. La referencia son **20 letras minúsculas** y salen en la URL del panel
   (`supabase.com/dashboard/project/<ref>`) o en Project Settings → General; no es el código de 8 caracteres del inicio de sesión. Si
   pide la contraseña de la base de datos se puede dejar en blanco: la CLI se conecta con un rol temporal de tu sesión.
3. `npm run deploy`: aplica las migraciones (pregunta antes: responde `Y`) y publica la función, que se empaqueta en los servidores de
   Supabase (`--use-api`), así que no hace falta Docker.
4. Un secreto para el límite de peticiones: `npx supabase secrets set RATE_LIMIT_SALT=<texto largo y aleatorio>`.
5. Comprueba: `API_BASE=https://<ref>.supabase.co/functions/v1/api-v1 python tests/test_api.py` (solo lecturas; las reservas de prueba
   se desactivan en la nube salvo `API_WRITES=1`). Con `SUPABASE_ANON_KEY=<clave anon del proyecto>` también comprueba que la
   base de datos está cerrada.
6. Plan gratuito: crea la variable de repositorio `API_BASE` en GitHub (Settings → Secrets and variables → Actions → Variables) para
   que `.github/workflows/keepalive.yml` evite la pausa por inactividad.

La dirección base de la API no es un secreto: irá en la app (paso «App: reservas reales»).

**Despliegue actual:** proyecto `turismo-valencia` (región `eu-north-1`, UE), dirección base
`https://rttqdgrrprzgdxdlhaeq.supabase.co/functions/v1/api-v1`. La cuenta tiene además un proyecto antiguo y pausado
(`Turismo_Valencia`) que no se usa: `npx supabase projects list` muestra todos con su referencia. Tras la pasarela de Supabase el ETag
llega débil (`W/"…"`); funciona igual y las pruebas aceptan las dos formas.

## Mantenimiento

- **Copia de seguridad:** `./scripts/backup.ps1` (las instrucciones están en el propio script). Se restaura con
  `docker run --rm -v "<carpeta de backups>:/backup" postgres:17-alpine pg_restore --clean --if-exists --no-owner -d "<cadena>" /backup/<archivo>.dump`.
  El plan gratuito de Supabase no suele incluir copias automáticas (revisa las condiciones vigentes).
- **Borrar los datos de un correo** (derecho de supresión): `scripts/borrar_datos_de_un_correo.sql`, en el editor SQL del panel.
- **Rol de permisos mínimos (opcional):** la función puede conectarse con un rol que solo ejecute las funciones de `app`. Se crea el rol
  con contraseña en el editor SQL y se guarda su cadena como secreto `API_DB_URL`, que la función prefiere a `SUPABASE_DB_URL`.

## Límites conocidos

- Sin cuentas, el UUID del dispositivo es el «secreto» de sus reservas; si se pierde (reinstalar la app), se recuperan con el
  localizador y el correo.
- Los límites de peticiones son una defensa básica, suficiente para este proyecto; no sustituyen a un captcha ni a un cortafuegos.
- La API todavía no cancela ni modifica reservas (el estado `cancelled` existe en la base de datos).
- El pago está simulado.
