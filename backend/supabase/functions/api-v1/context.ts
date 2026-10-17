import type { Sql } from "../_shared/db.ts";

/** Lo que recibe cada ruta: la petición, los parámetros de la ruta (`:id`…), la base de datos y la IP de quien llama. */
export interface Ctx {
  req: Request;
  url: URL;
  params: Record<string, string>;
  sql: Sql;
  ip: string;
}

export type Handler = (ctx: Ctx) => Promise<Response>;
