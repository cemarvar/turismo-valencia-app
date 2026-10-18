# Turismo Valencia · app Android

App nativa para turistas en Valencia, derivada de la web [Turismo Valencia](https://turismovalenciaa.netlify.app)
([repositorio de la web](https://github.com/cemarvar/TurismoValencia)). Proyecto del TFG de DAM.

Qué ver, eventos, gastronomía y alojamientos; mapa y rutas a pie; tickets, tours y reservas con entrada QR;
un asistente (ValBot) y contenido guardado para usar **sin conexión**. Hecha con Kotlin y Jetpack Compose,
siguiendo el diseño de Figma pantalla a pantalla.

> **Estado:** frontend completo y funcional; ValBot conectado a la web, lo del usuario guardado en local con Room, mapa real de
> OpenStreetMap con ubicación y rutas, avisos de eventos con notificaciones reales, icono propio y pantalla de arranque del sistema.
> El backend propio (Supabase) está publicado y probado, y la app ya lo usa: lee de él el catálogo de ofertas y hace las reservas
> reales (reservar exige conexión) y las pone al día cada noche en segundo plano. Está en cuatro idiomas (español, valenciano, inglés y
> francés; el texto de las páginas de la web sigue solo en español, ver [`docs/idiomas.md`](docs/idiomas.md)). Aún no hay mapa descargable por
> zonas (ver [Hoja de ruta](#hoja-de-ruta)).

## Qué incluye

| Sección | Pantallas |
|---|---|
| **Bienvenida** | Splash, onboarding de 3 páginas, permiso de ubicación, selector de idioma (ES · VA · EN · FR) |
| **Explorar** | Portada con carrusel de destacados y chips de sección, búsqueda con sugerencias, listados por sección, fichas de lugar y de evento (con el aviso de la mascletà 30 min antes), Tickets & Tours, turismo sostenible |
| **Contenido de la web** | Las 65 páginas de la web: Lo esencial, Planes, Qué ver, Eventos, Gastronomía, Alojamientos, Sostenible, guías de entradas, tours y experiencias, y los textos legales; más Contacto. Los índices de sección siguen el orden de la web (15 lugares, 6 eventos, 9 platos, 6 alojamientos) |
| **Mapa** | Mapa de OpenStreetMap con filtros por categoría, tu ubicación (con permiso), y ruta real a pie o en bici con los pasos por las calles (autobús y metro, estimados) |
| **ValBot** | Asistente con IA (Gemini, a través de la web) con tarjetas de lugares y guías, dictado por voz, respuestas básicas si el servidor no responde y modo sin conexión |
| **Favoritos y descargas** | Favoritos, fichas y mapa guardados, cuota de espacio, solo Wi-Fi y actualización automática cada noche |
| **Reservas** | Reservar en dos pasos (la confirma el servidor, con aforo por día y horario), reserva confirmada con código QR, historial y reservas canceladas |
| **Perfil** | Idioma, notificaciones (con el permiso del sistema), ubicación, tamaño del texto, cerrar sesión |
| **Estados** | Sin conexión, cargando, favoritos vacío, búsqueda sin resultados |

Las 27 pantallas del [diseño de Figma](docs/Figma) están implementadas y comparadas con las capturas en el emulador.

## Stack

- **Kotlin 2.2** y **Jetpack Compose** (BOM 2026.02.01) con **Material 3**
- **Arquitectura MVVM**: una pantalla = `Screen` + `UiState` + `ViewModel`, con `Route` que los conecta
- **Navigation Compose** (barra inferior solo en las rutas que la llevan en el diseño)
- Tipografía **Source Sans 3** empaquetada, para que la app funcione sin conexión
- **osmdroid** (mapa de OpenStreetMap), **Room** (base de datos local con lo del usuario), **OkHttp** (ValBot y rutas), **Retrofit** y **kotlinx.serialization** (la API propia del backend), **WorkManager** (el refresco nocturno en segundo plano), **ZXing** (código QR real), **AlarmManager** y notificaciones locales (avisos de eventos), **SplashScreen API** y `desugar_jdk_libs` (`java.time` desde Android 7)
- Android: `minSdk 24`, `targetSdk 37`, solo tema claro

Datos: el contenido de la web como JSON en `assets/web/` (con 479 fotos en `assets/img/`) y lo del usuario (favoritos, descargas,
reservas, ajustes…) en una base de datos Room que sigue ahí al cerrar la app. Usa Internet para ValBot, el catálogo de ofertas y las reservas
(la API propia), las rutas y las teselas del mapa; lo demás funciona sin conexión. Ninguna clave viaja en el APK.

## Cómo ejecutarla

Requisitos: Android Studio reciente (con su JDK) y un emulador o móvil con Android 7.0 o superior.

```bash
git clone https://github.com/cemarvar/turismo-valencia-app.git
cd turismo-valencia-app
./gradlew assembleDebug        # en Windows: ./gradlew.bat assembleDebug
```

O abre la carpeta en Android Studio, espera a que sincronice Gradle y pulsa **Run**.

| Comando | Para qué |
|---|---|
| `./gradlew assembleDebug` | Compilar el APK de depuración |
| `./gradlew lint` | Análisis estático |
| `./gradlew test` | Tests unitarios (cliente de ValBot, respaldo, fichas enlazadas, Markdown, mapeo de Room, cliente de la API propia con sus ejemplos del contrato, calendario del refresco nocturno, y los idiomas: formatos, todas las traducciones de textos y de contenido y las reglas de ValBot en los cuatro idiomas) |
| `./gradlew connectedDebugAndroidTest` | Tests en el emulador o móvil: persistencia y migración de Room (cierran y reabren la base de datos), el catálogo de ofertas con su caché, las reservas con un servidor falso (idempotencia, errores), el refresco nocturno con el WorkManager de pruebas, avisos (alarmas y notificaciones reales, también en cada idioma) y los idiomas (recursos, contenido traducido, buscador) |
| `./gradlew assembleReleaseCheck` | APK de publicación (R8) firmado con la clave de depuración y apuntando al backend local, para `tools/verify/verify_release.py` |

Por defecto la app habla con la API publicada. Para que use el backend local (`cd backend && npm start`, con Docker Desktop), desde el
emulador o un móvil por USB: `adb reverse tcp:54321 tcp:54321` y
`./gradlew installDebug -PapiBaseUrl=http://127.0.0.1:54321/functions/v1/api-v1/` (la versión de depuración admite HTTP sin cifrar
solo hacia el ordenador; la oferta de pruebas con aforo, `prueba-aforo`, solo existe en local).

### Abrir una pantalla concreta (solo depuración)

```bash
adb shell am start -n com.cmarcillovargas.turismovalencia/.MainActivity --es route detalle/mercado-central
# --ez offline true       simula que no hay conexión
# --ez emptyFavorites true vacía los favoritos
# --ez chatFake true        ValBot contesta como Gemini sin llamar al servidor
# --ez chatDown true        ValBot sin asistente en línea: contesta con las reglas de la app
# --es lang ES|VA|EN|FR     cambia el idioma de la app (el emulador está en inglés: las pruebas piden el español)
# --ez resetData true       vuelve a los datos de demostración (lo guardado persiste entre arranques)
adb emu geo fix -0.3755 39.4745   # (en el emulador) simula estar en Valencia: longitud y latitud
```

Rutas: `inicio`, `mapa`, `valbot`, `favoritos`, `reservas`, `busqueda`, `categoria/que-ver`, `detalle/<id>`,
`entradas?tab=experiencias`, `sostenible`, `ruta/<id>`, `reservar/<id>`, `reserva/seed-1`, `descargas`, `perfil`,
`notificaciones`, `contacto`, `pagina/<Pagina>` (p. ej. `pagina/Navidad`).

## Estructura

```text
app/src/main/
├─ java/com/cmarcillovargas/turismovalencia/
│  ├─ data/          modelos, contenido, repositorios (favoritos, descargas, reservas, ValBot…), Room (`local/`), avisos de eventos (`notify/`) y clientes de red (`remote/`: la API propia con Retrofit, ValBot y rutas)
│  ├─ navigation/    rutas y grafo de navegación
│  ├─ ui/
│  │  ├─ theme/      colores, tipografía, formas y espaciado
│  │  ├─ components/ componentes reutilizables (botones, chips, tarjetas, mapa, bloques de la web…)
│  │  └─ screens/    bienvenida · explorar · mapa · valbot · favoritos · reservas · perfil · web · estados
│  └─ util/          formato e intents (abrir web, llamar, correo, mapa, compartir)
├─ assets/
│  ├─ web/           contenido de las 65 páginas de la web (JSON)
│  └─ img/           fotos de la web, reducidas a 900 px
└─ res/              textos, fuentes, iconos e imágenes del diseño
docs/                diseño (Figma), progreso del proyecto, catálogo de componentes y API del backend
backend/             backend propio en Supabase: migraciones de PostgreSQL, función api-v1, contrato y pruebas
tools/
├─ web-import/       regenera el contenido y las fotos desde el repositorio de la web
├─ icon/             genera los iconos de lanzador para Android 7 (el adaptativo no hace falta)
└─ verify/           pruebas de contenido, interfaz y fotos frente a la web
```

Documentación del proyecto: [`docs/progreso.md`](docs/progreso.md) (hecho, pendiente y decisiones),
[`docs/componentes.md`](docs/componentes.md) (componentes y tokens del tema) y [`CLAUDE.md`](CLAUDE.md) (convenciones).

## Contenido de la web

El contenido sale de la web y no se escribe a mano. Si la web cambia:

```bash
cd tools/web-import
node fetch.mjs      # descarga las páginas de la web
node convert.mjs    # genera assets/web/*.json
python images.py    # descarga y reduce las fotos a assets/img/
```

Más detalles en [`tools/web-import/README.md`](tools/web-import/README.md).

## Pruebas

`tools/verify` comprueba que la app muestra lo mismo que la web y que las fotos cargan
(ver [`tools/verify/README.md`](tools/verify/README.md)):

- **Datos:** las 65 páginas de la web están en la app y se llega a todas desde alguna pantalla, y cada ficha de los índices tiene su equivalente.
- **Web publicada:** títulos, fotos, orden y textos de la portada y de los índices frente a lo que muestra la web en directo.
- **Interfaz (emulador):** chips, secciones, las 21 tarjetas de la portada, contacto, guías, fichas y buscador.
- **Mapa (emulador):** teselas, marcadores, selección, zoom, ubicación real simulada, ruta real, modo sin conexión y permisos.
- **Persistencia (emulador):** favoritos, descargas, reservas, ajustes, búsquedas, avisos y bienvenida siguen ahí tras cerrar y abrir la app.
- **Avisos (emulador):** permiso real de notificaciones (diálogo, «No permitir» y «Abrir ajustes»), alarma de la mascletà en `dumpsys alarm`, disparo adelantando el reloj, tocar la notificación con la app cerrada y reinicio del móvil.
- **Ofertas del servidor (emulador):** el catálogo llega y se guarda con su versión, el servidor confirma con 304 al reabrir, se ve sin conexión desde la caché y, sin nada guardado, el catálogo incluido en la app (`verify_ui.py ofertas`).
- **Reservas (emulador + backend local):** el servidor confirma y la app guarda su localizador, aforo («Ya no quedan plazas»), sin conexión (aviso y botón desactivado), servidor inalcanzable con reintento con la misma clave (una sola reserva en el servidor) y reserva cancelada en el servidor (`verify_ui.py reservas`).
- **Refresco nocturno (emulador):** el trabajo de WorkManager se programa con los ajustes de Descargas (red sin tarifa con «Solo Wi-Fi», a partir de las 3:00), se cancela al apagar «Actualizar automáticamente» y, con la app cerrada y el reloj pasadas las 3:00, el sistema lo ejecuta y deja programado el de la noche siguiente (`verify_ui.py refresco`).
- **Privacidad (emulador):** la política de privacidad de la web lleva debajo lo que añade la app (`verify_ui.py privacidad`).
- **Publicación (emulador + backend local):** la versión minificada con R8 arranca, guarda en Room, pide el catálogo, programa y ejecuta el refresco nocturno (WorkManager) y reserva con Retrofit y lee el error «Ya no quedan plazas» (`verify_release.py`).
- **Idiomas (emulador):** cambiar desde la hoja de idioma y desde los ajustes del móvil, y ver Inicio, fichas, reservas, notificaciones, Tickets & Tours, buscador y ValBot en español, valenciano, inglés y francés; que persiste; y el primer arranque con el móvil en inglés y en alemán (`verify_ui.py idiomas`).
- **Fotos:** tras cambiar de sección, cada tarjeta muestra su propia foto.
- **Backend (local):** `cd backend && npm test` prueba la API por HTTP (ofertas, reservas, idempotencia, aforo con peticiones simultáneas, límites y base de datos cerrada).

## Hoja de ruta

- [x] **ValBot en línea**: conectado a la función `/api/chat` de la web (Gemini); la clave se queda en el servidor y, si no responde, contesta con reglas
- [x] **Backend propio en Supabase** (`backend/`): PostgreSQL y una API de ofertas y reservas con escritura ([`docs/api.md`](docs/api.md)); publicado en Supabase y probado en local y en la nube
- [x] **Ofertas del servidor** en la app: Retrofit + kotlinx.serialization, caché en Room (esquema v3 con migración) y respaldo sin conexión
- [x] **Reservas reales** con el backend: reservar exige conexión, UUID del móvil e idempotencia por intento en Room (esquema v4 con migración), errores por código y dinero en céntimos
- [x] **Refresco nocturno en segundo plano** (WorkManager, respetando «Solo Wi-Fi» y «Actualizar automáticamente») y **privacidad de la app** en la página legal
- [x] **Persistencia local** con Room: favoritos, descargas, reservas, ajustes, búsquedas, avisos y primer arranque
- [x] **Mapa real** con osmdroid, ubicación del usuario y rutas a pie y en bici
- [ ] **Mapa sin conexión por zonas** (hoy se ven sin conexión las teselas ya vistas)
- [x] **Permiso de ubicación** real (pantalla de bienvenida, botón del mapa y Perfil)
- [x] **Notificaciones**: permiso del sistema y aviso de la mascletà (alarma local, 30 min antes)
- [x] **Icono de la app** y **pantalla de arranque del sistema** (SplashScreen API)
- [x] **Idiomas** ES · VA · EN · FR: interfaz, contenido propio (fichas y ofertas), títulos de la web, ValBot sin conexión, avisos y formatos; elegir desde la app o desde los ajustes del móvil ([`docs/idiomas.md`](docs/idiomas.md))
- [ ] Traducir el texto de las páginas de la web (hoy solo en español; se traducen sus títulos)
- [ ] Modo oscuro (el diseño de Figma solo define el tema claro)

## Créditos

Proyecto de fin de ciclo (DAM) de **César** ([@cemarvar](https://github.com/cemarvar)). Contenido, textos y fotografías de la web
[Turismo Valencia](https://github.com/cemarvar/TurismoValencia). Diseño en Figma.

Mapa © [colaboradores de OpenStreetMap](https://www.openstreetmap.org/copyright) (datos bajo licencia ODbL). Rutas calculadas con
[OSRM](https://project-osrm.org) en el servidor de [OpenStreetMap Alemania](https://routing.openstreetmap.de).
