-- Perfiles: RLS activa y las columnas sensibles sin SELECT para anon ni authenticated.
-- El esqueleto anterior llamaba plan()/ok() de pgTAP sin crear la extension, asi que
-- con ON_ERROR_STOP el archivo fallaba antes de comprobar nada.
-- Quien ve bio, edad, genero y suspension lo prueba 042_perfiles_privacidad.sql.
DO $$
DECLARE rol text;
BEGIN
  IF NOT (SELECT c.relrowsecurity FROM pg_class c WHERE c.oid = 'identidad.profiles'::regclass) THEN
    RAISE EXCEPTION 'identidad.profiles sin RLS';
  END IF;
  FOREACH rol IN ARRAY ARRAY['anon', 'authenticated'] LOOP
    IF has_column_privilege(rol, 'identidad.profiles', 'bio', 'SELECT')
       OR has_column_privilege(rol, 'identidad.profiles', 'age', 'SELECT')
       OR has_column_privilege(rol, 'identidad.profiles', 'gender', 'SELECT')
       OR has_column_privilege(rol, 'identidad.profiles', 'suspended_at', 'SELECT') THEN
      RAISE EXCEPTION '% puede leer columnas sensibles de identidad.profiles', rol;
    END IF;
  END LOOP;
END $$;
