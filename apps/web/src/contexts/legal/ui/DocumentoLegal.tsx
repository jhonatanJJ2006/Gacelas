import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useEsEscritorio } from '../../../shared/lib/useEsEscritorio'
import { LogoGacelas } from '../../../shared/ui/LogoGacelas'
import { useIdentidad } from '../../identidad/api'
import { PRIVACIDAD, TERMINOS, VERSIONES_LEGALES, type SeccionLegal } from '../domain/textos'

/**
 * Pagina de un documento legal (Terminos o Politica de privacidad). Cada uno tiene su propia URL
 * (/legal/terminos y /legal/privacidad), se lee sin sesion y no usa modales. Solo cambia la
 * presentacion: los textos viven en domain/textos.ts y deben seguir iguales a TextosLegales.kt.
 */

type Tipo = 'terminos' | 'privacidad'

interface Documento {
  titulo: string
  etiqueta: string
  ruta: string
  resumen: string
  version: string
  secciones: SeccionLegal[]
  testid: string
}

const DOCUMENTOS: Record<Tipo, Documento> = {
  terminos: {
    titulo: 'Términos y condiciones',
    etiqueta: 'Términos',
    ruta: '/legal/terminos',
    resumen:
      'Las reglas para usar Runcity: tu cuenta, el uso aceptable, tu contenido y la ley que se aplica.',
    version: VERSIONES_LEGALES.terminos,
    secciones: TERMINOS,
    testid: 'doc-terminos',
  },
  privacidad: {
    titulo: 'Política de privacidad',
    etiqueta: 'Privacidad',
    ruta: '/legal/privacidad',
    resumen:
      'Qué datos tratamos, para qué los usamos, con quién los compartimos y cómo ejercer tus derechos.',
    version: VERSIONES_LEGALES.privacidad,
    secciones: PRIVACIDAD,
    testid: 'doc-privacidad',
  },
}

const OTRO: Record<Tipo, Tipo> = { terminos: 'privacidad', privacidad: 'terminos' }

const FOCO =
  'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary-400 focus-visible:ring-offset-2 focus-visible:ring-offset-surface'

/** "2026-10-25" a "25 de octubre de 2026", sin pasar por UTC para no correr el dia. */
function fechaLarga(version: string): string {
  const [a, m, d] = version.split('-').map(Number)
  if (!a || !m || !d) return version
  return new Intl.DateTimeFormat('es-EC', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  }).format(new Date(a, m - 1, d))
}

/** "3. Tu cuenta" a { numero: "3", nombre: "Tu cuenta" }. */
function partirTitulo(titulo: string) {
  const m = /^(\d+)\.\s*(.*)$/.exec(titulo)
  return m ? { numero: m[1], nombre: m[2] } : { numero: '', nombre: titulo }
}

function Indice({ secciones }: { secciones: SeccionLegal[] }) {
  return (
    <ol className="space-y-0.5">
      {secciones.map((s, i) => {
        const { numero, nombre } = partirTitulo(s.titulo)
        return (
          <li key={s.titulo}>
            <a
              href={`#seccion-${i + 1}`}
              className={`flex min-h-[44px] items-baseline gap-3 rounded px-2 py-2 text-sm text-c2-muted hover:bg-c2-card hover:text-text-primary lg:min-h-0 lg:py-1.5 ${FOCO}`}
            >
              <span className="valor-c2 w-5 shrink-0 text-right text-[14px] text-primary-400">
                {numero}
              </span>
              <span>{nombre}</span>
            </a>
          </li>
        )
      })}
    </ol>
  )
}

export function DocumentoLegal({ tipo }: { tipo: Tipo }) {
  const doc = DOCUMENTOS[tipo]
  const otro = DOCUMENTOS[OTRO[tipo]]
  const location = useLocation()
  const navigate = useNavigate()
  const escritorio = useEsEscritorio()
  const hayDestinoAtras = location.key !== 'default'
  const conSesion = useIdentidad((s) => s.sesion != null)

  // Quien llega por un enlace directo no tiene historial: se le lleva a la entrada de la app.
  const volver = () => (hayDestinoAtras ? navigate(-1) : navigate('/'))

  return (
    <div className="min-h-screen bg-surface text-text-primary print:bg-white print:text-black">
      <header className="border-b border-c2-linea print:hidden">
        <div className="mx-auto flex w-full max-w-6xl items-center justify-between gap-4 px-4 py-3 sm:px-6 lg:px-8">
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={volver}
              aria-label="Volver"
              className={`flex h-12 w-12 -ml-3 shrink-0 items-center justify-center text-text-primary ${FOCO}`}
            >
              <span className="material-symbols-outlined" aria-hidden="true">
                arrow_back
              </span>
            </button>
            <Link
              to="/"
              aria-label="Runcity, inicio"
              className={`hidden rounded-lg sm:block ${FOCO}`}
            >
              <LogoGacelas tam={32} subtitulo={null} />
            </Link>
          </div>
          <nav aria-label="Documentos legales" className="flex gap-1 bg-c2-card p-1">
            {(Object.keys(DOCUMENTOS) as Tipo[]).map((t) => {
              const activo = t === tipo
              return (
                <Link
                  key={t}
                  to={DOCUMENTOS[t].ruta}
                  replace
                  aria-current={activo ? 'page' : undefined}
                  className={`flex min-h-[44px] items-center px-3 text-sm font-semibold transition-colors sm:px-4 ${FOCO} ${
                    activo
                      ? 'bg-primary-600 text-white'
                      : 'text-c2-muted hover:bg-c2-s2 hover:text-text-primary'
                  }`}
                >
                  {DOCUMENTOS[t].etiqueta}
                </Link>
              )
            })}
          </nav>
        </div>
      </header>

      <main>
        <div className="mx-auto w-full max-w-6xl px-4 pb-8 pt-10 sm:px-6 lg:px-8 lg:pt-14">
          <h1 className="font-display text-4xl print:text-black sm:text-6xl">{doc.titulo}</h1>
          <p className="mt-4 max-w-[40rem] text-lg leading-relaxed text-c2-muted print:text-black">
            {doc.resumen}
          </p>
          <div className="mt-6 flex max-w-[40rem] flex-col gap-1 border-l-4 border-primary-600 bg-c2-card px-4 py-3 print:bg-white">
            <p className="text-sm text-c2-muted print:text-black">
              Versión del{' '}
              <time dateTime={doc.version} className="font-bold text-primary-400 print:text-black">
                {fechaLarga(doc.version)}
              </time>
            </p>
            <p className="text-sm text-c2-muted print:text-black">
              Adaptado a la Ley Orgánica de Protección de Datos Personales del Ecuador.
            </p>
          </div>
        </div>

        <div className="mx-auto grid w-full max-w-6xl gap-8 px-4 pb-20 sm:px-6 lg:grid-cols-[16rem_minmax(0,1fr)] lg:gap-14 lg:px-8">
          {escritorio ? (
            <nav
              aria-label="Contenido del documento"
              className="sticky top-6 self-start print:hidden"
            >
              <p className="mb-3 px-2 text-sm font-bold text-text-primary">En este documento</p>
              <Indice secciones={doc.secciones} />
            </nav>
          ) : (
            <details className="border border-c2-linea bg-c2-card print:hidden">
              <summary
                className={`flex min-h-[44px] cursor-pointer items-center px-4 font-semibold ${FOCO}`}
              >
                En este documento
              </summary>
              <nav aria-label="Contenido del documento" className="px-2 pb-3">
                <Indice secciones={doc.secciones} />
              </nav>
            </details>
          )}

          <article data-testid={doc.testid} className="min-w-0">
            {doc.secciones.map((s, i) => (
              <section
                key={s.titulo}
                id={`seccion-${i + 1}`}
                aria-labelledby={`titulo-seccion-${i + 1}`}
                className="scroll-mt-6 border-t border-c2-linea py-8 first:border-t-0 first:pt-0 print:break-inside-avoid"
              >
                <h2
                  id={`titulo-seccion-${i + 1}`}
                  className="font-title-md text-title-md text-text-primary print:text-black sm:text-2xl"
                >
                  {s.titulo}
                </h2>
                <p className="mt-3 max-w-[68ch] text-[17px] leading-8 text-on-surface print:text-black">
                  {s.texto}
                </p>
              </section>
            ))}

            <div className="mt-4 grid gap-4 border-t border-c2-linea pt-8 print:hidden sm:grid-cols-2">
              {tipo === 'privacidad' && conSesion && (
                <Link
                  to="/ajustes/privacidad"
                  className={`tarjeta-c2 block p-5 transition-colors hover:bg-c2-s2 ${FOCO}`}
                >
                  <span className="block font-bold text-text-primary">Tus consentimientos</span>
                  <span className="mt-1 block text-sm text-c2-muted">
                    Revócalos o elimina tu cuenta desde Ajustes, Privacidad.
                  </span>
                </Link>
              )}
              <Link
                to={otro.ruta}
                className={`tarjeta-c2 block p-5 transition-colors hover:bg-c2-s2 ${FOCO}`}
              >
                <span className="block text-sm text-c2-muted">Lee también</span>
                <span className="mt-1 block font-bold text-text-primary">{otro.titulo}</span>
              </Link>
            </div>
          </article>
        </div>
      </main>

      <footer className="border-t border-c2-linea print:hidden">
        <p className="mx-auto w-full max-w-6xl px-4 py-6 text-xs text-c2-muted sm:px-6 lg:px-8">
          Runcity, Loja, Ecuador.
        </p>
      </footer>
    </div>
  )
}
