# API de Turismo Valencia (v1)

La app habla con una única API pública: una función de borde de Supabase (`backend/supabase/functions/api-v1`) que es la
**única puerta** a la base de datos (PostgreSQL, esquema privado `app`). La app no lleva ninguna clave de Supabase en el APK,
solo la dirección.

- **Dirección base:** `https://<proyecto>.supabase.co/functions/v1/api-v1`; la publicada es
  `https://rttqdgrrprzgdxdlhaeq.supabase.co/functions/v1/api-v1` (pública, no es un secreto) y en local,
  `http://127.0.0.1:54321/functions/v1/api-v1`.
- **Formato:** JSON en UTF-8. Las fechas de actividad son `AAAA-MM-DD` (hora de Valencia), los instantes (`createdAt`) son ISO-8601
  en UTC, y el dinero va **en céntimos** (enteros).
- **Versión:** `api-v1`. Añadir campos no cambia la versión (la app debe ignorar los campos que no conozca); un cambio incompatible
  sería una función nueva (`api-v2`) desplegada a la vez, para que las apps ya instaladas sigan funcionando.
- **Sin cuentas:** cada móvil genera un UUID aleatorio la primera vez (lo guarda en la app, no en el código) y lo manda en
  `X-Device-Id`; con él se listan sus reservas. Es el «secreto» del móvil: quien lo conozca puede ver esas reservas. Para recuperar
  una reserva en otro móvil sirve el localizador junto con el correo.
- **Ejemplos reales:** `backend/contract/examples/*.json`. Los usan las pruebas del servidor y las de la app; si la forma de la API
  cambia, las pruebas fallan hasta que se actualicen.

## Errores

Todos tienen la misma forma. La app decide qué texto enseñar según `code`; `message` es solo una pista para quien depura.

```json
{ "error": { "code": "sold_out", "message": "Ya no quedan plazas para ese día y horario." } }
```

| HTTP | `code` | Cuándo |
|---|---|---|
| 400 | `invalid_request` | Falta una cabecera, el cuerpo no es JSON o un campo tiene un tipo o formato incorrecto (el mensaje dice cuáles) |
| 404 | `offer_not_found` | La oferta no existe o ya no está disponible |
| 404 | `reservation_not_found` | No hay ninguna reserva con ese localizador y ese correo (igual si el localizador existe con otro correo) |
| 404 / 405 | `not_found` / `method_not_allowed` | Ruta o método que no existen (con `Allow`) |
| 409 | `sold_out` | No quedan plazas para ese día y horario |
| 409 | `idempotency_key_reused` | La clave de idempotencia ya se usó con otros datos |
| 413 | `payload_too_large` | El cuerpo pasa de 4 KB |
| 422 | `invalid_slot` | La oferta no tiene ese horario |
| 422 | `invalid_date` | Fecha pasada o a más de un año; o, si es hoy, un horario que ya pasó |
| 422 | `invalid_party` | Menos de 1 adulto o más de 20 personas entre adultos y niños |
| 422 | `invalid_email` | El correo no es válido |
| 429 | `rate_limited` | Demasiadas peticiones; `Retry-After` dice cuántos segundos esperar |
| 500 | `internal_error` | Fallo del servidor (sin detalles) |

## Ofertas

### `GET /offers`

Catálogo de entradas, tours y experiencias, en el orden en que se enseñan. Lleva `ETag` (la versión del catálogo) y
`Cache-Control: public, max-age=60`; con `If-None-Match` igual responde **304 sin cuerpo**, así la app solo descarga lo que cambió. En la nube la pasarela de Supabase
puede entregar el `ETag` como débil (`W/"…"`): el cliente debe devolverlo tal como lo recibió.

```json
{ "offers": [ { "id": "albufera-barca", "kind": "experiencia", "tag": "Excursión", "title": "Paseo en barca por la Albufera",
                "duration": "1 h", "place": "El Palmar", "extra": "español/inglés", "priceFromCents": 1800,
                "description": "Recorre el lago…", "placeId": "albufera-barca", "slots": ["16:30", "17:30", "18:30", "19:30"] } ] }
```

| Campo | Tipo | Significado |
|---|---|---|
| `id` | texto | Identificador estable (el mismo que usa la app para sus fotos) |
| `kind` | `entrada` \| `tour` \| `experiencia` | Tipo |
| `tag`, `title`, `duration`, `place`, `extra`, `description` | texto | Lo que se enseña |
| `priceFromCents` | entero | Precio por adulto en céntimos; los niños no pagan |
| `placeId` | texto o `null` | Ficha del lugar en la app donde empieza la actividad («Cómo llegar») |
| `slots` | lista de texto | Horarios (`HH:MM`) o `Cualquier hora` |

### `GET /offers/{id}`

Una oferta con el mismo formato (404 `offer_not_found` si no existe).

## Reservas

### `POST /reservations`

Crea una reserva. Cabeceras obligatorias: `X-Device-Id` (UUID del móvil) e `Idempotency-Key` (UUID de **este intento**: se genera
al pulsar «Confirmar reserva» y se reutiliza si hay que reintentar; así, si la red se corta antes de recibir la respuesta, no se
duplica la reserva).

```json
{ "offerId": "albufera-barca", "date": "2027-02-11", "time": "18:30", "adults": 2, "children": 1, "email": "laura.garcia@email.com" }
```

- **201** con la reserva. Si la clave ya se había usado con los mismos datos: **200** con la misma reserva y la cabecera
  `Idempotent-Replayed: true`.
- El servidor pone el precio (`totalCents` = precio por adulto × adultos) y el localizador `TV-<año de la actividad>-<número>`
  (el número es correlativo desde el 1000). El correo se guarda en minúsculas.
- Todo ocurre en una transacción: se bloquea el horario y se cuentan las plazas ocupadas (los niños ocupan plaza), así que dos
  reservas simultáneas no pueden pasarse del aforo.

```json
{ "id": "12b229f7-…", "locator": "TV-2027-1100", "offerId": "albufera-barca", "title": "Paseo en barca por la Albufera",
  "place": "El Palmar", "date": "2027-02-11", "time": "18:30", "adults": 2, "children": 1, "email": "laura.garcia@email.com",
  "totalCents": 3600, "status": "confirmed", "createdAt": "2026-10-14T17:04:06Z" }
```

`status` es `confirmed` o `cancelled`. «Completada» no existe en el servidor: la app la deduce cuando la fecha ya pasó.
El pago está simulado: no se piden ni guardan datos de tarjeta.

### `GET /reservations`

Las reservas del dispositivo (`X-Device-Id`), las más recientes primero, hasta 100: `{ "reservations": [ … ] }`.

### `GET /reservations/{locator}?email=…`

Recupera una reserva con su localizador y el correo con el que se hizo (sin distinguir mayúsculas). Si el correo no coincide,
responde como si no existiera, para que nadie pueda averiguar qué localizadores hay.

## Límites

| Qué | Límite | Ventana |
|---|---|---|
| Cualquier petición, por IP | 240 | 1 minuto |
| Crear reservas, por IP | 30 | 1 hora |
| Crear reservas, por dispositivo | 20 | 1 hora |
| Crear reservas, por correo | 10 | 1 día |
| Listar reservas, por dispositivo | 120 | 1 hora |
| Recuperar por localizador, por IP | 20 | 1 hora |

Al pasarse, **429** con `Retry-After`. Los contadores no guardan IPs ni correos, solo un resumen con sal.

## Cómo debe usarla la app

1. Generar el UUID del dispositivo la primera vez y guardarlo en Room (ajustes), nunca en el código (**hecho**, ver «Reservas»).
2. Al confirmar una reserva, generar una `Idempotency-Key` y guardarla hasta recibir respuesta; reintentar **con la misma clave** solo
   ante errores de red o 5xx. Si hay respuesta (200/201 o un 4xx), se acabó ese intento (**hecho**).
3. Reservar exige conexión (el servidor confirma y asigna el localizador); ver las reservas ya guardadas, no (**hecho**).
4. Cachear el catálogo con el `ETag` y usar el catálogo incluido en la app como respaldo si no hay red (**hecho**, ver abajo).
5. Enseñar los mensajes según `code`, no según `message` (**hecho**).

### Catálogo de ofertas: lo que hace hoy la app

El cliente es Retrofit con kotlinx.serialization (`data/remote/TurismoApi.kt`; la dirección sale de `BuildConfig.API_BASE_URL`) y
`OffersRepository` (`data/repo`) es el único que lo usa:

- Pide `GET /offers` al abrir la app y cada vez que vuelve la conexión, con `If-None-Match` igual al `ETag` guardado, tal como llegó
  (la pasarela de Supabase lo manda débil). **304**: la caché sigue vigente. **200**: sustituye el catálogo guardado (tablas `offers` y
  `cache_meta` de Room, en una transacción) y apunta el nuevo `ETag`. Cualquier fallo (sin red, 4xx/5xx, JSON que no es el del contrato)
  deja lo que ya había.
- Se ignoran las ofertas que la app no entiende (un `kind` desconocido, sin `id` o título, o con precio negativo) y las repetidas; un
  catálogo vacío no sustituye al guardado (sería un error del servidor, no que no haya ofertas).
- Mientras no haya recibido ningún catálogo (primer arranque sin conexión) se usa el incluido en la app (`FakeOfertas`), que tiene las
  mismas 16 ofertas; una prueba comprueba que coincide con los ejemplos del contrato.
- Los datos son los del servidor; las fotos del diseño (`image`, `thumb`, `ticket`) siguen en la app y se asocian por `id`: una oferta
  nueva del servidor se pinta con el hueco de foto de su sección.
- El dinero va en céntimos también en el modelo de la app (`Offer.priceFromCents`, `Reservation.totalCents`); `formatPrice(cents)`
  lo enseña («18 €», «12,50 €»).

### Reservas: lo que hace hoy la app

El cliente es el mismo (`TurismoApi`) y `ReservationsRepository` (`data/repo`) es el único que lo usa:

- **Dispositivo.** El UUID se genera la primera vez que se reserva y se guarda en Room (`settings.deviceId`, de forma atómica: nunca
  hay dos). Un móvil que nunca ha reservado no tiene identificador: poner las reservas al día no lo crea ni manda nada al servidor. Va en `X-Device-Id`. Si el usuario borra los datos de la app o la
  desinstala, el móvil pierde el UUID y deja de poder listar sus reservas (siguen existiendo en el servidor, con su localizador).
- **Reservar exige conexión.** Sin red, la pantalla muestra «Reservar necesita conexión a Internet» y el botón queda desactivado;
  ver las reservas guardadas no necesita red. Pulsar «Confirmar reserva» manda `POST /reservations` y el botón dice «Reservando…»
  (un segundo toque mientras tanto no hace nada). El total que se enseña es una vista previa: el que vale es el del servidor.
- **Clave de idempotencia por intento.** Antes de enviar se guarda el intento (tabla `reservation_attempts`: clave y datos, con un
  índice único por datos): pedir lo mismo otra vez reutiliza la clave y cambiar cualquier dato crea una nueva. Con una respuesta
  definitiva (2xx o 4xx salvo 429) el intento se borra; sin red, con un 5xx o un 429 se conserva y el reintento manda **la misma
  clave**, así que una reserva que el servidor sí guardó (cuya respuesta se perdió) no se duplica. Los intentos de más de 24 h se
  olvidan al poner las reservas al día.
- **Lo que se guarda.** La reserva que contesta el servidor (`id` = UUID del servidor, `locator` = su localizador, total en
  céntimos) y, solo entonces, se abre la confirmación. Las tres reservas de demostración (`seed-*`) siguen en el móvil junto a las
  del servidor. «Completada» se deduce de la fecha; un estado que la app no conoce se enseña como «Pendiente», nunca como
  confirmado. El servidor no envía correos: la confirmación dice «Reserva registrada con el correo …» y el formulario avisa de que
  el correo se guarda en el servidor para gestionar la reserva.
- **Poner al día.** `GET /reservations` (solo si el móvil ya tiene identificador) al abrir la app, cada vez que vuelve la conexión y
  cada noche en segundo plano (WorkManager, a partir de las 3:00; solo con Wi-Fi si «Descargar solo con Wi-Fi» está activado y si
  «Actualizar automáticamente» lo está; el catálogo `GET /offers` va en la misma pasada): lo que el servidor diga manda (una reserva
  cancelada sale en «Canceladas» con su estado). Un fallo se anota en el registro y la app sigue con lo guardado.
- **Mensajes por `code`** (nunca el `message` del servidor): `sold_out` «Ya no quedan plazas para ese día y horario», `offer_not_found`
  «Esta actividad ya no está disponible», `invalid_date`, `invalid_slot`, `invalid_party` y `invalid_email` (cada uno con su texto),
  `rate_limited` (con `Retry-After`: «Vuelve a probar en N min»), cualquier 5xx «El servicio de reservas no está disponible ahora»,
  sin red o respuesta ilegible «No hemos podido conectar con el servidor» y cualquier otro rechazo (`invalid_request`,
  `idempotency_key_reused`, `payload_too_large`…) «No se ha podido completar la reserva». El aviso desaparece al cambiar un dato.
- **Todavía no.** La app no usa `GET /reservations/{localizador}?email=` (recuperar una reserva en otro móvil) ni cancela reservas
  (la API tampoco: `cancelled` solo lo pone quien administra la base de datos).

## Datos personales

Se guarda, por reserva, el correo, las personas, la fecha y el horario. No hay cuentas, contraseñas ni datos de pago. Se pueden
borrar los datos de un correo a petición (ver `backend/README.md`, «Mantenimiento»).
