"""Prueba rápida de la versión de publicación (release, con R8): arranca, guarda en Room, catálogo de ofertas del servidor
(Retrofit + kotlinx.serialization), ValBot (OkHttp), mapa, ruta, reserva y aviso de la mascletà (alarma con `AlarmManager`,
receptores y notificaciones).
Una versión minificada puede romperse sin que la de depuración lo note (p. ej. al añadir una librería de Kotlin fuera de
`packageScope`), así que conviene pasarla tras tocar dependencias o `app/build.gradle.kts`.

Uso: python verify_release.py [ruta/al/app-release-signed.apk]
  Con ruta, la instala antes (`adb install -r`). Sin ella usa el APK que ya esté instalado.
  Para obtener el APK: ./gradlew assembleRelease y firmar `app/build/outputs/apk/release/app-release-unsigned.apk` con
  `apksigner sign --ks ~/.android/debug.keystore --ks-pass pass:android --key-pass pass:android` (la clave de depuración
  permite instalarlo encima de la de depuración).
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
    print("\n## Versión de publicación (R8): arranque, Room, ValBot, mapa, ruta y reservas")
    if len(sys.argv) > 1:
        out = v.adb("install", "-r", sys.argv[1])
        v.check("APK instalado", "Success" in out, out.strip()[:80])
    v.adb("logcat", "-c")
    v.adb("shell", "pm", "clear", v.PKG)

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

    # Reserva: transacción de Room bajo R8 y localizador correlativo
    tab("inicio")
    v.check("vuelve a Inicio", wait_for("Buscar en Valencia…", 5))
    v.check("chip Tickets & Tours", v.tap_chip("Tickets & Tours"))
    time.sleep(1.2)
    v.tap_text("Reservar")
    time.sleep(1.0)
    v.tap_text("Confirmar reserva")
    time.sleep(1.5)
    locator = next((t for t in v.texts() if re.fullmatch(r"TV-\d{4}-\d{4}", t)), "")
    v.check("reserva confirmada con localizador (…-0416)", locator.endswith("0416"), locator)
    restart()
    tab("reservas")
    v.check("tras reiniciar hay 4 reservas", wait_for("4 reservas · 1 pendiente", 5))

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

    log = v.adb("shell", "logcat", "-d", "-t", "3000")
    bad = [l for l in log.split("\n") if ("FATAL EXCEPTION" in l or "IllegalAccessError" in l or "NoSuchMethodError" in l or "ClassNotFoundException" in l) and v.PKG in log]
    v.check("sin cierres ni errores de clases en logcat", not bad, bad[0][:140] if bad else "")
    print("\nRESULTADO: %d/%d correctas" % (sum(v.results), len(v.results)))
    sys.exit(0 if all(v.results) else 1)


if __name__ == "__main__":
    main()
