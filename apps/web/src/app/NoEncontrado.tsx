import { Link } from 'react-router-dom'

export default function NoEncontrado() {
  return (
    <main className="min-h-dvh bg-bg-primary text-text-primary flex flex-col items-center justify-center gap-4 px-margin text-center">
      <span className="material-symbols-outlined text-[64px] text-c2-violeta" aria-hidden="true">
        explore_off
      </span>
      <h1 className="font-title-md text-title-md">Página no encontrada</h1>
      <p className="font-body-md text-body-md text-text-secondary max-w-sm">
        La dirección que buscas no existe o fue movida.
      </p>
      <Link
        to="/inicio"
        className="forma-boton-c2 bg-primary-600 text-on-primary px-6 h-12 inline-flex items-center font-label-lg"
      >
        Volver al inicio
      </Link>
    </main>
  )
}
