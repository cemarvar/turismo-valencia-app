import { ApiError } from "../_shared/http.ts";

// Aquí solo se comprueba la forma de lo que llega (tipos y formatos). Las reglas de negocio (fecha válida, horario de la oferta,
// plazas, correo) las comprueba la base de datos en la misma transacción que guarda la reserva.

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const DATE = /^\d{4}-\d{2}-\d{2}$/;
const TIME = /^(?:[01]\d|2[0-3]):[0-5]\d$/;

export const OFFER_ID = /^[a-z0-9]+(-[a-z0-9]+)*$/;
export const LOCATOR = /^TV-\d{4}-\d{4,}$/;
export const MAX_BODY_BYTES = 4096;

export interface ReservationInput {
  offerId: string;
  date: string;
  time: string;
  adults: number;
  children: number;
  email: string;
}

/** Cabecera con un UUID (el identificador anónimo del móvil o la clave de idempotencia de un intento). */
export function requireUuidHeader(req: Request, name: string): string {
  const value = req.headers.get(name)?.trim() ?? "";
  if (!UUID.test(value)) throw new ApiError(400, "invalid_request", `La cabecera ${name} tiene que ser un UUID.`);
  return value.toLowerCase();
}

export async function readJsonBody(req: Request): Promise<unknown> {
  if (Number(req.headers.get("content-length") ?? 0) > MAX_BODY_BYTES) {
    throw new ApiError(413, "payload_too_large", "El cuerpo de la petición es demasiado grande.");
  }
  const text = await req.text();
  if (new TextEncoder().encode(text).length > MAX_BODY_BYTES) {
    throw new ApiError(413, "payload_too_large", "El cuerpo de la petición es demasiado grande.");
  }
  try {
    return JSON.parse(text);
  } catch {
    throw new ApiError(400, "invalid_request", "El cuerpo no es un JSON válido.");
  }
}

function isRealDate(value: string): boolean {
  if (!DATE.test(value)) return false;
  const date = new Date(`${value}T00:00:00Z`);
  return !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === value;
}

/** Comprueba el cuerpo de «crear reserva» y lo devuelve ya con los tipos buenos; si algo no encaja, 400 con los campos que fallan. */
export function parseReservation(raw: unknown): ReservationInput {
  if (raw === null || typeof raw !== "object" || Array.isArray(raw)) {
    throw new ApiError(400, "invalid_request", "El cuerpo tiene que ser un objeto JSON.");
  }
  const body = raw as Record<string, unknown>;
  const problems: string[] = [];

  const text = (key: string, accept: (value: string) => boolean, hint: string): string => {
    const value = body[key];
    if (typeof value !== "string" || !accept(value.trim())) {
      problems.push(`${key} (${hint})`);
      return "";
    }
    return value.trim();
  };
  const count = (key: string, min: number, max: number, fallback?: number): number => {
    const value = body[key] === undefined && fallback !== undefined ? fallback : body[key];
    if (typeof value !== "number" || !Number.isInteger(value) || value < min || value > max) {
      problems.push(`${key} (entero entre ${min} y ${max})`);
      return min;
    }
    return value;
  };

  const input: ReservationInput = {
    offerId: text("offerId", (v) => OFFER_ID.test(v), "identificador de la oferta"),
    date: text("date", isRealDate, "fecha AAAA-MM-DD"),
    time: text("time", (v) => TIME.test(v) || v === "Cualquier hora", "horario HH:MM o «Cualquier hora»"),
    adults: count("adults", 1, 20),
    children: count("children", 0, 20, 0),
    email: text("email", (v) => v.length > 0 && v.length <= 254, "correo de hasta 254 caracteres"),
  };
  if (problems.length > 0) throw new ApiError(400, "invalid_request", `Datos incorrectos: ${problems.join(", ")}.`);
  return input;
}
