# Turismo Valencia · app Android

App nativa para turistas en Valencia, derivada de la web [Turismo Valencia](https://turismovalenciaa.netlify.app)
([repositorio de la web](https://github.com/cemarvar/TurismoValencia)). Proyecto del TFG de DAM.

Qué ver, eventos, gastronomía y alojamientos; mapa y rutas a pie; tickets, tours y reservas con entrada QR;
un asistente (ValBot) y contenido guardado para usar **sin conexión**. Hecha con Kotlin y Jetpack Compose,
siguiendo el diseño de Figma pantalla a pantalla.

> **Estado:** frontend completo y funcional; ValBot conectado a la web y lo del usuario guardado en local con Room. Aún no hay
> reservas en servidor ni mapa real (ver [Hoja de ruta](#hoja-de-ruta)).

## Qué incluye

| Sección | Pantallas |
|---|---|
| **Bienvenida** | Splash, onboarding de 3 páginas, permiso de ubicación, selector de idioma (ES · VA · EN · FR) |
| **Explorar** | Portada con carrusel de destacados y chips de sección, búsqueda con sugerencias, listados por sección, fichas de lugar y de evento, Tickets & Tours, turismo sostenible |
| **Contenido de la web** | Las 65 páginas de la web: Lo esencial, Planes, Qué ver, Eventos, Gastronomía, Alojamientos, Sostenible, guías de entradas, tours y experiencias, y los textos legales; más Contacto. Los índices de sección siguen el orden de la web (15 lugares, 6 eventos, 9 platos, 6 alojamientos) |
| **Mapa** | Mapa con filtros por categoría y ruta a pie con cuatro modos de transporte |
| **ValBot** | Asistente con IA (Gemini, a través de la web) con tarjetas de lugares y guías, dictado por voz, respuestas básicas si el servidor no responde y modo sin conexión |
| **Favoritos y descargas** | Favoritos, fichas y mapa guardados, cuota de espacio, solo Wi-Fi |
| **Reservas** | Reservar en dos pasos, reserva confirmada con código QR, historial |
| **Perfil** | Idioma, notificaciones, ubicación, tamaño del texto, cerrar sesión |
| **Estados** | Sin conexión, cargando, favoritos vacío, búsqueda sin resultados |

Las 27 pantallas del [diseño de Figma](docs/Figma) están implementadas y comparadas con las capturas en el emulador.

## Stack

- **Kotlin 2.2** y **Jetpack Compose** (BOM 2026.02.01) con **Material 3**
- **Arquitectura MVVM**: una pantalla = `Screen` + `UiState` + `ViewModel`, con `Route` que los conecta
- **Navigation Compose** (barra inferior solo en las rutas que la llevan en el diseño)
- Tipografía **Source Sans 3** empaquetada, para que la app funcione sin conexión
- **Room** (base de datos local con lo del usuario), **OkHttp** (llamada a `/api/chat` de ValBot), **ZXing** (código QR real) y `desugar_jdk_libs` (`java.time` desde Android 7)
- Android: `minSdk 24`, `targetSdk 37`, solo tema claro

Datos: el contenido de la web como JSON en `assets/web/` (con 479 fotos en `assets/img/`) y lo del usuario (favoritos, descargas,
reservas, ajustes…) en una base de datos Room que sigue ahí al cerrar la app. ValBot es lo único que usa Internet. Ninguna clave viaja en el APK.

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
| `./gradlew test` | Tests unitarios (cliente de ValBot, respaldo, fichas enlazadas, Markdown, mapeo de Room) |
| `./gradlew connectedDebugAndroidTest` | Tests de Room en el emulador o móvil (cierran y reabren la base de datos) |

### Abrir una pantalla concreta (solo depuración)

```bash
adb shell am start -n com.cmarcillovargas.turismovalencia/.MainActivity --es route detalle/mercado-central
# --ez offline true       simula que no hay conexión
# --ez emptyFavorites true vacía los favoritos
# --ez chatFake true        ValBot contesta como Gemini sin llamar al servidor
# --ez resetData true       vuelve a los datos de demostración (lo guardado persiste entre arranques)
```

Rutas: `inicio`, `mapa`, `valbot`, `favoritos`, `reservas`, `busqueda`, `categoria/que-ver`, `detalle/<id>`,
`entradas?tab=experiencias`, `sostenible`, `ruta/<id>`, `reservar/<id>`, `reserva/seed-1`, `descargas`, `perfil`,
`notificaciones`, `contacto`, `pagina/<Pagina>` (p. ej. `pagina/Navidad`).

## Estructura

```text
app/src/main/
├─ java/com/cmarcillovargas/turismovalencia/
│  ├─ data/          modelos, contenido, repositorios (favoritos, descargas, reservas, ValBot…), Room (`local/`) y cliente de ValBot (`remote/`)
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
docs/                diseño (Figma), progreso del proyecto y catálogo de componentes
tools/
├─ web-import/       regenera el contenido y las fotos desde el repositorio de la web
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
- **Persistencia (emulador):** favoritos, descargas, reservas, ajustes, búsquedas, avisos y bienvenida siguen ahí tras cerrar y abrir la app.
- **Publicación (emulador):** la versión minificada con R8 arranca y guarda en Room (`verify_release.py`).
- **Fotos:** tras cambiar de sección, cada tarjeta muestra su propia foto.

## Hoja de ruta

- [x] **ValBot en línea**: conectado a la función `/api/chat` de la web (Gemini); la clave se queda en el servidor y, si no responde, contesta con reglas
- [ ] **Actividades y reservas** desde la base de datos (y endpoint de escritura para crear reservas)
- [x] **Persistencia local** con Room: favoritos, descargas, reservas, ajustes, búsquedas, avisos y primer arranque
- [ ] **Mapa real** con osmdroid y mapa sin conexión
- [ ] **Permisos** de ubicación y notificaciones (aviso de la mascletà)
- [ ] Icono de la app, SplashScreen del sistema e **idiomas** ES · VA · EN · FR
- [ ] Modo oscuro (el diseño de Figma solo define el tema claro)

## Créditos

Proyecto de fin de ciclo (DAM) de **César** ([@cemarvar](https://github.com/cemarvar)). Contenido, textos y fotografías de la web
[Turismo Valencia](https://github.com/cemarvar/TurismoValencia). Diseño en Figma.
