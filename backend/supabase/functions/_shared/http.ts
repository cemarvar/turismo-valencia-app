// Respuestas HTTP de la API: JSON siempre, y un único formato de error {"error": {"code", "message"}}.
// La app decide el texto que enseña a partir de `code`; `message` es una pista para quien depura.

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
    readonly headers: Record<string, string> = {},
  ) {
    super(message);
  }
}

const BASE_HEADERS = {
  "content-type": "application/json; charset=utf-8",
  "x-content-type-options": "nosniff",
};

export function json(status: number, body: unknown, headers: Record<string, string> = {}): Response {
  return new Response(JSON.stringify(body), { status, headers: { ...BASE_HEADERS, ...headers } });
}

export function errorResponse(error: ApiError): Response {
  return json(error.status, { error: { code: error.code, message: error.message } }, error.headers);
}

// Errores de negocio que lanzan las funciones SQL con `raise exception '<código>'` (ver las migraciones).
const DOMAIN_ERRORS: Record<string, [number, string]> = {
  offer_not_found: [404, "La oferta no existe o ya no está disponible."],
  invalid_slot: [422, "Ese horario no existe para esta oferta."],
  invalid_date: [422, "La fecha tiene que ser de hoy a un año y, si es hoy, un horario que no haya pasado."],
  invalid_party: [422, "Tiene que haber al menos 1 adulto y, entre adultos y niños, 20 personas como mucho."],
  invalid_email: [422, "El correo no es válido."],
  sold_out: [409, "Ya no quedan plazas para ese día y horario."],
  idempotency_key_reused: [409, "Esa clave de idempotencia ya se usó con otros datos."],
  reservation_not_found: [404, "No hay ninguna reserva con ese localizador para este dispositivo o este correo."],
  reservation_past: [409, "Esa reserva ya no se puede cancelar: la actividad ya empezó o pasó."],
};

/** Convierte un error de la base de datos en una respuesta: los de negocio tienen su código; el resto es un 500 sin detalles. */
export function fromDatabaseError(error: unknown): ApiError {
  const e = error as { code?: string; message?: string };
  if (e?.code === "P0001" && e.message && e.message in DOMAIN_ERRORS) {
    const [status, message] = DOMAIN_ERRORS[e.message];
    return new ApiError(status, e.message, message);
  }
  // Se registra el tipo de fallo, nunca los datos de la petición (llevan correos).
  console.error("error de base de datos", e?.code ?? "sin código", e?.message ?? "");
  return new ApiError(500, "internal_error", "Ha ocurrido un error en el servidor.");
}
