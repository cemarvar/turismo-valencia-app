"""Pruebas de la API de Turismo Valencia (v1) por HTTP, como las haría la app. No necesitan instalar nada (solo Python 3).

En local (con el entorno arrancado: `npm start`):          npm test
Contra un despliegue real (solo lecturas, sin tocar datos): API_BASE=https://<proyecto>.supabase.co/functions/v1/api-v1 python tests/test_api.py
  Para que además cree reservas de prueba en el despliegue:  API_WRITES=1 (deja reservas reales: úsalo solo a propósito)

Variables: API_BASE (por defecto la local), API_FULL=1 (pruebas con la oferta `prueba-aforo` y los límites: solo existen en local),
API_WRITES=1 (crear reservas) y SUPABASE_ANON_KEY (comprueba que la API automática de Supabase no deja ver nada del esquema `app`;
en local se usa la clave pública de demostración de Supabase y en la nube hay que pasar la del proyecto).
"""
import json
import os
import random
import re
import threading
import unittest
import urllib.error
import urllib.request
import uuid
from datetime import date, datetime, timedelta, timezone

API_BASE = os.environ.get("API_BASE", "http://127.0.0.1:54321/functions/v1/api-v1").rstrip("/")
LOCAL = API_BASE.startswith(("http://127.0.0.1", "http://localhost"))
FULL = os.environ.get("API_FULL", "1" if LOCAL else "0") == "1"
WRITES = os.environ.get("API_WRITES", "1" if LOCAL else "0") == "1"
# Clave pública de demostración del Supabase local (la misma para todo el mundo: no es un secreto). En la nube hay que pasar la del proyecto.
LOCAL_DEMO_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZS1kZW1vIiwicm9sZSI6ImFub24iLCJleHAiOjE5ODM4MTI5OTZ9.CRXP1A7WOeoJeXxjNni43kdQwgnWNReilDMblYTn_I0"
ANON_KEY = os.environ.get("SUPABASE_ANON_KEY") or (LOCAL_DEMO_ANON_KEY if LOCAL else "")
UUID_RE = r"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"


def call(method, path, *, headers=None, body=None, raw=None, ip=None, base=None):
    """Una petición; devuelve (estado, cabeceras en minúsculas, JSON o None). Los errores HTTP no lanzan excepciones."""
    data = raw if raw is not None else (json.dumps(body).encode("utf-8") if body is not None else None)
    request = urllib.request.Request((base or API_BASE) + path, data=data, method=method)
    for name, value in (headers or {}).items():
        request.add_header(name, value)
    if data is not None and "Content-Type" not in (headers or {}):
        request.add_header("Content-Type", "application/json")
    if ip:
        request.add_header("X-Forwarded-For", ip)  # en local el límite de peticiones se fía de ella; en la nube se ignora
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            status, received, text = response.status, response.headers, response.read().decode("utf-8")
    except urllib.error.HTTPError as error:
        status, received, text = error.code, error.headers, error.read().decode("utf-8")
        error.close()
    lowered = {name.lower(): value for name, value in received.items()}
    parsed = json.loads(text) if text and "json" in lowered.get("content-type", "") else None
    return status, lowered, parsed


def random_ip():
    return "10.%d.%d.%d" % (random.randint(1, 254), random.randint(0, 254), random.randint(1, 254))


def madrid_today():
    try:
        from zoneinfo import ZoneInfo

        return datetime.now(ZoneInfo("Europe/Madrid")).date()
    except Exception:  # Windows sin base de zonas horarias: aproximación horario de verano/invierno
        offset = 2 if 4 <= datetime.now(timezone.utc).month <= 10 else 1
        return (datetime.now(timezone.utc) + timedelta(hours=offset)).date()


def future(days):
    return madrid_today() + timedelta(days=days)


def random_day():
    """Un día lejano al azar, para que las pruebas no se estorben entre sí ni con ejecuciones anteriores (los aforos son por día)."""
    return future(random.randint(30, 330))


def reserve(*, offer="albufera-barca", day=None, time="18:30", adults=2, children=0, email=None, device=None, key=None, ip=None):
    payload = {
        "offerId": offer,
        "date": (day or random_day()).isoformat(),
        "time": time,
        "adults": adults,
        "children": children,
        "email": email or "prueba-%s@example.com" % uuid.uuid4().hex[:10],
    }
    headers = {"X-Device-Id": device or str(uuid.uuid4()), "Idempotency-Key": key or str(uuid.uuid4())}
    return call("POST", "/reservations", headers=headers, body=payload, ip=ip or random_ip())


class Catalogo(unittest.TestCase):
    def test_catalogo_de_ofertas(self):
        status, headers, body = call("GET", "/offers")
        self.assertEqual(status, 200)
        self.assertIn("application/json", headers["content-type"])
        self.assertRegex(headers["etag"], r'^(W/)?".+"$')  # en la nube la pasarela la vuelve débil (W/"…") al comprimir la respuesta
        self.assertIn("max-age", headers["cache-control"])
        offers = body["offers"]
        self.assertGreaterEqual(len(offers), 16)
        by_id = {offer["id"]: offer for offer in offers}
        for expected in ("albufera-barca", "taller-paella", "tourist-card-72", "entrada-oceanografic"):
            self.assertIn(expected, by_id)
        for offer in offers:
            self.assertEqual(set(offer), {"id", "kind", "tag", "title", "duration", "place", "extra", "priceFromCents", "description", "placeId", "slots"})
            self.assertIn(offer["kind"], ("entrada", "tour", "experiencia"))
            self.assertIsInstance(offer["priceFromCents"], int)
            self.assertGreaterEqual(offer["priceFromCents"], 0)
            self.assertTrue(offer["slots"], offer["id"])
        self.assertEqual(offers[0]["id"], "albufera-barca")  # el orden del catálogo
        self.assertEqual(by_id["albufera-barca"]["priceFromCents"], 1800)
        self.assertEqual(by_id["albufera-barca"]["slots"], ["16:30", "17:30", "18:30", "19:30"])
        self.assertEqual(by_id["tourist-card-72"]["slots"], ["Cualquier hora"])
        self.assertIsNone(by_id["tourist-card-72"]["placeId"])
        self.assertEqual(by_id["taller-paella"]["placeId"], "mercado-central")

    def test_etag_y_304(self):
        _, headers, _ = call("GET", "/offers")
        etag = headers["etag"]  # tal como llega: fuerte en local, débil (W/"…") tras la pasarela de Supabase
        strong = etag[2:] if etag.startswith("W/") else etag
        status, _, body = call("GET", "/offers", headers={"If-None-Match": etag})
        self.assertEqual((status, body), (304, None))
        self.assertEqual(call("GET", "/offers", headers={"If-None-Match": strong})[0], 304)
        self.assertEqual(call("GET", "/offers", headers={"If-None-Match": "W/" + strong})[0], 304)
        self.assertEqual(call("GET", "/offers", headers={"If-None-Match": '"otra-version"'})[0], 200)

    def test_detalle_de_una_oferta(self):
        status, _, offer = call("GET", "/offers/taller-paella")
        self.assertEqual(status, 200)
        listed = {o["id"]: o for o in call("GET", "/offers")[2]["offers"]}["taller-paella"]
        self.assertEqual(offer, listed)

    def test_oferta_inexistente(self):
        for path in ("/offers/no-existe", "/offers/BAD%20ID", "/offers/Mayusculas"):
            status, _, body = call("GET", path)
            self.assertEqual((status, body["error"]["code"]), (404, "offer_not_found"), path)

    def test_rutas_y_metodos(self):
        status, _, body = call("GET", "/nada")
        self.assertEqual((status, body["error"]["code"]), (404, "not_found"))
        status, headers, body = call("DELETE", "/offers")
        self.assertEqual((status, body["error"]["code"]), (405, "method_not_allowed"))
        self.assertIn("GET", headers["allow"])
        status, headers, _ = call("PUT", "/reservations")
        self.assertEqual(status, 405)
        self.assertIn("POST", headers["allow"])


@unittest.skipUnless(WRITES, "crea reservas de verdad (API_WRITES=1)")
class Reservas(unittest.TestCase):
    def test_crear_una_reserva(self):
        day = random_day()
        status, headers, r = reserve(day=day, adults=2, children=1, email="  Laura.Garcia@Email.COM ")
        self.assertEqual(status, 201)
        self.assertEqual(headers["cache-control"], "no-store")
        self.assertEqual(set(r), {"id", "locator", "offerId", "title", "place", "date", "time", "adults", "children", "email", "totalCents", "status", "createdAt"})
        self.assertRegex(r["id"], UUID_RE)
        self.assertRegex(r["locator"], r"^TV-\d{4}-\d{4,}$")
        self.assertEqual(r["locator"][3:7], str(day.year))
        self.assertEqual(r["totalCents"], 3600)  # 2 adultos × 18 €; los niños no pagan
        self.assertEqual(r["email"], "laura.garcia@email.com")
        self.assertEqual(
            (r["offerId"], r["title"], r["place"], r["date"], r["time"], r["adults"], r["children"], r["status"]),
            ("albufera-barca", "Paseo en barca por la Albufera", "El Palmar", day.isoformat(), "18:30", 2, 1, "confirmed"),
        )
        self.assertRegex(r["createdAt"], r"^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\dZ$")

    def test_los_localizadores_son_correlativos_y_unicos(self):
        numbers = [int(reserve()[2]["locator"].split("-")[2]) for _ in range(3)]
        self.assertEqual(numbers, sorted(set(numbers)))
        self.assertGreaterEqual(numbers[0], 1000)

    def test_oferta_sin_horario_fijo(self):
        status, _, r = reserve(offer="tourist-card-72", time="Cualquier hora", adults=3)
        self.assertEqual(status, 201)
        self.assertEqual((r["time"], r["totalCents"]), ("Cualquier hora", 7500))

    def test_el_reintento_no_duplica(self):
        device, key, day = str(uuid.uuid4()), str(uuid.uuid4()), random_day()
        email = "reintento-%s@example.com" % uuid.uuid4().hex[:8]
        first = reserve(day=day, email=email, device=device, key=key)
        second = reserve(day=day, email=email, device=device, key=key)
        self.assertEqual((first[0], second[0]), (201, 200))
        self.assertEqual(second[1]["idempotent-replayed"], "true")
        self.assertNotIn("idempotent-replayed", first[1])
        self.assertEqual(first[2], second[2])
        listed = call("GET", "/reservations", headers={"X-Device-Id": device})[2]["reservations"]
        self.assertEqual([r["id"] for r in listed], [first[2]["id"]])

    def test_la_misma_clave_con_otros_datos_es_un_error(self):
        device, key, day = str(uuid.uuid4()), str(uuid.uuid4()), random_day()
        email = "clave-%s@example.com" % uuid.uuid4().hex[:8]
        self.assertEqual(reserve(day=day, adults=2, email=email, device=device, key=key)[0], 201)
        status, _, body = reserve(day=day, adults=3, email=email, device=device, key=key)
        self.assertEqual((status, body["error"]["code"]), (409, "idempotency_key_reused"))

    def test_datos_con_forma_incorrecta(self):
        good = {"offerId": "albufera-barca", "date": future(20).isoformat(), "time": "18:30", "adults": 2, "children": 0, "email": "a@b.es"}

        def post(body=None, raw=None, headers=None):
            headers = {"X-Device-Id": str(uuid.uuid4()), "Idempotency-Key": str(uuid.uuid4())} if headers is None else headers
            return call("POST", "/reservations", headers=headers, body=None if raw is not None else (good if body is None else body), raw=raw, ip=random_ip())

        without = lambda field: {k: v for k, v in good.items() if k != field}
        cases = {
            "sin cabeceras": dict(headers={}),
            "X-Device-Id que no es un UUID": dict(headers={"X-Device-Id": "123", "Idempotency-Key": str(uuid.uuid4())}),
            "falta Idempotency-Key": dict(headers={"X-Device-Id": str(uuid.uuid4())}),
            "no es JSON": dict(raw=b"{no es json"),
            "es una lista": dict(raw=b"[]"),
            "objeto vacío": dict(body={}),
            "fecha imposible": dict(body={**good, "date": "2026-02-30"}),
            "fecha mal escrita": dict(body={**good, "date": "20-10-2026"}),
            "adultos como texto": dict(body={**good, "adults": "2"}),
            "cero adultos": dict(body={**good, "adults": 0}),
            "adultos decimales": dict(body={**good, "adults": 1.5}),
            "21 adultos": dict(body={**good, "adults": 21}),
            "niños negativos": dict(body={**good, "children": -1}),
            "horario inválido": dict(body={**good, "time": "25:00"}),
            "oferta con mayúsculas": dict(body={**good, "offerId": "Albufera"}),
            "sin correo": dict(body=without("email")),
            "correo en blanco": dict(body={**good, "email": "   "}),
            "sin oferta": dict(body=without("offerId")),
        }
        for name, args in cases.items():
            with self.subTest(name):
                status, _, body = post(**args)
                self.assertEqual(status, 400)
                self.assertEqual(body["error"]["code"], "invalid_request")

        status, _, body = post(raw=b'{"email": "' + b"a" * 5000 + b'"}')
        self.assertEqual((status, body["error"]["code"]), (413, "payload_too_large"))

    def test_reglas_de_negocio(self):
        cases = {
            "oferta que no existe": (dict(offer="no-existe"), 404, "offer_not_found"),
            "horario que la oferta no tiene": (dict(time="09:00"), 422, "invalid_slot"),
            "fecha pasada": (dict(day=future(-1)), 422, "invalid_date"),
            "más de un año": (dict(day=future(400)), 422, "invalid_date"),
            "más de 20 personas": (dict(adults=15, children=6), 422, "invalid_party"),
            "correo sin arroba": (dict(email="sin-arroba"), 422, "invalid_email"),
            "correo sin dominio": (dict(email="a@b"), 422, "invalid_email"),
        }
        for name, (args, expected_status, code) in cases.items():
            with self.subTest(name):
                status, _, body = reserve(**args)
                self.assertEqual((status, body["error"]["code"]), (expected_status, code))

    def test_listar_solo_las_del_propio_dispositivo(self):
        mine, other = str(uuid.uuid4()), str(uuid.uuid4())
        first = reserve(device=mine)[2]
        second = reserve(device=mine)[2]
        reserve(device=other)
        status, headers, body = call("GET", "/reservations", headers={"X-Device-Id": mine})
        self.assertEqual(status, 200)
        self.assertEqual(headers["cache-control"], "no-store")
        self.assertEqual([r["id"] for r in body["reservations"]], [second["id"], first["id"]])  # la más reciente primero
        self.assertEqual(len(call("GET", "/reservations", headers={"X-Device-Id": other})[2]["reservations"]), 1)
        self.assertEqual(call("GET", "/reservations", headers={"X-Device-Id": str(uuid.uuid4())})[2], {"reservations": []})
        self.assertEqual(call("GET", "/reservations")[0], 400)

    def test_recuperar_por_localizador_y_correo(self):
        email = "Recupera-%s@Example.com" % uuid.uuid4().hex[:8]
        created = reserve(email=email)[2]
        status, _, found = call("GET", "/reservations/%s?email=%s" % (created["locator"], urllib.request.quote(email)), ip=random_ip())
        self.assertEqual((status, found), (200, created))
        # sin distinguir mayúsculas
        status, _, found = call("GET", "/reservations/%s?email=%s" % (created["locator"].lower(), email.upper()), ip=random_ip())
        self.assertEqual((status, found["id"]), (200, created["id"]))
        for name, path in {
            "correo que no es": "/reservations/%s?email=otra@example.com" % created["locator"],
            "sin correo": "/reservations/%s" % created["locator"],
            "localizador mal formado": "/reservations/XX-1?email=%s" % email,
            "localizador que no existe": "/reservations/TV-2099-9999?email=%s" % email,
        }.items():
            with self.subTest(name):
                status, _, body = call("GET", path, ip=random_ip())
                self.assertEqual((status, body["error"]["code"]), (404, "reservation_not_found"))

    @unittest.skipUnless(FULL, "usa la oferta de pruebas, que solo existe en local")
    def test_horario_de_hoy(self):
        today = madrid_today()
        status, _, body = reserve(offer="prueba-aforo", day=today, time="00:01")
        self.assertEqual((status, body["error"]["code"]), (422, "invalid_date"))  # ya pasó
        self.assertEqual(reserve(offer="prueba-aforo", day=today, time="23:59")[0], 201)


@unittest.skipUnless(FULL and WRITES, "usa la oferta de pruebas, que solo existe en local")
class Aforo(unittest.TestCase):
    OFFER = "prueba-aforo"

    def test_las_plazas_se_cuentan_por_dia_y_horario(self):
        day = random_day()
        self.assertEqual(reserve(offer=self.OFFER, day=day, time="10:00", adults=2)[0], 201)  # 2 de 3
        status, _, body = reserve(offer=self.OFFER, day=day, time="10:00", adults=1, children=1)  # 2 + 2 > 3 (los niños ocupan plaza)
        self.assertEqual((status, body["error"]["code"]), (409, "sold_out"))
        self.assertEqual(reserve(offer=self.OFFER, day=day, time="10:00", adults=1)[0], 201)  # 3 de 3
        self.assertEqual(reserve(offer=self.OFFER, day=day, time="10:00", adults=1)[0], 409)  # completo
        self.assertEqual(reserve(offer=self.OFFER, day=day, time="12:00", adults=3)[0], 201)  # otro horario, libre
        self.assertEqual(reserve(offer=self.OFFER, day=day + timedelta(days=1), time="10:00", adults=3)[0], 201)  # otro día, libre

    def test_peticiones_simultaneas_no_se_pasan_del_aforo(self):
        day, statuses = random_day(), []
        threads = [threading.Thread(target=lambda: statuses.append(reserve(offer=self.OFFER, day=day, time="11:00", adults=1)[0])) for _ in range(8)]
        for thread in threads:
            thread.start()
        for thread in threads:
            thread.join()
        self.assertEqual(sorted(statuses), [201] * 3 + [409] * 5)  # aforo de 3: exactamente 3 entran

    def test_el_mismo_intento_a_la_vez_se_guarda_una_vez(self):
        device, key, day = str(uuid.uuid4()), str(uuid.uuid4()), random_day()
        email = "a-la-vez-%s@example.com" % uuid.uuid4().hex[:8]
        results = []
        threads = [threading.Thread(target=lambda: results.append(reserve(day=day, time="17:30", email=email, device=device, key=key))) for _ in range(8)]
        for thread in threads:
            thread.start()
        for thread in threads:
            thread.join()
        statuses = sorted(result[0] for result in results)
        self.assertEqual(statuses, [200] * 7 + [201])
        self.assertEqual(len({result[2]["id"] for result in results}), 1)
        self.assertEqual(len(call("GET", "/reservations", headers={"X-Device-Id": device})[2]["reservations"]), 1)


@unittest.skipUnless(FULL, "los límites solo se prueban en local")
class Limites(unittest.TestCase):
    def test_limite_por_ip(self):
        ip, last = random_ip(), None
        for _ in range(600):
            last = call("GET", "/offers/no-existe", ip=ip)
            if last[0] == 429:
                break
        status, headers, body = last
        self.assertEqual((status, body["error"]["code"]), (429, "rate_limited"))
        self.assertTrue(1 <= int(headers["retry-after"]) <= 60)

    @unittest.skipUnless(WRITES, "crea reservas de verdad (API_WRITES=1)")
    def test_limite_por_correo_y_por_dispositivo(self):
        email = "limite-%s@example.com" % uuid.uuid4().hex[:8]
        base = random.randint(30, 300)
        statuses = [reserve(day=future(base + i), email=email)[0] for i in range(12)]
        self.assertEqual(statuses, [201] * 10 + [429] * 2)  # 10 reservas por correo y día

        device = str(uuid.uuid4())
        statuses = [reserve(day=future(base + i), device=device)[0] for i in range(22)]
        self.assertEqual(statuses, [201] * 20 + [429] * 2)  # 20 reservas por dispositivo y hora


EXAMPLES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "contract", "examples")


class Contrato(unittest.TestCase):
    """Los ejemplos de `contract/examples` son los que usa la app en sus pruebas: si la API cambia de forma, esto falla hasta
    que se actualicen (y así la app no se queda con un contrato viejo)."""

    @staticmethod
    def example(name):
        with open(os.path.join(EXAMPLES, name), encoding="utf-8") as file:
            return json.load(file)

    def same_shape(self, example, actual, path="$"):
        if isinstance(example, dict):
            self.assertIsInstance(actual, dict, path)
            self.assertEqual(sorted(example), sorted(actual), path)
            for key in example:
                self.same_shape(example[key], actual[key], path + "." + key)
        elif isinstance(example, list):
            self.assertIsInstance(actual, list, path)
            if example and actual:
                self.same_shape(example[0], actual[0], path + "[0]")
        elif example is not None and actual is not None:  # `null` en un ejemplo = campo opcional (placeId)
            self.assertIs(type(example), type(actual), path)

    def test_catalogo(self):
        self.same_shape(self.example("offers-list.json"), call("GET", "/offers")[2])
        self.same_shape(self.example("offer.json"), call("GET", "/offers/albufera-barca")[2])
        for offer in self.example("offers-list.json")["offers"]:  # y los dos casos de placeId (con valor y nulo)
            self.same_shape(offer, call("GET", "/offers/" + offer["id"])[2], offer["id"])

    def test_errores(self):
        self.same_shape(self.example("error-offer-not-found.json"), call("GET", "/offers/no-existe")[2])

    @unittest.skipUnless(WRITES, "crea reservas de verdad (API_WRITES=1)")
    def test_reservas(self):
        request = self.example("reservation-request.json")
        self.assertEqual(sorted(request), ["adults", "children", "date", "email", "offerId", "time"])
        device = str(uuid.uuid4())
        status, _, created = reserve(offer=request["offerId"], day=random_day(), time=request["time"], adults=request["adults"], children=request["children"], device=device)
        self.assertEqual(status, 201)
        self.same_shape(self.example("reservation.json"), created)
        self.same_shape(self.example("reservations-list.json"), call("GET", "/reservations", headers={"X-Device-Id": device})[2])
        status, _, error = reserve(adults=0)
        self.assertEqual(status, 400)
        self.same_shape(self.example("error-invalid-request.json"), error)


@unittest.skipUnless(ANON_KEY, "falta SUPABASE_ANON_KEY (la clave pública del proyecto)")
class BaseDeDatosCerrada(unittest.TestCase):
    def test_la_api_automatica_no_ve_el_esquema_app(self):
        root = API_BASE.split("/functions/v1")[0]
        for table in ("offers", "offer_slots", "reservations", "rate_limits"):
            for profile in (None, "app"):
                headers = {"apikey": ANON_KEY, "Authorization": "Bearer " + ANON_KEY}
                if profile:
                    headers["Accept-Profile"] = profile
                status, _, _ = call("GET", "/rest/v1/%s?select=*" % table, headers=headers, base=root)
                self.assertIn(status, (401, 403, 404, 406), "%s con perfil %s dejó pasar la petición (%s)" % (table, profile, status))
        status, _, _ = call("POST", "/rest/v1/rpc/create_reservation", headers={"apikey": ANON_KEY, "Authorization": "Bearer " + ANON_KEY, "Content-Profile": "app"}, body={}, base=root)
        self.assertIn(status, (401, 403, 404, 406))


if __name__ == "__main__":
    print("API: %s · local=%s · completas=%s · escrituras=%s" % (API_BASE, LOCAL, FULL, WRITES))
    unittest.main(verbosity=2)
