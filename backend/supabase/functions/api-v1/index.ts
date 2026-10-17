// API de Turismo Valencia (v1): ofertas y reservas. La lógica está en router.ts y en las funciones SQL de las migraciones.
import { handle } from "./router.ts";

Deno.serve(handle);
