-- Funciones del esquema `app`. Las reglas de negocio (validar, evitar duplicados, plazas, localizador) y la forma del JSON de la
-- API viven aquí, dentro de la base de datos y en una sola transacción; la función de borde (api-v1) solo traduce HTTP.
-- Los errores de negocio se lanzan con `raise exception '<código>'` y la API los convierte en su respuesta HTTP:
--   offer_not_found (404) · invalid_slot, invalid_date, invalid_party, invalid_email (422) · sold_out, idempotency_key_reused (409)

-- ---------------------------------------------------------------------------------------------------------------------------
-- Ofertas
-- ---------------------------------------------------------------------------------------------------------------------------
create function app.offer_json(o app.offers) returns jsonb
language sql stable as $$
    select jsonb_build_object(
        'id', o.id,
        'kind', o.kind,
        'tag', o.tag,
        'title', o.title,
        'duration', o.duration,
        'place', o.place,
        'extra', o.extra,
        'priceFromCents', o.price_from_cents,
        'description', o.description,
        'placeId', o.place_id,
        'slots', coalesce((select jsonb_agg(s.slot order by s.slot) from app.offer_slots s where s.offer_id = o.id), '[]'::jsonb)
    )
$$;

-- Versión del catálogo (cambia cuando cambia una oferta o sus horarios): es el ETag que usa la app para cachear.
create function app.offers_version() returns text
language sql stable as $$
    select coalesce((extract(epoch from max(updated_at)) * 1000000)::bigint, 0)::text || '-' || count(*)::text
    from app.offers where active
$$;

create function app.list_offers() returns jsonb
language sql stable as $$
    select jsonb_build_object('offers', coalesce(jsonb_agg(app.offer_json(o) order by o.position, o.id), '[]'::jsonb))
    from app.offers o where o.active
$$;

create function app.get_offer(p_id text) returns jsonb
language sql stable as $$
    select app.offer_json(o) from app.offers o where o.id = p_id and o.active
$$;

-- ---------------------------------------------------------------------------------------------------------------------------
-- Reservas
-- ---------------------------------------------------------------------------------------------------------------------------
create function app.reservation_json(r app.reservations) returns jsonb
language sql stable as $$
    select jsonb_build_object(
        'id', r.id,
        'locator', r.locator,
        'offerId', r.offer_id,
        'title', r.offer_title,
        'place', r.offer_place,
        'date', to_char(r.activity_date, 'YYYY-MM-DD'),
        'time', r.slot,
        'adults', r.adults,
        'children', r.children,
        'email', r.email,
        'totalCents', r.total_cents,
        'status', r.status,
        'createdAt', to_char(r.created_at at time zone 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS"Z"')
    )
$$;

-- Crea una reserva y devuelve {replayed, reservation}. Todo ocurre en una transacción:
--  1. Si ya hay una reserva con la misma clave de idempotencia es un reintento (la red se cortó antes de recibir la respuesta):
--     se devuelve la misma, sin duplicar. Si la clave se reutiliza con otros datos, es un error.
--  2. Se validan la oferta, las personas, el correo y la fecha (la de hoy en Valencia; un horario de hoy que ya pasó no vale).
--  3. Se bloquea la fila del horario y se cuentan las plazas ocupadas: dos reservas a la vez no pueden pasarse del aforo.
--  4. El precio lo pone el servidor (precio por adulto × adultos; los niños no pagan), nunca el cliente.
create function app.create_reservation(
    p_device   uuid,
    p_key      uuid,
    p_offer    text,
    p_date     date,
    p_time     text,
    p_adults   integer,
    p_children integer,
    p_email    text
) returns jsonb
language plpgsql as $$
declare
    v_email    text := lower(btrim(coalesce(p_email, '')));
    v_today    date := (now() at time zone 'Europe/Madrid')::date;
    v_now_time time := (now() at time zone 'Europe/Madrid')::time;
    v_offer    app.offers;
    v_slot     app.offer_slots;
    v_row      app.reservations;
    v_taken    integer;
begin
    select * into v_row from app.reservations where device_id = p_device and idempotency_key = p_key;
    if found then
        if v_row.offer_id <> p_offer or v_row.activity_date <> p_date or v_row.slot <> p_time
           or v_row.adults <> p_adults or v_row.children <> p_children or v_row.email <> v_email then
            raise exception 'idempotency_key_reused';
        end if;
        return jsonb_build_object('replayed', true, 'reservation', app.reservation_json(v_row));
    end if;

    select * into v_offer from app.offers where id = p_offer and active;
    if not found then
        raise exception 'offer_not_found';
    end if;
    if p_adults is null or p_children is null or p_adults < 1 or p_children < 0 or p_adults + p_children > 20 then
        raise exception 'invalid_party';
    end if;
    if length(v_email) > 254 or v_email !~ '^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$' then
        raise exception 'invalid_email';
    end if;
    if p_date is null or p_date < v_today or p_date > v_today + 365 then
        raise exception 'invalid_date';
    end if;

    select * into v_slot from app.offer_slots where offer_id = p_offer and slot = p_time for update;
    if not found then
        raise exception 'invalid_slot';
    end if;
    if p_date = v_today and v_slot.slot ~ '^[0-9]{2}:[0-9]{2}$' and v_slot.slot::time <= v_now_time then
        raise exception 'invalid_date';
    end if;
    if v_slot.capacity is not null then
        select coalesce(sum(adults + children), 0) into v_taken
        from app.reservations
        where offer_id = p_offer and activity_date = p_date and slot = p_time and status = 'confirmed';
        if v_taken + p_adults + p_children > v_slot.capacity then
            raise exception 'sold_out';
        end if;
    end if;

    begin
        insert into app.reservations (number, locator, offer_id, offer_title, offer_place, activity_date, slot,
                                      adults, children, email, total_cents, device_id, idempotency_key)
        select n,
               'TV-' || extract(year from p_date)::int || '-' || n,
               v_offer.id, v_offer.title, v_offer.place, p_date, p_time,
               p_adults, p_children, v_email, v_offer.price_from_cents * p_adults, p_device, p_key
        from (select nextval('app.reservation_number_seq')::int as n) as next_number
        returning * into v_row;
    exception when unique_violation then
        -- Otra petición con la misma clave se coló entre la comprobación y el alta: se devuelve la que ya se guardó.
        select * into v_row from app.reservations where device_id = p_device and idempotency_key = p_key;
        if found then
            return jsonb_build_object('replayed', true, 'reservation', app.reservation_json(v_row));
        end if;
        raise;
    end;

    return jsonb_build_object('replayed', false, 'reservation', app.reservation_json(v_row));
end;
$$;

-- Las reservas de un dispositivo, las más recientes primero.
create function app.list_reservations(p_device uuid) returns jsonb
language sql stable as $$
    select jsonb_build_object('reservations', coalesce(jsonb_agg(app.reservation_json(r) order by r.created_at desc, r.number desc), '[]'::jsonb))
    from (select * from app.reservations where device_id = p_device order by created_at desc, number desc limit 100) r
$$;

-- Una reserva por localizador y correo (sirve para recuperarla en otro móvil). Si el correo no coincide, no devuelve nada.
create function app.find_reservation(p_locator text, p_email text) returns jsonb
language sql stable as $$
    select app.reservation_json(r)
    from app.reservations r
    where r.locator = upper(btrim(p_locator)) and r.email = lower(btrim(p_email))
$$;

-- ---------------------------------------------------------------------------------------------------------------------------
-- Límite de peticiones: cuenta una petición en la ventana actual y dice si todavía está dentro del límite
-- ---------------------------------------------------------------------------------------------------------------------------
create function app.hit_rate_limit(p_bucket text, p_limit integer, p_window_seconds integer) returns boolean
language plpgsql as $$
declare
    v_window timestamptz := to_timestamp(floor(extract(epoch from now()) / p_window_seconds) * p_window_seconds);
    v_hits   integer;
begin
    insert into app.rate_limits as r (bucket, window_start, hits)
    values (p_bucket, v_window, 1)
    on conflict (bucket, window_start) do update set hits = r.hits + 1
    returning r.hits into v_hits;

    -- Limpieza ocasional de contadores viejos, para que la tabla no crezca.
    if random() < 0.02 then
        delete from app.rate_limits where window_start < now() - interval '2 days';
    end if;
    return v_hits <= p_limit;
end;
$$;

-- Nadie salvo el propietario puede ejecutarlas: la API pública no ve este esquema.
revoke all on all functions in schema app from public, anon, authenticated;
