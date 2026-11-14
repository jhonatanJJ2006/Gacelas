Pruebas RLS en SQL. El workflow `db.yml` aplica las migraciones y ejecuta estos scripts con `psql -v ON_ERROR_STOP=1` en Postgres efimero: un error de migracion o un `RAISE EXCEPTION` rompe el job.
