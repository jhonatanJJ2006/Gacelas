# Plantillas de correo C2 Deporte cinetico

Plantillas HTML compatibles con clientes de correo (tablas, estilos en linea, fondo oscuro #0C1322) para pegar en Supabase Auth, en Authentication, Email Templates. No se aplican automaticamente: la configuracion remota no se modifica desde este repositorio.

| Archivo                      | Plantilla de Supabase Auth | Variables                        |
| ---------------------------- | -------------------------- | -------------------------------- |
| 01-confirmar-cuenta.html     | Confirm signup             | ConfirmationURL, Data.full_name  |
| 02-recuperar-contrasena.html | Reset password             | ConfirmationURL, Email           |
| 03-enlace-magico.html        | Magic link                 | ConfirmationURL, Token           |
| 04-cambio-correo.html        | Change email address       | ConfirmationURL, Email, NewEmail |
| 05-bienvenida.html           | Uso propio (no es de Auth) | Segun el envio                   |
| 06-recordatorio-evento.html  | Uso propio (no es de Auth) | Segun el envio                   |
| 07-resumen-semanal.html      | Uso propio (no es de Auth) | Segun el envio                   |

Las plantillas 05 a 07 quedan listas para un envio propio (por ejemplo con Resend) cuando se configure el SMTP.
