import { useEffect, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { LogoGacelas } from '../../shared/ui/LogoGacelas'
import { MAPAS_LANDING } from './mapasLanding'

/**
 * Landing publica en "/" (sin sesion). Presenta el producto con lo que de verdad hace hoy
 * (docs/DESIGN.md, SYSTEM.md, BUSINESS_RULES.md, RUTAS-LOJA.md, LEGAL.md) y lleva a las
 * pantallas de acceso. Es una pagina normal, sin modales.
 */

const TITULO = 'Runcity: corre con la manada de Loja'
const URL_ANDROID = 'https://github.com/jhonatanJJ2006/Gacelas/releases/latest'

const FOCO =
  'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-400 focus-visible:ring-offset-2 focus-visible:ring-offset-surface'
// Los botones con corte angular recortan su propio anillo: el foco se dibuja en un contenedor.
const FOCO_CONTENEDOR =
  'rounded-sm has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-primary-400 has-[:focus-visible]:ring-offset-2 has-[:focus-visible]:ring-offset-surface'
const CORTE_GRANDE = 'polygon(0 0, 100% 0, 100% calc(100% - 32px), calc(100% - 32px) 100%, 0 100%)'

// Datos de docs/RUTAS-LOJA.md (trazados de OpenStreetMap, ODbL).
const RUTAS = [
  { nombre: 'Cerro Villonaco', tipo: 'Montaña', km: '10.19', desnivel: '628 m', dificil: true },
  { nombre: 'Parque Lineal La Tebaida', tipo: 'Parque', km: '2.98', desnivel: '44 m' },
  { nombre: 'Orillas del río Zamora', tipo: 'Urbana', km: '3.88', desnivel: '20 m' },
  { nombre: 'Mirador Las Peñas', tipo: 'Urbana', km: '1.23', desnivel: '93 m' },
] as const

const PRIVACIDAD = [
  {
    valor: 'Apagado',
    texto: 'Compartir en vivo viene desactivado. Solo se enciende si tú lo activas.',
  },
  {
    valor: '300 m',
    texto:
      'Ocultamos el inicio y el final de tus recorridos en un radio de 300 m alrededor de tu casa. Puedes cambiar el radio.',
  },
  { valor: 'Sin anuncios', texto: 'No vendemos tus datos ni los usamos para publicidad.' },
  {
    valor: 'Borrable',
    texto:
      'Puedes descargar tus datos, revocar tus consentimientos y eliminar tu cuenta cuando quieras.',
  },
] as const

// Lo que la app hace hoy, en corto, para el hero.
const RESUMEN = [
  { icono: 'my_location', texto: 'GPS en cada carrera' },
  { icono: 'route', texto: 'Rutas reales de Loja' },
  { icono: 'offline_pin', texto: 'Mapa sin conexión (Android)' },
  { icono: 'groups', texto: 'Eventos y clubes' },
] as const

// Una de las 26 rutas oficiales (docs/RUTAS-LOJA.md), en la misma zona del mapa del hero.
const RUTA_DESTACADA = {
  nombre: 'Centro histórico de Loja',
  tipo: 'Urbana',
  km: '1.55',
  desnivel: '30 m',
  dificultad: 'Fácil',
} as const

// Cifras que salen de docs/RUTAS-LOJA.md, sin inventar usuarios ni opiniones.
const DATOS_REALES = [
  { valor: '26', texto: 'rutas oficiales' },
  { valor: '0.20–10.19', texto: 'km por ruta' },
  { valor: '628 m', texto: 'de desnivel en el Villonaco' },
] as const

const DIAS = [
  { letra: 'L', hecho: true },
  { letra: 'M', hecho: true },
  { letra: 'M', hecho: false },
  { letra: 'J', hecho: true },
  { letra: 'V', hecho: true },
  { letra: 'S', hecho: false },
  { letra: 'D', hecho: false },
] as const

function Panel({ className = '', children }: { className?: string; children: ReactNode }) {
  return (
    <article className={`tarjeta-c2 landing-panel p-6 sm:p-8 ${className}`}>{children}</article>
  )
}

type Punto = { readonly x: number; readonly y: number }
type DatosMapa = {
  readonly inicio: Punto
  readonly fin: Punto
  readonly manada: readonly Punto[]
  readonly vivo: Punto
}

const COLORES_MANADA = ['bg-primary-400', 'bg-[#F472B6]'] as const

/**
 * Capa de puntos sobre un mapa generado desde OpenStreetMap (scripts/generar-mapas-landing.py).
 * Las posiciones salen del mismo script, asi que quedan sobre la ruta real.
 */
function PuntosRuta({ datos }: { datos: DatosMapa }) {
  const en = (p: Punto) => ({ left: `${p.x}%`, top: `${p.y}%` })
  return (
    <>
      <span
        aria-hidden="true"
        className="absolute h-3.5 w-3.5 -translate-x-1/2 -translate-y-1/2 rounded-full border-[3px] border-[#F472B6] bg-surface"
        style={en(datos.inicio)}
      />
      <span
        aria-hidden="true"
        className="absolute h-3.5 w-3.5 -translate-x-1/2 -translate-y-1/2 rounded-full border-[3px] border-surface bg-[#F472B6]"
        style={en(datos.fin)}
      />
      {datos.manada.map((m, i) => (
        <span
          key={`${m.x}-${m.y}`}
          aria-hidden="true"
          className={`absolute h-3 w-3 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-surface ${COLORES_MANADA[i % COLORES_MANADA.length]}`}
          style={en(m)}
        />
      ))}
      <span
        aria-hidden="true"
        className="absolute -translate-x-1/2 -translate-y-1/2"
        style={en(datos.vivo)}
      >
        <span className="absolute inset-0 animate-ping rounded-full bg-secondary-500/60 motion-reduce:animate-none"></span>
        <span className="relative block h-4 w-4 rounded-full border-[3px] border-surface bg-secondary-500"></span>
      </span>
    </>
  )
}

/**
 * Mapa nocturno del centro de Loja (datos OSM) con una ruta, la manada y el corredor en vivo.
 * En movil va de fondo detras del titulo (como el login de la app); en escritorio, a la derecha.
 */
function EscenaRuta() {
  return (
    <figure className="pointer-events-none absolute inset-x-0 top-0 -z-0 mx-auto w-full max-w-[560px] lg:pointer-events-auto lg:relative lg:z-auto lg:order-2 lg:max-w-[460px]">
      <div
        role="img"
        aria-label="Mapa del centro de Loja con calles y edificios, una ruta de carrera desde la Puerta de la Ciudad, corredores de la manada y uno compartiendo en vivo"
        data-testid="landing-escena"
        className="landing-mapa relative w-full overflow-hidden bg-surface opacity-50 lg:border lg:border-c2-linea lg:opacity-100 lg:[clip-path:var(--corte)]"
        style={{ aspectRatio: '4 / 5', ['--corte' as string]: CORTE_GRANDE }}
      >
        <img
          src="/images/landing-mapa-hero.webp"
          alt=""
          width={960}
          height={1200}
          decoding="async"
          fetchPriority="high"
          className="absolute inset-0 h-full w-full object-cover"
        />
        <div
          aria-hidden="true"
          className="absolute inset-0 bg-gradient-to-b from-surface/60 via-surface/20 to-surface lg:bg-gradient-to-t lg:from-surface/70 lg:via-transparent lg:to-transparent"
        />
        <div className="hidden lg:contents">
          <PuntosRuta datos={MAPAS_LANDING.hero} />
        </div>
        <p className="absolute right-3 top-3 hidden items-center gap-2 rounded-full border border-c2-linea bg-surface/90 px-3 py-1 text-xs font-bold text-c2-menta backdrop-blur-sm lg:flex">
          <span className="h-2 w-2 rounded-full bg-secondary-500"></span>
          Así se ve compartir en vivo
        </p>
      </div>
      <figcaption className="mt-2 hidden text-right text-xs text-c2-muted lg:block">
        Mapa: © colaboradores de OpenStreetMap
      </figcaption>
    </figure>
  )
}

/** Tarjeta con los datos reales de una ruta oficial, estilo tarjeta C2 de la app. */
function TarjetaRuta() {
  const r = RUTA_DESTACADA
  return (
    <div className="tarjeta-c2 landing-aparecer landing-retraso-3 relative p-4 backdrop-blur-sm sm:p-5">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="etiqueta-c2 flex items-center gap-1.5 text-primary-400">
            <span aria-hidden="true" className="material-symbols-outlined text-[14px]">
              verified
            </span>
            Ruta oficial
          </p>
          <p className="mt-1 truncate font-semibold text-text-primary">{r.nombre}</p>
          <p className="text-sm text-c2-muted">
            {r.tipo}, {r.dificultad.toLowerCase()}
          </p>
        </div>
        <span aria-hidden="true" className="material-symbols-outlined text-[28px] text-[#F472B6]">
          route
        </span>
      </div>
      <dl className="mt-3 grid grid-cols-3 gap-2">
        <div className="metrica-c2">
          <dt className="etiqueta-c2">Distancia</dt>
          <dd className="valor-c2 text-text-primary">
            {r.km}{' '}
            <span className="font-sans text-xs font-semibold not-italic text-c2-muted">km</span>
          </dd>
        </div>
        <div className="metrica-c2">
          <dt className="etiqueta-c2">Desnivel</dt>
          <dd className="valor-c2 text-text-primary">+{r.desnivel}</dd>
        </div>
        <div className="metrica-c2">
          <dt className="etiqueta-c2">Nivel</dt>
          <dd className="valor-c2 text-c2-menta">{r.dificultad}</dd>
        </div>
      </dl>
    </div>
  )
}

function Cabecera() {
  return (
    <header className="relative z-10 mx-auto flex w-full max-w-6xl items-center justify-between gap-3 px-4 py-4 sm:px-6 lg:px-8">
      <Link
        to="/"
        aria-label="Runcity, inicio"
        className={`shrink-0 whitespace-nowrap rounded-lg ${FOCO}`}
      >
        <LogoGacelas tam={36} />
      </Link>
      <nav aria-label="Secciones" className="hidden items-center gap-7 md:flex">
        <a
          href="#funciones"
          className={`rounded py-2 text-sm font-semibold text-c2-muted hover:text-text-primary ${FOCO}`}
        >
          Funciones
        </a>
        <a
          href="#privacidad"
          className={`rounded py-2 text-sm font-semibold text-c2-muted hover:text-text-primary ${FOCO}`}
        >
          Privacidad
        </a>
      </nav>
      <div className="flex shrink-0 items-center gap-3 sm:gap-5">
        <Link
          to="/inicio-sesion"
          aria-label="Iniciar sesión"
          className={`rounded py-2 text-sm font-semibold text-text-primary hover:text-primary-400 ${FOCO}`}
        >
          <span className="sm:hidden">Entrar</span>
          <span className="hidden sm:inline">Iniciar sesión</span>
        </Link>
        <div className={`hidden sm:block ${FOCO_CONTENEDOR}`}>
          <Link
            to="/registro"
            className="boton-c2-chico whitespace-nowrap px-4 focus-visible:outline-none sm:px-5"
          >
            Crear cuenta
          </Link>
        </div>
      </div>
    </header>
  )
}

function Funciones() {
  return (
    <section
      id="funciones"
      aria-labelledby="funciones-titulo"
      className="mx-auto w-full max-w-6xl scroll-mt-4 px-4 py-16 sm:px-6 lg:px-8 lg:py-20"
    >
      <h2 id="funciones-titulo" className="text-balance font-display text-4xl sm:text-5xl">
        Hecho para correr en Loja
      </h2>
      <p className="mt-4 max-w-[34rem] text-lg leading-relaxed text-c2-muted">
        Tus carreras, las rutas de la ciudad y la gente con la que sales, en un solo lugar.
      </p>

      <div className="mt-10 grid gap-4 lg:grid-cols-12">
        <Panel className="lg:col-span-7">
          <h3 className="font-display text-2xl">Rutas de Loja listas para salir</h3>
          <p className="mt-3 max-w-[34rem] leading-relaxed text-c2-muted">
            Runcity trae 26 rutas oficiales de Loja y sus alrededores, con distancia, desnivel y
            dificultad. También puedes grabar tus carreras, dibujar rutas a mano y compartirlas. En
            la app Android puedes descargar el mapa para usarlo sin conexión.
          </p>
          <ul className="mt-5">
            {RUTAS.map((r) => (
              <li
                key={r.nombre}
                className="grid grid-cols-[1fr_auto] items-center gap-x-4 border-t border-c2-linea py-3"
              >
                <div className="min-w-0">
                  <p className="font-semibold text-text-primary">{r.nombre}</p>
                  <p className="text-sm text-c2-muted">
                    {r.tipo}, desnivel {r.desnivel}
                  </p>
                </div>
                <div className="text-right">
                  <p className="valor-c2 text-[26px] text-text-primary">
                    {r.km}{' '}
                    <span className="font-sans text-sm font-semibold not-italic text-c2-muted">
                      km
                    </span>
                  </p>
                  <p
                    className={`mt-0.5 text-xs font-bold ${'dificil' in r ? 'text-[#F472B6]' : 'text-c2-menta'}`}
                  >
                    {'dificil' in r ? 'Difícil' : 'Fácil'}
                  </p>
                </div>
              </li>
            ))}
          </ul>
          <p className="mt-3 text-xs text-c2-muted">Trazados: © colaboradores de OpenStreetMap.</p>
        </Panel>

        <Panel className="flex flex-col lg:col-span-5">
          <h3 className="font-display text-2xl">Que te sigan, si tú quieres</h3>
          <p className="mt-3 leading-relaxed text-c2-muted">
            Activa compartir en vivo y tus seguidores, tu club o un evento verán tu posición en el
            mapa mientras corres. Se detiene solo cuando terminas.
          </p>
          <div
            role="img"
            aria-label="Mapa junto al río Zamora con la ruta de un grupo, sus corredores y uno compartiendo su posición en vivo"
            data-testid="landing-mapa-vivo"
            className="relative mt-6 w-full overflow-hidden border border-c2-linea bg-surface"
            style={{ aspectRatio: '2 / 1' }}
          >
            <img
              src="/images/landing-mapa-vivo.webp"
              alt=""
              width={1280}
              height={640}
              loading="lazy"
              decoding="async"
              className="absolute inset-0 h-full w-full object-cover"
            />
            <PuntosRuta datos={MAPAS_LANDING.vivo} />
          </div>
          <div
            aria-hidden="true"
            className="mt-4 flex items-center justify-between gap-4 bg-c2-s2 px-4 py-3"
          >
            <div>
              <p className="text-sm font-semibold text-text-primary">Compartir en vivo</p>
              <p className="text-xs text-c2-muted">Lo activaste tú. Se apaga al terminar.</p>
            </div>
            <span className="relative h-7 w-12 shrink-0 rounded-full bg-secondary-500/80">
              <span className="absolute right-1 top-1 h-5 w-5 rounded-full bg-white"></span>
            </span>
          </div>
          <p className="mt-2 text-xs text-c2-muted">Mapa: © colaboradores de OpenStreetMap</p>
        </Panel>

        <Panel className="lg:col-span-7">
          <h3 className="font-display text-2xl">Mira cómo está la zona antes de salir</h3>
          <p className="mt-3 max-w-[34rem] leading-relaxed text-c2-muted">
            Mostramos el nivel de seguridad de cada zona con datos abiertos del SIS ECU 911 y
            reportes verificados de otros corredores. Cada día te sugerimos una ruta con un nivel de
            seguridad aceptable que no hayas corrido en los últimos días.
          </p>
          <p className="mt-3 max-w-[34rem] leading-relaxed text-c2-muted">
            Si reportas un incidente, los demás lo ven sin tu nombre y con la ubicación aproximada.
          </p>
          <p className="mt-3 text-xs text-c2-muted">
            Datos agregados del SIS ECU 911, licencia CC-BY.
          </p>
        </Panel>

        <Panel className="lg:col-span-5">
          <h3 className="font-display text-2xl">Corre en grupo</h3>
          <ul className="mt-4 space-y-3 leading-relaxed text-c2-muted">
            <li>Clubes y grupos con punto de encuentro, días, hora, nivel y ritmo.</li>
            <li>Eventos con cupo y lista de espera.</li>
            <li>Un feed para ver las carreras de los corredores que sigues.</li>
          </ul>
        </Panel>

        <Panel className="lg:col-span-12">
          <div className="flex flex-col gap-6 md:flex-row md:items-center md:justify-between">
            <div className="max-w-[34rem]">
              <h3 className="font-display text-2xl">Mantén el ritmo</h3>
              <p className="mt-3 leading-relaxed text-c2-muted">
                Rachas diarias y semanales, logros y una meta semanal. Te avisamos cuando tu racha
                está en riesgo.
              </p>
            </div>
            <div aria-hidden="true" className="flex shrink-0 gap-2">
              {DIAS.map((d, i) => (
                <div key={i} className="flex flex-col items-center gap-1.5">
                  <span
                    className={`block h-11 w-9 -skew-x-12 ${d.hecho ? 'bg-primary-600' : 'bg-c2-s2'}`}
                  ></span>
                  <span className="text-xs font-semibold text-c2-muted">{d.letra}</span>
                </div>
              ))}
            </div>
          </div>
        </Panel>
      </div>
    </section>
  )
}

function Privacidad() {
  return (
    <section
      id="privacidad"
      aria-labelledby="privacidad-titulo"
      className="scroll-mt-4 border-y border-c2-linea bg-c2-card/60"
    >
      <div className="mx-auto grid w-full max-w-6xl gap-10 px-4 py-16 sm:px-6 lg:grid-cols-[minmax(0,0.8fr)_minmax(0,1.2fr)] lg:gap-16 lg:px-8 lg:py-20">
        <div>
          <h2 id="privacidad-titulo" className="text-balance font-display text-4xl sm:text-5xl">
            Tu ubicación es tuya
          </h2>
          <p className="mt-4 max-w-[30rem] text-lg leading-relaxed text-c2-muted">
            Runcity empieza mostrando lo mínimo y tú decides qué compartir y con quién.
          </p>
          <Link
            to="/legal/privacidad"
            className={`mt-6 inline-block rounded py-2 font-semibold text-primary-400 underline underline-offset-4 hover:text-primary-200 ${FOCO}`}
          >
            Leer la política de privacidad
          </Link>
        </div>
        <ul className="divide-y divide-c2-linea border-y border-c2-linea">
          {PRIVACIDAD.map((p) => (
            <li
              key={p.valor}
              className="grid grid-cols-[7.5rem_minmax(0,1fr)] items-baseline gap-4 py-5 sm:grid-cols-[11rem_minmax(0,1fr)] sm:gap-6"
            >
              <span className="valor-c2 whitespace-normal text-[28px] text-primary-400 sm:text-[36px]">
                {p.valor}
              </span>
              <p className="max-w-[34rem] leading-relaxed text-on-surface">{p.texto}</p>
            </li>
          ))}
        </ul>
      </div>
    </section>
  )
}

function LlamadaFinal() {
  return (
    <section
      aria-labelledby="cta-titulo"
      className="mx-auto w-full max-w-6xl px-4 py-16 sm:px-6 lg:px-8 lg:py-24"
    >
      <div
        className="escenario-c2 relative overflow-hidden border border-c2-linea px-6 py-12 sm:px-12 sm:py-16"
        style={{ clipPath: CORTE_GRANDE }}
      >
        <div className="relative z-10 max-w-xl">
          <h2 id="cta-titulo" className="font-display text-4xl sm:text-6xl">
            Sal a correr hoy
          </h2>
          <p className="mt-4 text-lg leading-relaxed text-on-surface">
            Crea tu cuenta, activa el GPS cuando quieras grabar y súmate a las rutas de Loja.
          </p>
          <div className="mt-8 flex flex-col gap-4 sm:flex-row sm:items-center">
            <div className={`${FOCO_CONTENEDOR} w-full sm:w-auto`}>
              <Link to="/registro" className="boton-c2 focus-visible:outline-none sm:px-10">
                Crear cuenta
              </Link>
            </div>
            <a
              href={URL_ANDROID}
              target="_blank"
              rel="noopener noreferrer"
              className={`rounded py-2 font-semibold text-text-primary underline underline-offset-4 hover:text-primary-200 ${FOCO}`}
            >
              Descargar para Android
              <span className="sr-only"> (se abre en una pestaña nueva)</span>
            </a>
          </div>
          <p className="mt-5 text-sm text-on-surface">
            ¿Ya tienes cuenta?{' '}
            <Link
              to="/inicio-sesion"
              className={`rounded font-semibold underline underline-offset-4 hover:text-primary-200 ${FOCO}`}
            >
              Iniciar sesión
            </Link>
          </p>
        </div>
      </div>
    </section>
  )
}

function Pie() {
  return (
    <footer className="border-t border-c2-linea">
      <div className="mx-auto flex w-full max-w-6xl flex-col gap-6 px-4 py-10 sm:px-6 md:flex-row md:items-start md:justify-between lg:px-8">
        <div className="max-w-sm">
          <LogoGacelas tam={32} subtitulo={null} />
          <p className="mt-3 text-sm leading-relaxed text-c2-muted">
            Hecho en Loja, Ecuador. Proyecto de Jhonatan Jara, estudiante de Computación en la UTPL.
          </p>
        </div>
        <nav aria-label="Documentos legales" className="flex gap-6">
          <Link
            to="/legal/terminos"
            className={`rounded py-2 text-sm font-semibold text-text-primary hover:text-primary-400 ${FOCO}`}
          >
            Términos y condiciones
          </Link>
          <Link
            to="/legal/privacidad"
            className={`rounded py-2 text-sm font-semibold text-text-primary hover:text-primary-400 ${FOCO}`}
          >
            Política de privacidad
          </Link>
        </nav>
      </div>
    </footer>
  )
}

export function Landing() {
  useEffect(() => {
    const anterior = document.title
    document.title = TITULO
    return () => {
      document.title = anterior
    }
  }, [])

  return (
    <div className="min-h-screen bg-surface text-text-primary" data-testid="landing">
      <a
        href="#contenido"
        className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-20 focus:bg-c2-card focus:px-4 focus:py-2 focus:font-semibold"
      >
        Saltar al contenido
      </a>
      <Cabecera />
      <main id="contenido">
        <section aria-labelledby="hero-titulo" className="relative isolate overflow-hidden">
          <div aria-hidden="true" className="escenario-c2 absolute inset-0 -z-10 hidden lg:block" />
          <div className="relative mx-auto grid w-full max-w-6xl gap-10 px-4 pb-14 pt-4 sm:px-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,0.85fr)] lg:items-center lg:gap-14 lg:px-8 lg:pb-20 lg:pt-10">
            <EscenaRuta />
            <div className="relative">
              <p className="landing-aparecer mb-3 inline-flex items-center gap-2 rounded-full border border-primary-400/30 bg-surface/70 px-3 py-1 text-[11px] font-bold uppercase tracking-[0.16em] text-primary-200 backdrop-blur-sm sm:text-xs">
                <span aria-hidden="true" className="material-symbols-outlined text-[14px]">
                  location_on
                </span>
                App de running en Loja, Ecuador
              </p>
              <h1
                id="hero-titulo"
                className="landing-aparecer text-balance font-display text-[52px] leading-[0.95] drop-shadow-[0_2px_12px_rgba(12,19,34,0.8)] sm:text-6xl lg:text-[68px]"
              >
                Corre con la <span className="text-primary-400">manada</span> de Loja
              </h1>
              <p className="landing-aparecer landing-retraso-1 mt-4 max-w-[32rem] text-base leading-relaxed text-on-surface sm:text-lg">
                Graba tus carreras con GPS, sal por rutas reales de la ciudad y deja que tu grupo te
                siga en vivo, solo cuando tú lo decidas.
              </p>
              <ul
                aria-label="Qué hace Runcity"
                className="landing-aparecer landing-retraso-1 mt-5 grid grid-cols-2 gap-x-4 gap-y-2.5 sm:max-w-[30rem]"
              >
                {RESUMEN.map((b) => (
                  <li key={b.texto} className="flex items-center gap-2 text-sm text-on-surface">
                    <span
                      aria-hidden="true"
                      className="material-symbols-outlined shrink-0 text-[20px] text-primary-400"
                    >
                      {b.icono}
                    </span>
                    {b.texto}
                  </li>
                ))}
              </ul>
              <div className="landing-aparecer landing-retraso-2 mt-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:gap-6">
                <div className={`${FOCO_CONTENEDOR} w-full sm:w-auto`}>
                  <Link
                    to="/registro"
                    className="boton-c2 whitespace-nowrap focus-visible:outline-none sm:px-10"
                  >
                    Crear cuenta
                    <span aria-hidden="true" className="material-symbols-outlined text-[20px]">
                      arrow_forward
                    </span>
                  </Link>
                </div>
                <p className="text-center text-sm text-c2-muted sm:text-left">
                  ¿Ya tienes cuenta?{' '}
                  <Link
                    to="/inicio-sesion"
                    className={`rounded py-2 font-semibold text-text-primary underline underline-offset-4 hover:text-primary-200 ${FOCO}`}
                  >
                    Iniciar sesión
                  </Link>
                </p>
              </div>
              <div className="mt-6 lg:max-w-[30rem]">
                <TarjetaRuta />
              </div>
              <ul
                aria-label="Runcity en cifras"
                className="landing-aparecer landing-retraso-3 mt-5 grid grid-cols-3 gap-3 border-t border-c2-linea pt-4"
              >
                {DATOS_REALES.map((d) => (
                  <li key={d.texto}>
                    <p className="valor-c2 text-[20px] text-text-primary sm:text-[24px]">
                      {d.valor}
                    </p>
                    <p className="mt-1 text-xs leading-snug text-c2-muted">{d.texto}</p>
                  </li>
                ))}
              </ul>
              <p className="mt-4 text-xs text-c2-muted">
                Funciona en el navegador y tiene{' '}
                <a
                  href={URL_ANDROID}
                  target="_blank"
                  rel="noopener noreferrer"
                  className={`rounded font-semibold text-text-primary underline underline-offset-4 hover:text-primary-200 ${FOCO}`}
                >
                  app para Android
                  <span className="sr-only"> (se abre en una pestaña nueva)</span>
                </a>
                . Rutas y mapa: © colaboradores de OpenStreetMap.
              </p>
            </div>
          </div>
        </section>
        <Funciones />
        <Privacidad />
        <LlamadaFinal />
      </main>
      <Pie />
    </div>
  )
}
