import { ApiError, fromDatabaseError, json } from "../_shared/http.ts";
import { enforce } from "../_shared/limits.ts";
import type { Ctx } from "./context.ts";
import { LOCATOR, parseReservation, readJsonBody, requireUuidHeader } from "./validate.ts";

// Las reservas no se guardan en ninguna caché.
const NO_STORE = { "cache-control": "no-store" };

/**
 * POST /reservations — cabeceras `X-Device-Id` (UUID del móvil) e `Idempotency-Key` (UUID de este intento).
 * 201 con la reserva; si es un reintento de uno ya guardado, 200 con la misma reserva y `Idempotent-Replayed: true`.
 */
export async function createReservation({ req, sql, ip }: Ctx): Promise<Response> {
  await enforce(sql, "ip-reserve", ip, 30, 3600);
  const device = requireUuidHeader(req, "x-device-id");
  const key = requireUuidHeader(req, "idempotency-key");
  const input = parseReservation(await readJsonBody(req));
  await enforce(sql, "device-reserve", device, 20, 3600);
  await enforce(sql, "email-reserve", input.email.toLowerCase(), 10, 86400);

  let result: { replayed: boolean; reservation: unknown };
  try {
    const [row] = await sql`
      select app.create_reservation(
        ${device}::uuid, ${key}::uuid, ${input.offerId}, ${input.date}::date, ${input.time},
        ${input.adults}::int, ${input.children}::int, ${input.email}
      ) as result`;
    result = row.result;
  } catch (error) {
    throw fromDatabaseError(error);
  }
  return json(result.replayed ? 200 : 201, result.reservation, {
    ...NO_STORE,
    ...(result.replayed ? { "idempotent-replayed": "true" } : {}),
  });
}

/** GET /reservations — las del dispositivo (`X-Device-Id`), las más recientes primero. */
export async function listReservations({ req, sql }: Ctx): Promise<Response> {
  const device = requireUuidHeader(req, "x-device-id");
  await enforce(sql, "device-list", device, 120, 3600);
  const [{ body }] = await sql`select app.list_reservations(${device}::uuid) as body`;
  return json(200, body, NO_STORE);
}

/**
 * GET /reservations/:locator?email=… — recupera una reserva con su localizador y el correo con el que se hizo. Si no coincide,
 * responde igual que si no existiera (así no se puede averiguar qué localizadores existen).
 */
export async function findReservation({ params, url, sql, ip }: Ctx): Promise<Response> {
  await enforce(sql, "ip-lookup", ip, 20, 3600);
  const locator = params.locator.toUpperCase();
  const email = url.searchParams.get("email")?.trim() ?? "";
  if (!LOCATOR.test(locator) || email.length === 0 || email.length > 254) {
    throw new ApiError(404, "reservation_not_found", "No hay ninguna reserva con ese localizador y ese correo.");
  }
  const [{ body }] = await sql`select app.find_reservation(${locator}, ${email}) as body`;
  if (body === null) throw new ApiError(404, "reservation_not_found", "No hay ninguna reserva con ese localizador y ese correo.");
  return json(200, body, NO_STORE);
}
