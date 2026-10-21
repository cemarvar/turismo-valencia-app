"""Prueba rápida de la versión de publicación (release, con R8): arranca, guarda en Room, catálogo de ofertas del servidor
y reservas (Retrofit + kotlinx.serialization: GET, POST con cabeceras y cuerpo, y el cuerpo de los errores; recuperarlas con su
localizador y su correo y cancelarlas), el refresco nocturno
(WorkManager con su `WorkerFactory`: se programa y, al lanzarlo adelantando el reloj, el trabajo pone todo al día), ValBot (OkHttp),
mapa, ruta, aviso de la mascletà (alarma con `AlarmManager`, receptores y notificaciones), el cambio de idioma (AppCompat), el modo oscuro (tema DayNight, esquema oscuro y filtro del mapa) y el selector de tema (Claro / Oscuro / el del móvil: AppCompat, la copia rápida y Room).
Una versión minificada puede romperse sin que la de depuración lo note (p. ej. al añadir una librería de Kotlin fuera de
`packageScope`), así que conviene pasarla tras tocar dependencias o `app/build.gradle.kts`.

Uso: python verify_release.py [ruta/al/app-releaseCheck.apk]
  Con ruta, la instala antes (`adb install -r`). Sin ella usa el APK que ya esté instalado.
  El APK sale de `./gradlew assembleReleaseCheck` (`app/build/outputs/apk/releaseCheck/app-releaseCheck.apk`): es la versión
  de publicación con R8, firmada con la clave de depuración (se instala encima de la de depuración) y que apunta siempre al
  backend local, para que las reservas de prueba no lleguen nunca a la nube.
  Necesita el backend local en marcha (cd backend && npm start, con Docker Desktop): lo comprueba y crea `adb reverse tcp:54321`.
Necesita el emulador a 780x1688 px / 320 dpi. La app de publicación no admite los extras de depuración, así que la
prueba entra por la bienvenida y navega con la barra inferior. Borra los datos de la app al empezar."""
import re
import sys
import time

import verify_ui as v

BAR_Y = 1580
BAR_X = {"inicio": 78, "mapa": 234, "valbot": 390, "favoritos": 547, "reservas": 703}


def restart():
    v.adb("shell", "am", "force-stop", v.PKG)
    v.adb("shell", "am", "start", "-n", v.PKG + "/.MainActivity")
    time.sleep(3.0)


def tab(name):
    v.adb("shell", "input", "tap", str(BAR_X[name]), str(BAR_Y))
    time.sleep(1.6)


def wait_for(text, seconds=8.0, exact=True):
    end = time.time() + seconds
    while time.time() < end:
        if v.find(text, exact=exact):
            return True
        time.sleep(0.7)
    return False


def main():
    print("\n## Versión de publicación (R8): arranque, Room, ofertas y reservas del servidor, refresco nocturno, ValBot, mapa y ruta")
    if not v.local_api_up() or v.local_sql("select 1") is None:
        print("FAIL - hace falta el backend local en marcha (cd backend && npm start, con Docker Desktop): la app de comprobación apunta a él")
        sys.exit(1)
    v.adb("reverse", "tcp:54321", "tcp:54321")
    v.clean_local_backend()
    if len(sys.argv) > 1:
        out = v.adb("install", "-r", sys.argv[1])
        v.check("APK instalado", "Success" in out, out.strip()[:80])
    v.adb("logcat", "-c")
    v.adb("shell", "pm", "clear", v.PKG)
    # El emulador está en inglés y la app sigue el idioma del móvil: la prueba espera el español (la versión de publicación no admite
    # el extra `--es lang`, así que se fija el idioma de la app en el sistema).
    v.set_app_locale("es")

    # Primer arranque: crea la base de datos y la rellena con la demostración
    restart()
    time.sleep(1.8)  # splash
    v.check("primer arranque: se muestra la bienvenida", wait_for("Saltar", 6))
    v.tap_text("Saltar")
    v.tap_text("Mientras se usa la app")
    # La pantalla de permiso abre después el diálogo real del sistema (en inglés en el emulador).
    if wait_for("While using the app", 5):
        v.tap_text("While using the app")
    v.check("terminada la bienvenida se entra en Inicio", wait_for("Buscar en Valencia…", 8))
    v.check("Inicio lee las descargas de Room (Mercado Central sin conexión)", v.find("Sin conexión") is not None)
    line = v.offers_log("actualizado")
    v.check("Ofertas: Retrofit y kotlinx.serialization piden el catálogo al servidor y lo guardan bajo R8", "actualizado" in line, line)

    # Refresco nocturno: WorkManager (androidx.work, con su propia WorkerFactory) bajo R8. Se programa al abrir la app y, al lanzar el
    # trabajo (con el reloj pasadas las 3:00), el sistema lo ejecuta con la configuración de `TurismoApp` y pone todo al día.
    job = v.wait_job(True, 15)
    v.check("Refresco nocturno: WorkManager programa su trabajo bajo R8, solo con Wi-Fi y con batería no baja", bool(job and job["unmetered"] and job["battery_not_low"]), str(job))
    line = v.run_night_job(job) if job else ""
    v.check("Refresco nocturno: al lanzar el trabajo, la WorkerFactory crea el RefreshWorker y todo queda al día", "al día" in line, line)
    again = v.wait_job(True, 10)
    v.check("Refresco nocturno: deja programado el de la noche siguiente", bool(again and again["delay"] is not None and 23 * 3600 < again["delay"] <= 24 * 3600 + 60), str(again and again["delay"]))

    # Favoritos: quitar uno y que siga quitado tras reiniciar
    tab("favoritos")
    v.check("demo: 6 lugares guardados", wait_for("6 lugares guardados", 5))
    v.tap_text("Quitar de favoritos")
    v.check("quitar un favorito: quedan 5", wait_for("5 lugares guardados", 4))
    restart()
    v.check("al reabrir no se repite la bienvenida (se guardó)", wait_for("Buscar en Valencia…", 6) and v.find("Saltar") is None)
    tab("favoritos")
    v.check("tras reiniciar siguen 5 favoritos", wait_for("5 lugares guardados", 5))

    # ValBot: OkHttp + org.json bajo R8 (con el servidor caído contesta el respaldo)
    tab("valbot")
    v.check("ValBot: abre con su saludo", wait_for("¡Hola! Soy ValBot, tu asistente de turismo en Valencia. ¿En qué puedo ayudarte?", 5))
    v.tap_text("Restaurantes de paella cerca")
    got = wait_for("Mira estas opciones:", 14, exact=False) or wait_for("paella", 2, exact=False)
    answered = any("paella" in t.lower() and t != "Restaurantes de paella cerca" for t in v.texts())
    v.check("ValBot: contesta (en línea o con el respaldo básico)", got and answered, "")

    # Mapa (osmdroid) y ruta (OkHttp + org.json) bajo R8
    tab("mapa")
    v.check("Mapa: atribución de OpenStreetMap", wait_for("© Colaboradores de OpenStreetMap", 10))
    time.sleep(4)
    detail = v.map_detail()
    v.check("Mapa: muestra teselas de OpenStreetMap", detail > 28, "desviación %.1f" % detail)
    pins = [t for t in ("Lonja de la Seda", "Catedral y Santo Cáliz", "Mercado Central", "Barrio del Carmen") if v.pin_nodes(t)]
    v.check("Mapa: marcadores accesibles sobre el mapa", len(pins) >= 3, str(pins))
    v.tap_text("Cómo llegar")
    v.check("Ruta: se calcula con el servicio y acaba en la llegada", wait_for("Has llegado: Mercado Central", 14))
    time.sleep(2)
    v.check("Ruta: distancia y hora de llegada", any(re.match(r"^\d+ m · llegada \d\d:\d\d$", t) or re.match(r"^\d[\d,]* km · llegada", t) for t in v.texts()))
    v.back()
    tab("inicio")

    # Reserva: Retrofit + kotlinx.serialization (POST con cabeceras y cuerpo) y la transacción de Room bajo R8
    tab("inicio")
    v.check("vuelve a Inicio", wait_for("Buscar en Valencia…", 5))
    v.check("chip Tickets & Tours", v.tap_chip("Tickets & Tours"))
    time.sleep(1.2)
    v.tap_text("Reservar")
    time.sleep(1.0)
    v.tap_text("Confirmar reserva")
    locator = v.locator_on_screen()
    v.check("reserva confirmada por el servidor, con su localizador", bool(locator), locator)
    server = (v.local_sql("select status, email, adults from app.reservations where locator = '%s'" % locator) or [""])[0]
    v.check("el servidor la tiene: confirmada, con el correo de la demostración y 2 adultos", server == "confirmed|laura.garcia@example.com|2", server)
    restart()
    tab("reservas")
    v.check("tras reiniciar hay 4 reservas", wait_for("4 reservas · 1 pendiente", 5))

    # Error del servidor: el cuerpo de error se lee con kotlinx.serialization y sale «Ya no quedan plazas» (la oferta de pruebas
    # tiene 3 plazas a las 10:00 y una reserva previa de 2 deja solo 1)
    v.local_sql("select app.create_reservation(gen_random_uuid(), gen_random_uuid(), 'prueba-aforo', (now() at time zone 'Europe/Madrid')::date + 1, '10:00', 2, 0, 'precarga@example.com')")
    tab("inicio")
    v.check("chip Tickets & Tours", v.tap_chip("Tickets & Tours"))
    time.sleep(1.2)
    v.tap_text("Tours")
    time.sleep(1.0)
    v.scroll_to("Oferta de pruebas con aforo", tries=20)  # la última oferta de la pestaña (después vienen las guías)
    v.swipe_up(1, dist=500)  # y un poco más, para ver su botón entero
    ns = v.nodes()
    title = next((n for n in ns if n["t"] == "Oferta de pruebas con aforo"), None)
    buttons = [n for n in ns if n["t"] == "Reservar" and title and n["b"][1] >= title["b"][1] - 40]
    v.check("la oferta de pruebas con aforo está en Tours, con su botón «Reservar»", bool(title and buttons))
    if buttons:
        v.tap(min(buttons, key=lambda n: n["b"][1]))
    time.sleep(1.0)
    v.tap_text("10:00")
    v.tap_text("Confirmar reserva")
    v.check("sin plazas sale «Ya no quedan plazas para ese día y horario»", wait_for("Ya no quedan plazas para ese día y horario. Prueba con otro.", 8))
    v.back()
    v.back()

    # Recuperar y cancelar: Retrofit con @Path, @Query, @Header y @Body, la reserva ya cancelada que lee kotlinx.serialization y el error
    # 404 del contrato, todo bajo R8. Se recupera la reserva de antes (ya está en el móvil: no se duplica) y se cancela.
    tab("reservas")
    v.scroll_to("¿Reservaste desde otro móvil?")
    v.tap_text("¿Reservaste desde otro móvil?")
    v.check("Recuperar: se abre el diálogo bajo R8", wait_for("Recuperar una reserva", 5))
    if len(v.edit_fields()) == 2:
        v.tap(v.edit_fields()[0])
        v.type_text("TV-2099-9999")
        v.tap(v.edit_fields()[1])
        v.type_text("laura.garcia@example.com")
        v.tap_text("Recuperar")
        v.check("Recuperar: un localizador que no existe devuelve el error del contrato (404 reservation_not_found)", wait_for("No hay ninguna reserva con ese localizador y ese correo. Revisa los dos datos.", 10))
        v.clear_field(v.edit_fields()[0])
        v.type_text(locator.lower())
        v.tap_text("Recuperar")
        shown = v.locator_on_screen(12)
        v.check("Recuperar: con su localizador y su correo se abre la reserva (GET con @Path y @Query)", shown == locator, shown)
        button = v.open_cancel_button()
        v.check("la reserva ofrece «Cancelar reserva»", bool(button and button["en"]))
        v.tap_text("Cancelar reserva")
        wait_for("¿Cancelar esta reserva?", 5)
        v.tap_text("Sí, cancelar")
        v.check("Cancelar: el servidor la cancela (POST con @Path, @Header y @Body) y la pantalla lo dice", wait_for("Reserva cancelada", 10))
        v.check("el servidor la tiene cancelada", v.server_status(locator) == "cancelled", str(v.server_status(locator)))
    else:
        v.check("Recuperar: el diálogo tiene sus dos campos", False)
    v.back()  # la confirmación de una reserva no lleva la barra inferior
    tab("inicio")

    # Avisos de eventos: receptores, AlarmManager y notificaciones (androidx.core) bajo R8
    v.set_notification_permission(True)
    restart()
    alarms = v.wait_alarms(1, 10)
    v.check("Avisos: con el permiso dado, la alarma de la mascletà se programa al arrancar", len(alarms) == 1 and v.is_mascleta_window(alarms[0]), str(alarms))
    tab("inicio")
    v.check("chip Eventos", v.tap_chip("Eventos"))
    time.sleep(1.2)
    v.check("Fallas 2027 está en los eventos de la portada", v.tap_text("Fallas 2027"))
    time.sleep(2)
    switch = v.reminder_switch()
    v.check("Fallas: el aviso de la mascletà sale encendido", bool(switch and switch["on"]))
    if switch:
        v.tap(switch)
    time.sleep(1.2)
    v.check("apagar el aviso cancela la alarma", len(v.wait_alarms(0)) == 0)
    v.back()

    # Idiomas: el idioma por app de AppCompat (AppCompatActivity, su servicio de almacenamiento y LocaleManager) y los recursos de cada
    # idioma bajo R8, cambiando desde la hoja de Inicio y desde los ajustes del móvil.
    tab("inicio")
    v.check("Idiomas: Inicio en español, con el chip «ES»", wait_for("Buscar en Valencia…", 5) and v.find("ES") is not None)
    v.tap_text("ES")
    v.check("Idiomas: la hoja ofrece los cuatro idiomas", wait_for("Valencià", 4) and all(v.find(t) for t in ("Español", "English", "Français")))
    v.tap_text("English")
    v.tap_text("Aplicar")
    v.check("Idiomas: «Aplicar» pasa la app a inglés (AppCompat bajo R8)", wait_for("Search Valencia…", 8) and v.find("What to see") is not None)
    v.check("Idiomas: las fichas salen traducidas (Central Market) con el formato inglés", wait_for("Central Market", 5) and any("(2.3k)" in t for t in v.texts()))
    v.check("Idiomas: el sistema guarda «en»", v.app_locales() == "en", v.app_locales())
    restart()
    v.check("Idiomas: tras cerrar y abrir la app sigue en inglés", wait_for("Search Valencia…", 8))
    v.set_app_locale("fr")
    v.check("Idiomas: desde los ajustes del móvil, francés", wait_for("Rechercher à Valence…", 8) and v.find("À voir") is not None)
    v.set_app_locale("ca-ES-valencia")
    v.check("Idiomas: desde los ajustes del móvil, valenciano (ca-ES-valencia → values-ca)", wait_for("Cerca a València…", 8) and v.find("Què veure") is not None)
    v.set_app_locale("es")
    v.check("Idiomas: de vuelta al español", wait_for("Buscar en Valencia…", 8) and v.find("Qué ver") is not None)

    # Selector de tema (Perfil → Tema): `AppCompatDelegate.setDefaultNightMode`, la copia rápida del modo en SharedPreferences que lee
    # `TurismoApp` antes de crear la actividad y Room, bajo R8. El móvil sigue en claro: «Oscuro» tiene que pintar la app oscura por sí solo.
    tab("favoritos")
    v.tap_text("LG")
    v.check("Tema: Perfil ofrece la fila «Tema» con «Sigue al del sistema»", wait_for("Tema", 5) and v.tema_subtitle() == "Sigue al del sistema", str(v.tema_subtitle()))
    v.tap_text("Tema")
    wait_for("Cancelar", 4)
    v.tap_text("Oscuro")
    time.sleep(2.8)
    v.check("Tema: «Oscuro» con el móvil en claro pinta la app oscura bajo R8", v.app_is_dark() and v.tema_subtitle() == "Oscuro", str(v.tema_subtitle()))
    restart()
    v.check("Tema: tras cerrar y abrir la app sigue oscura (se guardó y se aplica al arrancar)", wait_for("Buscar en Valencia…", 8) and v.app_is_dark())
    tab("favoritos")
    v.tap_text("LG")
    wait_for("Tema", 5)
    v.tap_text("Tema")
    wait_for("Cancelar", 4)
    v.tap_last("Sigue al del sistema")
    time.sleep(2.8)
    v.check("Tema: «Sigue al del sistema» con el móvil en claro vuelve a pintarla clara", v.app_is_light() and v.tema_subtitle() == "Sigue al del sistema", str(v.tema_subtitle()))
    v.back()
    tab("inicio")

    # Modo oscuro: el tema DayNight de AppCompat, el esquema oscuro de Compose y el filtro de color del mapa bajo R8. El resto de la prueba
    # es en modo claro; aquí se enciende el oscuro del sistema (`cmd uimode night yes`) y se vuelve al claro al terminar.
    try:
        v.set_night(True)
        restart()
        tab("inicio")
        v.check("Oscuro: Inicio se repinta oscuro bajo R8", wait_for("Buscar en Valencia…", 8) and v.region_luma(v.shot(), (0, 900, 24, 1500)) < 30, "%.0f" % v.region_luma(v.shot(), (0, 900, 24, 1500)))
        tab("mapa")
        v.check("Oscuro: el mapa conserva la atribución y oscurece sus teselas bajo R8", wait_for("© Colaboradores de OpenStreetMap", 10))
        time.sleep(4)
        mean = v.region_luma(v.shot(), (0, 560, 780, 1000))
        v.check("Oscuro: las teselas del mapa son oscuras y con detalle", mean < 85 and v.map_detail() > 14, "media %.0f, detalle %.1f" % (mean, v.map_detail()))
        tab("reservas")
        v.check("Oscuro: Mis reservas sigue funcionando", wait_for("reservas · ", 6, exact=False))
    finally:
        v.set_night(False)
    restart()
    tab("inicio")
    v.check("Claro: de vuelta al modo claro", wait_for("Buscar en Valencia…", 8) and v.region_luma(v.shot(), (0, 900, 24, 1500)) > 230)

    log = v.adb("shell", "logcat", "-d", "-t", "3000")
    bad = [l for l in log.split("\n") if ("FATAL EXCEPTION" in l or "IllegalAccessError" in l or "NoSuchMethodError" in l or "ClassNotFoundException" in l) and v.PKG in log]
    v.check("sin cierres ni errores de clases en logcat", not bad, bad[0][:140] if bad else "")
    print("\nRESULTADO: %d/%d correctas" % (sum(v.results), len(v.results)))
    sys.exit(0 if all(v.results) else 1)


if __name__ == "__main__":
    main()
