-- Datos SOLO para pruebas en local (`npm run reset` los carga). El despliegue a producción no los incluye.
-- Una oferta con aforo pequeño y horarios límite para probar plazas, concurrencia y «horario de hoy ya pasado».

insert into app.offers (id, kind, tag, title, duration, place, extra, price_from_cents, description, place_id, position)
values ('prueba-aforo', 'tour', 'Pruebas', 'Oferta de pruebas con aforo', '1 h', 'Pruebas', '', 1000,
        'Solo existe en el entorno local: sirve para probar las plazas y los horarios.', null, 999);

insert into app.offer_slots (offer_id, slot, capacity) values
    ('prueba-aforo', '10:00', 3),   -- aforo de 3 personas
    ('prueba-aforo', '11:00', 3),   -- aforo de 3 personas (pruebas con peticiones simultáneas)
    ('prueba-aforo', '12:00', null),
    ('prueba-aforo', '00:01', null), -- siempre pasado si la fecha es hoy
    ('prueba-aforo', '23:59', null); -- siempre futuro si la fecha es hoy
