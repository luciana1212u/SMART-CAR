-- SMART-CAR: ajustes para el registro con Supabase Auth.
-- Ejecutar UNA vez en Supabase -> SQL Editor. Es idempotente (se puede repetir sin problema).
-- Solo AGREGA: una funcion nueva y un indice nuevo. NO crea ni modifica tablas, NO toca el
-- trigger al_crear_usuario, NO cambia politicas RLS y NO borra datos.
-- (VS Code puede marcar errores de sintaxis si abre el archivo como T-SQL; es PostgreSQL.)

-- 1) Comprobacion de nombre de usuario libre, usada por el backend ANTES de registrar.
--    - Devuelve solo true/false: no expone filas de perfiles.
--    - SECURITY DEFINER: se ejecuta con permisos del dueno (postgres), asi funciona aunque la
--      politica RLS solo deje ver el perfil propio.
--    - search_path vacio + nombres calificados: impide que alguien "secuestre" la funcion
--      creando objetos con el mismo nombre en otro esquema.
create or replace function public.usuario_disponible(p_usuario text)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select not exists (
    select 1
    from public.perfiles p
    where pg_catalog.lower(p.usuario) = pg_catalog.lower(p_usuario)
  );
$$;

-- Permisos: nadie por defecto; solo los roles que usa la API con la clave publicable.
revoke all on function public.usuario_disponible(text) from public;
revoke all on function public.usuario_disponible(text) from anon, authenticated;
grant execute on function public.usuario_disponible(text) to anon, authenticated;

-- 2) Garantia real de unicidad sin distinguir mayusculas (evita carreras entre dos registros).
--    Es un indice nuevo: no modifica la tabla ni sus restricciones actuales (un UNIQUE que ya
--    tengas sigue funcionando; este solo agrega la comparacion en minusculas).
--    Se crea solo si perfiles existe y NO hay ya usuarios repetidos ignorando mayusculas;
--    si los hay, avisa en lugar de fallar y no cambia nada.
do $$
begin
  if to_regclass('public.perfiles') is null then
    raise notice 'Omitido: no existe public.perfiles';
  elsif exists (
    select 1 from public.perfiles
    group by pg_catalog.lower(usuario)
    having count(*) > 1
  ) then
    raise notice 'Omitido: hay nombres de usuario repetidos ignorando mayusculas en perfiles. Corrigelos y vuelve a ejecutar';
  else
    create unique index if not exists perfiles_usuario_unico
      on public.perfiles (pg_catalog.lower(usuario));
  end if;
end
$$;

-- 3) Comprobaciones de SOLO LECTURA (opcionales; ejecutalas por separado):
--
-- a) Que existan la funcion, el indice y el trigger:
--    select proname, prosecdef, proconfig from pg_proc where proname = 'usuario_disponible';
--    select indexname from pg_indexes where indexname = 'perfiles_usuario_unico';
--    select tgname from pg_trigger where tgname = 'al_crear_usuario' and not tgisinternal;
--
-- b) Restricciones actuales de perfiles (por si ya validan el formato del usuario):
--    select conname, pg_get_constraintdef(oid) from pg_constraint
--    where conrelid = 'public.perfiles'::regclass;
--
-- c) Quien puede ejecutar la funcion (debe incluir anon y authenticated, no public):
--    select grantee, privilege_type from information_schema.routine_privileges
--    where routine_name = 'usuario_disponible';
--
-- d) Probar la funcion (true = libre):
--    select public.usuario_disponible('un_usuario_que_no_existe');
