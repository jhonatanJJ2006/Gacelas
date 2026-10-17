# ADR 002: Capacidad objetivo y perfil free

**Fecha**: 2026-10-14  
**Estado**: Aceptado  
**Decisores**: Jhonatan Jara, Equipo Gacelas  
**Relacionado**: [ADR 001](001-backend-selection.md)

## Contexto

El lanzamiento previsto supera las 300 personas usando la app al mismo tiempo, y la arquitectura debe poder crecer bastante mas que eso. Hoy el proyecto sigue en el plan Free de Supabase. Esos dos hechos no se pueden cumplir a la vez en el runtime: el codigo puede estar preparado para la carga grande, pero el perfil que corre ahora tiene que quedarse dentro de los topes de la plataforma.

Limites publicados del plan Free que importan aqui:

- Realtime: 200 conexiones concurrentes, sin excedente. Al pasar de 200 la conexion se rechaza.
- Realtime: 100 mensajes por segundo y 2 millones de mensajes al mes.
- Postgres Changes cuenta un mensaje por cliente que escucha el cambio.
- Broadcast cuenta 1 mensaje enviado mas 1 por cada cliente que lo recibe.
- Base de datos: 500 MB. Egress: 5 GB al mes.
- Edge Functions: 500.000 invocaciones al mes.
- Las peticiones HTTP a PostgREST no tienen cupo de lecturas/escrituras. El costo esta en CPU, disco y egress.
- Un proyecto free se pausa tras una semana sin uso. No se agrega un cron artificial para evitarlo.

Limites del plan Pro, solo como puerta de salida, no como runtime actual:

- 500 conexiones Realtime incluidas. Sin tope de gasto, el techo configurable llega a 10.000.
- 500 mensajes por segundo incluidos. Sin tope de gasto, 2.500.
- Computo Small: 400 conexiones del pooler. Medium: 600.

## Decision

Se disenan dos cosas distintas.

1. **Capacidad objetivo del codigo**, para cuando se salga de free:
   - 3.000 sesiones concurrentes.
   - 500 corredores emitiendo posicion.
   - Un evento con hasta 1.000 espectadores.
2. **Perfil free**, que es el unico runtime permitido hasta un ADR de salida:
   - Presupuesto de aplicacion: 150 de las 200 conexiones Realtime.
   - Como maximo 1 canal vivo por cliente.
   - Posicion como maximo cada 5 segundos.
   - Lotes de 1 a 50 puntos, y nunca mas de 1 punto por segundo.
   - Si el cupo local esta lleno, no se abre otro canal y no se hace polling de posiciones.
   - El feed lee resumenes, no `track_points`.
   - El cliente no inserta `track_points`. Lo hace el RPC `append_track_batch`.

Supabase sigue siendo la base de datos, la autenticacion y PostGIS. No se migra a otro backend. Un BFF en Hono puede existir en el repositorio, pero queda apagado. En free el camino por defecto es PostgREST, porque no gasta el cupo de Edge Functions ni el de Cloudflare Workers.

## Reglas de Realtime

- El vivo usa Broadcast, un canal por actividad.
- No hay canal global de posiciones.
- El espectador se suscribe solo a corredores visibles en su mapa.
- Postgres Changes no escucha `track_points`.
- Postgres Changes, cuando se use, queda limitado a inicio o fin de actividad y a notificaciones.
- Esta prohibido correr pruebas de carga o e2e contra el proyecto free alojado.

## Por que no 300 sockets en free

300 personas en vivo a la vez ya superan las 200 conexiones del plan Free. El codigo no puede saltarse ese tope. Lo que si cabe en free es mucha gente consultando feed y rutas por HTTP, mas un cupo chico de seguimiento en vivo. Servir 300 sockets simultaneos exige salir de free. Ese paso esta descrito en el ADR 003, cuando exista.

## Consecuencias

### Positivas

- El mismo contrato de lotes, canales y resumenes sirve en free y en pro.
- Un broadcast mal disenado no puede multiplicar mensajes contra todo el mundo.
- El disco de 500 MB no se llena con selects de rutas crudas en el feed.
- Cambiar de perfil es subir numeros de configuracion, no reescribir clientes.

### Negativas

- En free el vivo se niega al llegar al cupo. El usuario ve que esta completo.
- El cupo de 150 conexiones del proyecto no lo puede imponer solo el navegador. El cliente impone 1 canal por sesion. El tope de proyecto se respeta no abriendo sockets de mas y no haciendo fan-out global.
- Sin plan Pro el proyecto se puede pausar a los 7 dias.

## Referencias

- [Limites de Realtime](https://supabase.com/docs/guides/realtime/limits)
- [Precio de Realtime](https://supabase.com/docs/guides/realtime/pricing)
- [ADR 001](001-backend-selection.md)
