import { ApiError, fromDatabaseError, json } from "../_shared/http.ts";
import { enforce } from "../_shared/limits.ts";
import type { Ctx } from "./context.ts";
import { LOCATOR, optionalUuidHeader, parseReservation, readJsonBody, readOptionalJsonBody, requireUuidHeader } from "./validate.ts";

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

/** GET /reservations — las del dispositivo (`X-Device-Id`: las que hizo y las que ha recuperado), las más recientes primero. */
export async function listReservations({ req, sql }: Ctx): Promise<Response> {
  const device = requireUuidHeader(req, "x-device-id");
  await enforce(sql, "device-list", device, 120, 3600);
  const [{ body }] = await sql`select app.list_reservations(${device}::uuid) as body`;
  return json(200, body, NO_STORE);
}

/**
 * GET /reservations/:locator?email=… — recupera una reserva con su localizador y el correo con el que se hizo. Si no coincide,
 * responde igual que si no existiera (así no se puede averiguar qué localizadores existen). Con la cabecera opcional `X-Device-Id`
 * (el UUID de quien la recupera), la reserva queda apuntada a ese móvil y su `GET /reservations` la trae desde entonces, así que se
 * pone al día sola; sin ella, solo se ve.
 */
export async function findReservation({ req, params, url, sql, ip }: Ctx): Promise<Response> {
  await enforce(sql, "ip-lookup", ip, 20, 3600);
  const device = optionalUuidHeader(req, "x-device-id");
  const locator = params.locator.toUpperCase();
  const email = url.searchParams.get("email")?.trim() ?? "";
  if (!LOCATOR.test(locator) || email.length === 0 || email.length > 254) {
    throw new ApiError(404, "reservation_not_found", "No hay ninguna reserva con ese localizador y ese correo.");
  }
  const [{ body }] = await sql`select app.find_reservation(${locator}, ${email}, ${device}::uuid) as body`;
  if (body === null) throw new ApiError(404, "reservation_not_found", "No hay ninguna reserva con ese localizador y ese correo.");
  return json(200, body, NO_STORE);
}

/**
 * POST /reservations/:locator/cancel — cancela la reserva. Cabecera `X-Device-Id` (UUID del móvil) y, opcional, un cuerpo
 * `{ "email": "…" }`: puede cancelarla el móvil que la hizo o quien tenga el localizador y el correo (la recuperó en otro móvil).
 * 200 con la reserva ya cancelada; cancelar una que ya lo estaba también es 200 con la misma. 404 `reservation_not_found` si no existe
 * o no es de quien lo pide (igual que si no existiera) y 409 `reservation_past` si la actividad ya empezó o pasó.
 */
export async function cancelReservation({ req, params, sql, ip }: Ctx): Promise<Response> {
  await enforce(sql, "ip-cancel", ip, 30, 3600);
  const device = requireUuidHeader(req, "x-device-id");
  const body = await readOptionalJsonBody(req);
  if (body !== null && (typeof body !== "object" || Array.isArray(body))) {
    throw new ApiError(400, "invalid_request", "El cuerpo tiene que ser un objeto JSON.");
  }
  const rawEmail = (body as Record<string, unknown> | null)?.email;
  if (rawEmail !== undefined && rawEmail !== null && (typeof rawEmail !== "string" || rawEmail.trim().length > 254)) {
    throw new ApiError(400, "invalid_request", "Datos incorrectos: email (texto de hasta 254 caracteres).");
  }
  await enforce(sql, "device-cancel", device, 20, 3600);

  const locator = params.locator.toUpperCase();
  if (!LOCATOR.test(locator)) throw new ApiError(404, "reservation_not_found", "No hay ninguna reserva con ese localizador para este dispositivo o este correo.");
  let result: { changed: boolean; reservation: unknown };
  try {
    const [row] = await sql`select app.cancel_reservation(${device}::uuid, ${locator}, ${typeof rawEmail === "string" ? rawEmail : ""}) as result`;
    result = row.result;
  } catch (error) {
    throw fromDatabaseError(error);
  }
  return json(200, result.reservation, NO_STORE);
}
