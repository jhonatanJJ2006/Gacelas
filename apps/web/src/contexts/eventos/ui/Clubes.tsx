import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { BotonTexto, Dialogo, Interruptor, MensajeError } from '../../../shared/ui/ui'
import { useApp } from '../../../store/app'
import { clubes, validarClub, type Club } from '../api'

const campo =
  'w-full h-11 px-3 bg-surface-container border border-border-subtle rounded-lg text-text-primary placeholder-text-secondary'

export function Clubes() {
  const navigate = useNavigate()
  const sesion = useApp((s) => s.sesion)
  const [lista, setLista] = useState<Club[] | null>(null)
  const [mios, setMios] = useState<Set<string>>(new Set())
  const [consulta, setConsulta] = useState('')
  const [creando, setCreando] = useState(false)
  const [form, setForm] = useState({
    nombre: '',
    descripcion: '',
    ubicacion: 'Loja',
    conAprobacion: false,
  })
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!clubes.disponible()) return setLista([])
    void clubes
      .listar()
      .then((r) => {
        setLista(r.todos)
        setMios(r.mios)
      })
      .catch(() => setLista([]))
  }, [sesion])

  const crear = async () => {
    const e = validarClub(form)
    if (e) return setError(e)
    try {
      const c = await clubes.crear(form)
      navigate(`/comunidad/clubes/${c.id}`)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo crear el club')
    }
  }
  const q = consulta.trim().toLowerCase()
  const visibles = (lista ?? []).filter(
    (c) => !q || `${c.nombre} ${c.ubicacion}`.toLowerCase().includes(q)
  )
  const propios = visibles.filter((c) => mios.has(c.id))
  const otros = visibles.filter((c) => !mios.has(c.id))

  const tarjeta = (c: Club) => (
    <li key={c.id}>
      <Link
        to={`/comunidad/clubes/${c.id}`}
        className="flex items-center gap-4 bg-surface-container rounded-2xl p-4 hover:bg-surface-container-high"
      >
        <div className="w-12 h-12 rounded-full bg-primary-600/25 text-primary-400 flex items-center justify-center shrink-0">
          <span className="material-symbols-outlined">groups</span>
        </div>
        <div className="min-w-0 flex-1">
          <p className="font-semibold text-text-primary truncate">{c.nombre}</p>
          <p className="text-sm text-text-secondary truncate">
            {c.miembros} {c.miembros === 1 ? 'miembro' : 'miembros'}
            {c.ubicacion && ` · ${c.ubicacion}`}
            {c.conAprobacion && ' · con aprobación'}
          </p>
        </div>
        <span className="material-symbols-outlined text-text-secondary">chevron_right</span>
      </Link>
    </li>
  )

  return (
    <main className="w-full min-h-screen bg-surface contenedor-escritorio lg:max-w-4xl pb-28">
      <header className="flex items-center gap-3 px-margin py-4 lg:px-0">
        <button
          onClick={() => navigate('/comunidad')}
          aria-label="Volver"
          className="w-11 h-11 rounded-full bg-surface-container flex items-center justify-center text-text-primary"
        >
          <span className="material-symbols-outlined">arrow_back</span>
        </button>
        <h1 className="flex-1 font-headline-md text-headline-md text-text-primary">Clubes</h1>
        {sesion && (
          <button
            onClick={() => {
              setError(null)
              setCreando(true)
            }}
            aria-label="Crear club"
            data-testid="crear-club"
            className="w-11 h-11 rounded-full bg-primary-600 text-on-primary flex items-center justify-center"
          >
            <span className="material-symbols-outlined">add</span>
          </button>
        )}
      </header>
      <div className="px-margin lg:px-0 space-y-5">
        <input
          aria-label="Buscar clubes"
          placeholder="Buscar clubes…"
          className={campo}
          value={consulta}
          onChange={(e) => setConsulta(e.target.value)}
        />
        {lista === null ? (
          <p className="text-text-secondary">Cargando…</p>
        ) : !clubes.disponible() ? (
          <p className="text-text-secondary">Los clubes necesitan conexión con el servidor.</p>
        ) : (
          <>
            {propios.length > 0 && (
              <section aria-labelledby="mis-clubes" className="space-y-2">
                <h2 id="mis-clubes" className="font-title-md text-title-md text-text-primary">
                  Mis clubes
                </h2>
                <ul className="space-y-2">{propios.map(tarjeta)}</ul>
              </section>
            )}
            <section aria-labelledby="descubrir" className="space-y-2">
              <h2 id="descubrir" className="font-title-md text-title-md text-text-primary">
                Descubrir
              </h2>
              {otros.length === 0 ? (
                <p className="text-text-secondary text-sm">No hay más clubes. ¡Crea el primero!</p>
              ) : (
                <ul className="space-y-2" data-testid="lista-clubes">
                  {otros.map(tarjeta)}
                </ul>
              )}
            </section>
          </>
        )}
      </div>
      <Dialogo
        titulo="Crear club"
        abierto={creando}
        onCerrar={() => setCreando(false)}
        acciones={
          <>
            <BotonTexto onClick={() => setCreando(false)}>Cancelar</BotonTexto>
            <BotonTexto onClick={() => void crear()}>Crear</BotonTexto>
          </>
        }
      >
        <input
          aria-label="Nombre del club"
          placeholder="Nombre"
          className={campo}
          value={form.nombre}
          maxLength={60}
          onChange={(e) => setForm({ ...form, nombre: e.target.value })}
        />
        <textarea
          aria-label="Descripción del club"
          placeholder="Descripción"
          rows={3}
          className={`${campo} h-auto py-2`}
          value={form.descripcion}
          onChange={(e) => setForm({ ...form, descripcion: e.target.value })}
        />
        <input
          aria-label="Ubicación del club"
          placeholder="Ubicación"
          className={campo}
          value={form.ubicacion}
          onChange={(e) => setForm({ ...form, ubicacion: e.target.value })}
        />
        <div className="flex items-center justify-between">
          <span className="text-text-primary">Aprobar a quien se una</span>
          <Interruptor
            activo={form.conAprobacion}
            onCambiar={(v) => setForm({ ...form, conAprobacion: v })}
            etiqueta="Aprobar a quien se una"
          />
        </div>
        <MensajeError mensaje={error} />
      </Dialogo>
    </main>
  )
}
