import { ApiError, json } from "../_shared/http.ts";
import type { Ctx } from "./context.ts";
import { OFFER_ID } from "./validate.ts";

// El catálogo cambia poco: se puede cachear un minuto y la app revalida con el ETag (la versión del catálogo).
const CACHE = "public, max-age=60";

function matches(header: string | null, etag: string): boolean {
  return header !== null && header.split(",").some((token) => token.trim().replace(/^W\//, "") === etag);
}

/** GET /offers → {"offers": [...]} con ETag; con `If-None-Match` igual devuelve 304 sin cuerpo. */
export async function listOffers({ req, sql }: Ctx): Promise<Response> {
  const [{ version }] = await sql`select app.offers_version() as version`;
  const etag = `"${version}"`;
  const headers = { etag, "cache-control": CACHE };
  if (matches(req.headers.get("if-none-match"), etag)) return new Response(null, { status: 304, headers });
  const [{ body }] = await sql`select app.list_offers() as body`;
  return json(200, body, headers);
}

/** GET /offers/:id */
export async function getOffer({ params, sql }: Ctx): Promise<Response> {
  const id = params.id;
  if (!OFFER_ID.test(id)) throw new ApiError(404, "offer_not_found", "La oferta no existe o ya no está disponible.");
  const [{ body }] = await sql`select app.get_offer(${id}) as body`;
  if (body === null) throw new ApiError(404, "offer_not_found", "La oferta no existe o ya no está disponible.");
  return json(200, body, { "cache-control": CACHE });
}
