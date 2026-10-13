"""Prueba rápida de la versión de publicación (release, con R8): arranca, guarda en Room, ValBot (OkHttp) y reserva.
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
    print("\n## Versión de publicación (R8): arranque, Room, ValBot y reservas")
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
    v.check("terminada la bienvenida se entra en Inicio", wait_for("Buscar en Valencia…", 6))
    v.check("Inicio lee las descargas de Room (Mercado Central sin conexión)", v.find("Sin conexión") is not None)

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

    log = v.adb("shell", "logcat", "-d", "-t", "3000")
    bad = [l for l in log.split("\n") if ("FATAL EXCEPTION" in l or "IllegalAccessError" in l or "NoSuchMethodError" in l or "ClassNotFoundException" in l) and v.PKG in log]
    v.check("sin cierres ni errores de clases en logcat", not bad, bad[0][:140] if bad else "")
    print("\nRESULTADO: %d/%d correctas" % (sum(v.results), len(v.results)))
    sys.exit(0 if all(v.results) else 1)


if __name__ == "__main__":
    main()
