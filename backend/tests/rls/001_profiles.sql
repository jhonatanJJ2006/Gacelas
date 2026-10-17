-- pgTAP: perfiles visibles segun privacidad (esqueleto; ejecutar con extension pgtap en CI).
BEGIN;
SELECT plan(1);
SELECT ok(true, 'placeholder: conectar pgTAP y usuarios de prueba en CI');
SELECT finish();
ROLLBACK;
