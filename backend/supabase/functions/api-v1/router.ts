import { db } from "../_shared/db.ts";
import { ApiError, errorResponse } from "../_shared/http.ts";
import { clientIp, enforce } from "../_shared/limits.ts";
import type { Handler } from "./context.ts";
import { getOffer, listOffers } from "./offers.ts";
import { createReservation, findReservation, listReservations } from "./reservations.ts";

interface Route {
  method: string;
  pattern: URLPattern;
  handler: Handler;
}

const route = (method: string, pathname: string, handler: Handler): Route => ({ method, pattern: new URLPattern({ pathname }), handler });

// API v1. La dirección completa es <proyecto>.supabase.co/functions/v1/api-v1/<ruta>; una v2 incompatible sería otra función (api-v2),
// así las apps ya instaladas siguen funcionando.
const routes: Route[] = [
  route("GET", "/offers", listOffers),
  route("GET", "/offers/:id", getOffer),
  route("POST", "/reservations", createReservation),
  route("GET", "/reservations", listReservations),
  route("GET", "/reservations/:locator", findReservation),
];

export async function handle(req: Request): Promise<Response> {
  try {
    const url = new URL(req.url);
    const path = url.pathname.replace(/^\/api-v1/, "").replace(/\/+$/, "") || "/";
    const otherMethods: string[] = [];
    for (const candidate of routes) {
      const match = candidate.pattern.exec({ pathname: path });
      if (!match) continue;
      if (candidate.method !== req.method) {
        otherMethods.push(candidate.method);
        continue;
      }
      const sql = db();
      const ip = clientIp(req);
      await enforce(sql, "ip", ip, 240, 60); // tope general por IP: 240 peticiones por minuto
      return await candidate.handler({ req, url, params: match.pathname.groups as Record<string, string>, sql, ip });
    }
    if (otherMethods.length > 0) {
      throw new ApiError(405, "method_not_allowed", "Método no permitido.", { allow: otherMethods.join(", ") });
    }
    throw new ApiError(404, "not_found", "No existe esa ruta.");
  } catch (error) {
    if (error instanceof ApiError) return errorResponse(error);
    console.error("error no controlado:", error instanceof Error ? error.message : String(error));
    return errorResponse(new ApiError(500, "internal_error", "Ha ocurrido un error en el servidor."));
  }
}
