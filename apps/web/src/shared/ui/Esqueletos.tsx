/*
 * Esqueletos de carga C2: bloques con brillo suave (solo opacidad, en GPU) que imitan la forma del
 * contenido que viene. Todos llevan aria-busy y un texto para lectores de pantalla.
 */

export function Esqueleto({ className = '' }: { className?: string }) {
  return <div aria-hidden="true" className={`esqueleto-c2 bg-white/[0.07] ${className}`} />
}

/** Lista de tarjetas (rutas, clubes, eventos, corredores). */
export function EsqueletoLista({
  filas = 3,
  conImagen = false,
  testId = 'esqueleto-lista',
}: {
  filas?: number
  conImagen?: boolean
  testId?: string
}) {
  return (
    <div aria-busy="true" role="status" data-testid={testId} className="space-y-3">
      <span className="sr-only">Cargando</span>
      {Array.from({ length: filas }, (_, i) => (
        <div key={i} className="bg-c2-card border border-c2-linea rounded-[14px] p-4 space-y-3">
          {conImagen && <Esqueleto className="h-32 w-full rounded-[10px]" />}
          <div className="flex items-center gap-3">
            <Esqueleto className="w-12 h-12 rounded-xl shrink-0" />
            <div className="flex-1 space-y-2">
              <Esqueleto className="h-4 w-2/3 rounded" />
              <Esqueleto className="h-3 w-1/2 rounded" />
            </div>
          </div>
        </div>
      ))}
    </div>
  )
}

/** Pantalla completa: cabecera, bloque destacado y tarjetas (carga de la app y de pantallas). */
export function EsqueletoPantalla({ testId = 'esqueleto-pantalla' }: { testId?: string }) {
  return (
    <main
      aria-busy="true"
      role="status"
      data-testid={testId}
      className="min-h-[100dvh] bg-gradient-to-b from-[#2A0A3D] via-[#1A0B2E] to-[#120821] px-5 pt-[max(env(safe-area-inset-top),20px)] pb-24"
    >
      <span className="sr-only">Cargando</span>
      <div className="max-w-md mx-auto space-y-4">
        <div className="flex items-center gap-3">
          <Esqueleto className="w-10 h-10 rounded-full" />
          <Esqueleto className="h-6 w-40 rounded" />
        </div>
        <Esqueleto className="h-44 w-full rounded-[14px]" />
        <div className="grid grid-cols-3 gap-3">
          <Esqueleto className="h-16 rounded-[10px]" />
          <Esqueleto className="h-16 rounded-[10px]" />
          <Esqueleto className="h-16 rounded-[10px]" />
        </div>
        <EsqueletoLista filas={2} testId="esqueleto-pantalla-lista" />
      </div>
    </main>
  )
}

/** Silueta del corredor mientras carga la escena 3D. */
export function EsqueletoEscena3D({ texto = 'Cargando escena 3D' }: { texto?: string }) {
  return (
    <div
      role="status"
      aria-busy="true"
      data-testid="esqueleto-3d"
      className="w-full h-full flex items-end justify-center pb-6"
    >
      <span className="sr-only">{texto}</span>
      <div className="flex flex-col items-center gap-2" aria-hidden="true">
        <Esqueleto className="w-14 h-14 rounded-full" />
        <Esqueleto className="w-20 h-32 rounded-[40%]" />
        <Esqueleto className="w-28 h-4 rounded-[50%]" />
      </div>
    </div>
  )
}
