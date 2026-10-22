-- Derecho de supresión: borra todas las reservas de un correo (y, en cascada, los apuntes de los móviles que las recuperaron).
-- Se ejecuta en el editor SQL del panel de Supabase (SQL Editor). Cambia el correo y ejecuta; devuelve cuántas se borraron.

with borradas as (
    delete from app.reservations
    where email = lower(btrim('correo@ejemplo.com'))
    returning 1
)
select count(*) as reservas_borradas from borradas;

-- Retención (opcional, para hacer de vez en cuando): deja anónimas las reservas con más de un año, conservando el resto del dato
-- (qué se reservó y cuándo) para estadísticas pero sin el correo.
-- update app.reservations
--    set email = 'anonimo-' || id || '@borrado.invalid'
--  where activity_date < current_date - interval '1 year' and email not like 'anonimo-%';
