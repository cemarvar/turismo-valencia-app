import postgres from "npm:postgres@3.4.5";

export type Sql = ReturnType<typeof postgres>;

let client: Sql | undefined;

/**
 * Conexión a la base de datos de Supabase. Por defecto `SUPABASE_DB_URL`, que Supabase pone en cada función (es una conexión de
 * propietario: por eso la API pública no ve el esquema `app` y todo pasa por esta función). Si existe `API_DB_URL` se usa esa:
 * sirve en local (ver config.toml) y en producción para conectar con un rol de permisos mínimos en vez del propietario.
 * `prepare: false` es necesario con el pooler en modo transacción, y `max: 1` porque cada instancia atiende pocas peticiones a la vez.
 */
export function db(): Sql {
  if (!client) {
    const url = Deno.env.get("API_DB_URL") ?? Deno.env.get("SUPABASE_DB_URL");
    if (!url) throw new Error("Falta SUPABASE_DB_URL");
    client = postgres(url, { max: 1, prepare: false, idle_timeout: 20, connect_timeout: 10 });
  }
  return client;
}
