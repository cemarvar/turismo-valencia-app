import { ApiError } from "./http.ts";
import type { Sql } from "./db.ts";

/**
 * IP de quien llama. Detrás de Supabase (Cloudflare) la fiable es `cf-connecting-ip`; si no está se usa la última de
 * `x-forwarded-for`, que es la que añade el último proxy y no la que puede escribir el cliente. Solo en local
 * (`API_ENV=local`) se usa la primera, la que manda el cliente, para que las pruebas puedan simular IPs distintas.
 */
export function clientIp(req: Request): string {
  const cloudflare = req.headers.get("cf-connecting-ip");
  if (cloudflare) return cloudflare.trim();
  const forwarded = req.headers.get("x-forwarded-for");
  if (forwarded) {
    const addresses = forwarded.split(",").map((a) => a.trim());
    return Deno.env.get("API_ENV") === "local" ? addresses[0] : addresses[addresses.length - 1];
  }
  return "desconocida";
}

// Los contadores no guardan IPs ni correos tal cual, solo un resumen con sal.
async function digest(value: string): Promise<string> {
  const salt = Deno.env.get("RATE_LIMIT_SALT") ?? "turismo-valencia-local";
  const bytes = new TextEncoder().encode(`${salt}:${value}`);
  const hash = new Uint8Array(await crypto.subtle.digest("SHA-256", bytes));
  return Array.from(hash.slice(0, 16), (b) => b.toString(16).padStart(2, "0")).join("");
}

/** Cuenta una petición de `subject` (una IP, un dispositivo, un correo…) y responde 429 si ya pasó de `limit` en la ventana. */
export async function enforce(sql: Sql, kind: string, subject: string, limit: number, windowSeconds: number): Promise<void> {
  const bucket = `${kind}:${await digest(subject)}:${windowSeconds}`;
  const [{ ok }] = await sql`select app.hit_rate_limit(${bucket}, ${limit}::int, ${windowSeconds}::int) as ok`;
  if (!ok) {
    const retryAfter = windowSeconds - (Math.floor(Date.now() / 1000) % windowSeconds);
    throw new ApiError(429, "rate_limited", "Demasiadas peticiones. Inténtalo de nuevo más tarde.", { "retry-after": String(retryAfter) });
  }
}
