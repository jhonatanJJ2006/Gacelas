# Presupuesto de costos de producción - Gacelas

Estimación de costos para operar Gacelas (Android, iOS, web y backend Supabase) en producción. Precios en USD, sin IVA salvo donde se indica. Precios verificados en las fuentes oficiales el **13 de octubre de 2026**. Todo valor marcado como _(estimado)_ no proviene de una tabla de precios oficial o depende de supuestos de uso.

Documentos relacionados: [ARQUITECTURA.md](ARQUITECTURA.md), [FREE_TIER_BUDGET.md](FREE_TIER_BUDGET.md) (análisis anterior de límites de Supabase Free).

## Resumen

| Escenario (MAU)    | Mensual (aprox.) | Anual (aprox.) |
| ------------------ | ---------------- | -------------- |
| Presupuesto gratis | USD 0            | USD 0 (\*)     |
| Producción ~1.000  | USD 98           | USD 1.178      |
| Producción ~10.000 | USD 154          | USD 1.853      |
| Producción ~50.000 | USD 273          | USD 3.278      |

(\*) Publicar en las tiendas exige igualmente Google Play (USD 25 una vez) y Apple Developer Program (USD 99/año).

**Recomendación principal:** mantener MapLibre + OpenFreeMap (con descarga offline de Loja) y no migrar a Google Maps. En móvil Google Maps SDK es gratis, pero en web el SKU Dynamic Maps cuesta USD 7 por 1.000 cargas sobre 10.000 gratis, y la migración rompería el mapa offline y el código compartido de MapLibre. Como respaldo comercial con SLA, preparar MapTiler Flex (USD 30/mes) o Stadia Starter (USD 20/mes).

## Supuestos de uso

| Supuesto                                       | Valor _(estimado)_                           |
| ---------------------------------------------- | -------------------------------------------- |
| Carreras con GPS por usuario activo al mes     | 12                                           |
| Tamaño de una carrera (puntos GPS + metadatos) | ~60 KB (ver `FREE_TIER_BUDGET.md`)           |
| Crecimiento de base de datos por MAU al mes    | ~0,72 MB                                     |
| Egress API (feeds, eventos, clubes, carreras)  | ~15 MB por MAU al mes                        |
| Descargas de APK desde Storage                 | 10 % de MAU al mes, ~30 MB cada una          |
| Cargas de mapa en móvil                        | ~20 por MAU al mes                           |
| Usuarios que usan la web                       | 20 % de MAU, ~10 cargas de mapa cada uno     |
| Ejecuciones de CI macOS (repo privado)         | ~600 min/mes                                 |
| Personas con correo corporativo                | 1                                            |
| Login                                          | Google/Apple (pocos correos transaccionales) |

## Precios verificados por servicio

### Mapas

Fuente Google: <https://developers.google.com/maps/billing-and-pricing/pricing> (consultado 13-oct-2026). Precios por 1.000 eventos, tramo "límite gratis hasta 100.000".

| SKU Google Maps Platform               | Gratis al mes | Precio por 1.000 (siguiente tramo) |
| -------------------------------------- | ------------- | ---------------------------------- |
| Maps SDK (Android/iOS, cargas móviles) | Ilimitado     | USD 0                              |
| Dynamic Maps (web, JavaScript)         | 10.000        | USD 7,00                           |
| Static Maps                            | 10.000        | USD 2,00                           |
| Routes: Compute Routes Essentials      | 10.000        | USD 5,00                           |
| Directions (legacy)                    | 10.000        | USD 5,00                           |
| Geocoding                              | 10.000        | USD 5,00                           |
| Places Autocomplete Requests           | 10.000        | USD 2,83                           |
| Places Place Details Essentials        | 10.000        | USD 5,00                           |
| Places Text Search Pro                 | 5.000         | USD 32,00                          |
| Elevation                              | 5.000         | USD 5,00                           |

Alternativas (consultado 13-oct-2026):

| Proveedor                                           | Plan gratis                                                       | Plan comercial más barato                                                         |
| --------------------------------------------------- | ----------------------------------------------------------------- | --------------------------------------------------------------------------------- |
| [OpenFreeMap](https://openfreemap.org/) (actual)    | Sin límites, sin API key, uso comercial permitido, atribución OSM | No hay plan de pago; sin SLA ni soporte (financiado por donaciones)               |
| [MapTiler](https://www.maptiler.com/cloud/pricing/) | 5.000 sesiones, 100.000 requests/mes; no comercial                | Flex USD 30/mes: 25.000 sesiones, 500.000 requests; extra USD 2,50/1.000 sesiones |
| [Stadia Maps](https://stadiamaps.com/pricing)       | 200.000 créditos/mes; no comercial                                | Starter USD 20/mes: 1.000.000 créditos (1 tile vectorial = 1 crédito)             |
| [Mapbox](https://www.mapbox.com/pricing)            | Maps SDK móvil: primer tramo hasta 25.000 MAU sin costo           | Precio por MAU sobre 25.000 no visible en la página _(no verificado)_             |

### Backend: Supabase

Fuente: <https://supabase.com/pricing> (consultado 13-oct-2026).

| Recurso         | Free                          | Pro (USD 25/mes)                                     |
| --------------- | ----------------------------- | ---------------------------------------------------- |
| MAU             | 50.000                        | 100.000, luego USD 0,00325/MAU                       |
| Base de datos   | 500 MB                        | 8 GB disco, luego USD 0,125/GB                       |
| Egress          | 5 GB                          | 250 GB, luego USD 0,09/GB                            |
| Cached egress   | 5 GB                          | 250 GB, luego USD 0,03/GB                            |
| Storage         | 1 GB, archivos de hasta 50 MB | 100 GB, luego USD 0,0213/GB                          |
| Realtime        | 200 conexiones, 2 M mensajes  | 500 conexiones (+USD 10/1.000), 5 M mensajes         |
| Edge Functions  | 500.000 invocaciones          | 2 M, luego USD 2/millón                              |
| Backups         | No                            | Diarios, 7 días                                      |
| Pausa           | Tras 1 semana de inactividad  | Nunca                                                |
| Compute         | Compartido (500 MB RAM)       | Créditos USD 10 = Micro; Small USD 15, Medium USD 60 |
| PITR (opcional) | No                            | USD 100/mes por 7 días de retención                  |

Nota: el APK pesa cerca del límite de 50 MB por archivo del plan Free; si lo supera, la subida a Storage falla.

### Web: Vercel

Fuente: <https://vercel.com/pricing> (consultado 13-oct-2026). **Hobby es solo para uso personal y no comercial** (FAQ de la página). Pro: USD 20/mes por asiento de desarrollador, incluye 1 TB de transferencia y 10 M de requests CDN. Hobby: 100 GB de transferencia y 1 M de requests, sin posibilidad de pagar excedentes.

### Dominio

| Opción                                  | Precio                                                             | Fuente                                                                                                                                                                                 |
| --------------------------------------- | ------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| .com en Cloudflare Registrar (al costo) | USD 10,46/año hoy; ~USD 11,17/año desde el 1-nov-2026 _(estimado)_ | <https://www.cloudflare.com/domains/>, tarifa de registro Verisign USD 10,97 desde 1-nov-2026: <https://itp.cdn.icann.org/en/files/registry-agreements/com/com-fees-01-11-2026-en.pdf> |
| .com en Namecheap                       | Renovación ~USD 18,48/año _(no verificado en la web de Namecheap)_ | Referencia de terceros                                                                                                                                                                 |
| .ec / .com.ec en NIC.ec                 | USD 40,25/año IVA incluido (USD 35 + IVA)                          | <https://www.nic.ec/tarifario.php>                                                                                                                                                     |

### Correo

| Servicio                                                                        | Precio                                                                                                                            |
| ------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| Cloudflare Email Routing                                                        | Gratis (solo reenvío, no buzón propio) _(no verificado en esta revisión)_                                                         |
| [Zoho Mail Free](https://www.zoho.com/mail/zohomail-pricing.html)               | Hasta 5 usuarios, 5 GB, un dominio, solo web/app (sin IMAP/POP). Precio de planes pagos no visible en la página _(no verificado)_ |
| [Google Workspace Business Starter](https://workspace.google.com/pricing)       | USD 7/usuario/mes (USD 5,60 con compromiso anual/promoción de 12 meses)                                                           |
| [Resend](https://resend.com/pricing) (transaccional)                            | Free: 3.000/mes, máx. 100/día. Pro: USD 20/mes, 50.000 correos                                                                    |
| [SMTP por defecto de Supabase](https://supabase.com/docs/guides/auth/auth-smtp) | Solo a miembros del equipo, 2 mensajes/hora; no apto para producción                                                              |

### Tiendas, push, monitoreo y CI

| Servicio                                                                                      | Precio                                                                              |
| --------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------- |
| [Google Play Console](https://support.google.com/googleplay/android-developer/answer/6112435) | USD 25 pago único                                                                   |
| [Apple Developer Program](https://developer.apple.com/support/enrollment/)                    | USD 99/año                                                                          |
| Push FCM / APNs                                                                               | Gratis _(no verificado en esta revisión; APNs va incluido en el programa de Apple)_ |
| [Sentry](https://sentry.io/pricing/)                                                          | Developer gratis (1 usuario, 50.000 errores). Team USD 26/mes                       |
| [GitHub Actions](https://docs.github.com/en/billing/concepts/product-billing/github-actions)  | Gratis en repos públicos. Privado: Free 2.000 min/mes, Pro y Team 3.000 min/mes     |
| [Tarifas por minuto](https://docs.github.com/en/billing/reference/actions-minute-multipliers) | Linux 2 núcleos USD 0,006/min; macOS USD 0,062/min (~10 veces Linux)                |
| [GitHub Team](https://github.com/pricing)                                                     | USD 4/usuario/mes                                                                   |

## Presupuesto 1: gratis (USD 0/mes)

| Pieza         | Servicio gratis                      | Límite que se rompe primero                                                                                                                                      |
| ------------- | ------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Backend       | Supabase Free                        | 5 GB de egress: con los supuestos anteriores alcanza para ~300 MAU. 500 MB de base: ~700 meses-usuario de carreras. Pausa tras 7 días sin actividad, sin backups |
| Web           | Vercel Hobby                         | **Prohibido para uso comercial**; válido solo mientras Gacelas sea un proyecto personal sin ingresos                                                             |
| Mapas         | MapLibre + OpenFreeMap               | Sin SLA; si el servicio cae o cambia condiciones, no hay soporte                                                                                                 |
| Dominio       | Subdominio `*.vercel.app`            | Sin marca propia; el enrolamiento de Apple como organización exige dominio y web propios                                                                         |
| Correo        | Cloudflare Email Routing o Zoho Free | Zoho Free sin IMAP; Email Routing solo reenvía                                                                                                                   |
| Transaccional | Resend Free                          | 100 correos/día                                                                                                                                                  |
| Errores       | Sentry Developer                     | 1 usuario, 50.000 errores/mes                                                                                                                                    |
| CI y releases | GitHub (repo público)                | Al pasar el repo a privado, 2.000 min/mes y macOS a USD 0,062/min consumen la cuota rápido                                                                       |
| Distribución  | APK por GitHub Releases + web        | Sin Play Store ni App Store; iOS no se puede distribuir a público sin Apple Developer Program                                                                    |

Conclusión: el modo gratis sirve para beta cerrada (decenas o pocos cientos de usuarios) y uso no comercial. No es legalmente válido para un producto comercial por Vercel Hobby, MapTiler/Stadia Free (si se usaran) y la falta de backups.

## Presupuesto 2: producción real

Stack recomendado: MapLibre + OpenFreeMap, Supabase Pro, Vercel Pro (1 asiento), dominio .com en Cloudflare, Google Workspace Starter (1 usuario) + Cloudflare Email Routing para alias, Resend, Sentry, GitHub Free con repo privado.

### Supabase por escenario _(estimado según supuestos)_

| Concepto            | ~1.000 MAU         | ~10.000 MAU                | ~50.000 MAU                  |
| ------------------- | ------------------ | -------------------------- | ---------------------------- |
| Plan Pro            | 25                 | 25                         | 25                           |
| Compute             | Micro (incluido) 0 | Small: 15 - 10 crédito = 5 | Medium: 60 - 10 crédito = 50 |
| Egress API          | 15 GB (incluido) 0 | 150 GB (incluido) 0        | 750 GB: 500 x 0,09 = 45      |
| Cached egress (APK) | 3 GB (incluido) 0  | 30 GB (incluido) 0         | 150 GB (incluido) 0          |
| Disco BD            | < 8 GB 0           | ~50 GB: 42 x 0,125 = 5,25  | ~200 GB: 192 x 0,125 = 24    |
| Realtime pico       | < 500 0            | < 500 0                    | ~1.000 conexiones: +10       |
| **Total Supabase**  | **25**             | **35,25**                  | **154**                      |

### Totales mensuales (USD)

| Concepto                                  | ~1.000 MAU | ~10.000 MAU | ~50.000 MAU |
| ----------------------------------------- | ---------- | ----------- | ----------- |
| Supabase                                  | 25,00      | 35,25       | 154,00      |
| Vercel Pro (1 asiento)                    | 20,00      | 20,00       | 20,00       |
| Mapas (OpenFreeMap)                       | 0,00       | 0,00        | 0,00        |
| Google Workspace Starter (1 usuario)      | 7,00       | 7,00        | 7,00        |
| Resend                                    | 0,00       | 20,00       | 20,00       |
| Sentry                                    | 0,00       | 26,00       | 26,00       |
| GitHub Actions macOS privado _(estimado)_ | 37,00      | 37,00       | 37,00       |
| Apple Developer (99/12)                   | 8,25       | 8,25        | 8,25        |
| Dominio .com (11,17/12)                   | 0,93       | 0,93        | 0,93        |
| Push FCM/APNs                             | 0,00       | 0,00        | 0,00        |
| **Total mensual**                         | **98,18**  | **154,43**  | **273,18**  |
| **Total anual (x 12)**                    | **1.178**  | **1.853**   | **3.278**   |

GitHub Actions: 600 min macOS x USD 0,062 = USD 37,20. La cuota de 2.000 minutos incluidos se agota con los jobs de Linux y macOS _(estimado; GitHub mide la cuota en minutos estándar)_. Mitigación: ejecutar el workflow iOS solo en `main` y en releases, o mantener el repo público; con eso este rubro tiende a USD 0.

### Costos únicos

| Concepto                                   | USD               |
| ------------------------------------------ | ----------------- |
| Google Play Console                        | 25                |
| Dominio .ec / .com.ec (opcional, 1.er año) | 40,25 (IVA incl.) |
| Total mínimo                               | 25                |

Apple Developer (USD 99) se paga cada año y ya está prorrateado en el total mensual.

### Opcionales

| Concepto                                               | USD/mes          |
| ------------------------------------------------------ | ---------------- |
| Supabase PITR (7 días)                                 | 100              |
| Respaldo `pg_dump` externo (p. ej. bucket S3/R2)       | < 1 _(estimado)_ |
| Mapas con SLA: MapTiler Flex / Stadia Starter          | 30 / 20          |
| Dominio personalizado de Supabase                      | 10               |
| GitHub Team (si hay más colaboradores en repo privado) | 4 por usuario    |

## Comparación: Google Maps en lugar de MapLibre

Supuesto: móvil con Maps SDK (gratis, ilimitado) y web con Dynamic Maps (20 % de MAU x 10 cargas).

| Escenario   | Cargas web/mes | Costo Dynamic Maps         |
| ----------- | -------------- | -------------------------- |
| ~1.000 MAU  | 2.000          | USD 0 (bajo 10.000 gratis) |
| ~10.000 MAU | 20.000         | 10.000 x 7/1.000 = USD 70  |
| ~50.000 MAU | 100.000        | 90.000 x 7/1.000 = USD 630 |

A eso se suman Geocoding, Places o Routes si se usan (USD 5 por 1.000 tras 10.000 gratis). Además habría que reescribir el mapa compartido KMP, perder la descarga offline de Loja basada en MapLibre y gestionar API keys restringidas. **Recomendación: mantener MapLibre + OpenFreeMap**; usar Google solo si se necesita un servicio puntual (por ejemplo, Places para buscar lugares de eventos) y con cuotas configuradas.

## Recomendaciones

1. Pasar a Supabase Pro antes de abrir al público: backups, sin pausa y 250 GB de egress.
2. Pasar a Vercel Pro en cuanto haya monetización o marca comercial.
3. Configurar SMTP propio (Resend) en Supabase Auth; el SMTP por defecto no sirve en producción.
4. Reducir el tamaño de las carreras (simplificar puntos, comprimir) para contener el disco de Supabase en 10.000+ MAU.
5. Antes de pasar el repo a privado, limitar el workflow iOS o presupuestar unos USD 37/mes de macOS.
6. Revisar estos precios cada 6 meses; los esquemas de Google Maps y GitHub cambian con frecuencia.
