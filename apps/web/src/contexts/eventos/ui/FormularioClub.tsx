import { useEffect, useState, type ReactNode } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { MapaEditable } from '../../../shared/ui/MapaEditable'
import { Interruptor, MensajeError } from '../../../shared/ui/ui'
import {
  borradorDesdeClub,
  borradorVacio,
  clubes,
  DIAS_CORTOS,
  ETIQUETA_NIVEL,
  leerRitmo,
  LIMITE_CREACION,
  LIMITE_DIARIO,
  textoRitmo,
  urlPublica,
  validarClub,
  type BorradorClub,
  type Club,
  type NivelGrupo,
  type TipoClub,
} from '../api'

const campo =
  'w-full h-11 px-3 bg-surface-container border border-border-subtle rounded-lg text-text-primary placeholder-text-secondary'

function Campo({
  etiqueta,
  children,
  ayuda,
}: {
  etiqueta: string
  children: ReactNode
  ayuda?: string
}) {
  return (
    <label className="block space-y-1">
      <span className="text-xs font-semibold uppercase tracking-wide text-text-secondary">
        {etiqueta}
      </span>
      {children}
      {ayuda && <span className="block text-[11px] text-text-secondary">{ayuda}</span>}
    </label>
  )
}

/** Registrar o editar una comunidad o un grupo de running. */
export function FormularioClub() {
  const navigate = useNavigate()
  const { id } = useParams()
  const [params] = useSearchParams()
  const tipoInicial: TipoClub = params.get('tipo') === 'comunidad' ? 'comunidad' : 'grupo'
  const [b, setB] = useState<BorradorClub>(() => ({
    ...borradorVacio(tipoInicial),
    comunidadId: params.get('comunidad'),
  }))
  const [todos, setTodos] = useState<Club[]>([])
  const [error, setError] = useState<string | null>(null)
  const [guardando, setGuardando] = useState(false)
  const [ritmo, setRitmo] = useState('')
  const [cargado, setCargado] = useState(!id)

  useEffect(() => {
    if (!clubes.disponible()) return
    void clubes
      .listar()
      .then((r) => setTodos(r.todos))
      .catch(() => undefined)
    if (id)
      void clubes.detalle(id).then((c) => {
        if (c) {
          setB(borradorDesdeClub(c))
          setRitmo(textoRitmo(c.ritmoSegKm).replace(' min/km', ''))
        }
        setCargado(true)
      })
  }, [id])

  const set = <K extends keyof BorradorClub>(k: K, v: BorradorClub[K]) =>
    setB((x) => ({ ...x, [k]: v }))
  const grupo = b.tipo === 'grupo'
  const comunidades = todos.filter((c) => c.tipo === 'comunidad' && !c.oculto)

  const subir = async (k: 'logo' | 'portada', f: File | undefined) => {
    if (!f) return
    if (f.size > 5 * 1024 * 1024) return setError('La imagen debe pesar menos de 5 MB')
    try {
      set(k, await clubes.subirImagen(f, f.name))
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo subir la imagen')
    }
  }

  const guardar = async () => {
    const borrador = { ...b, ritmoSegKm: ritmo.trim() ? leerRitmo(ritmo) : null }
    if (ritmo.trim() && borrador.ritmoSegKm == null)
      return setError('Escribe el ritmo como 5:30 (minutos por km)')
    const e = validarClub(borrador, todos, id)
    if (e) return setError(e)
    setGuardando(true)
    setError(null)
    try {
      if (id) {
        await clubes.editar(id, borrador)
        navigate(`/comunidad/clubes/${id}`, { replace: true })
      } else {
        const c = await clubes.crear(borrador)
        navigate(`/comunidad/clubes/${c.id}`, { replace: true })
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo guardar')
    } finally {
      setGuardando(false)
    }
  }

  const titulo = id
    ? grupo
      ? 'Editar grupo'
      : 'Editar comunidad'
    : grupo
      ? 'Registrar grupo'
      : 'Registrar comunidad'

  return (
    <main className="w-full min-h-screen bg-surface contenedor-escritorio lg:max-w-3xl pb-32">
      <header className="flex items-center gap-3 px-margin py-4 lg:px-0">
        <button
          onClick={() => navigate(-1)}
          aria-label="Volver"
          className="w-11 h-11 rounded-full bg-surface-container flex items-center justify-center text-text-primary"
        >
          <span className="material-symbols-outlined">arrow_back</span>
        </button>
        <h1 className="flex-1 font-headline-md text-headline-md text-text-primary">{titulo}</h1>
      </header>
      {!cargado ? (
        <p className="px-margin text-text-secondary">Cargando…</p>
      ) : (
        <form
          className="px-margin lg:px-0 space-y-5"
          onSubmit={(e) => {
            e.preventDefault()
            void guardar()
          }}
        >
          {!id && (
            <div
              role="radiogroup"
              aria-label="Qué quieres registrar"
              className="grid grid-cols-2 gap-2"
            >
              {(['grupo', 'comunidad'] as const).map((t) => (
                <button
                  type="button"
                  key={t}
                  role="radio"
                  aria-checked={b.tipo === t}
                  onClick={() => set('tipo', t)}
                  className={`tarjeta-c2 p-3 text-left ${b.tipo === t ? 'border-primary-600 bg-primary-600/10' : ''}`}
                >
                  <span className="block font-semibold text-text-primary">
                    {t === 'grupo' ? 'Grupo de running' : 'Comunidad'}
                  </span>
                  <span className="block text-[11px] text-text-secondary">
                    {t === 'grupo'
                      ? 'Un equipo que entrena junto'
                      : 'Un colectivo amplio, como una universidad o una ciudad'}
                  </span>
                </button>
              ))}
            </div>
          )}

          <section className="tarjeta-c2 p-4 space-y-4">
            <Campo etiqueta="Nombre">
              <input
                aria-label="Nombre"
                className={campo}
                value={b.nombre}
                maxLength={60}
                placeholder={grupo ? 'Gacelas del Jipiro' : 'Runners Loja'}
                onChange={(e) => set('nombre', e.target.value)}
              />
            </Campo>
            <Campo etiqueta="Descripción">
              <textarea
                aria-label="Descripción"
                className={`${campo} h-24 py-2`}
                value={b.descripcion}
                maxLength={500}
                placeholder="Qué hacen, a quién buscan"
                onChange={(e) => set('descripcion', e.target.value)}
              />
            </Campo>
            <Campo etiqueta="Ciudad o zona">
              <input
                aria-label="Ciudad o zona"
                className={campo}
                value={b.ciudad}
                maxLength={60}
                onChange={(e) => set('ciudad', e.target.value)}
              />
            </Campo>
            <Campo etiqueta="Redes o contacto (opcional)">
              <input
                aria-label="Redes o contacto"
                className={campo}
                value={b.contacto}
                maxLength={200}
                placeholder="@instagram, WhatsApp o correo"
                onChange={(e) => set('contacto', e.target.value)}
              />
            </Campo>
          </section>

          {grupo && (
            <section className="tarjeta-c2 p-4 space-y-4" aria-label="Entrenamientos">
              <h2 className="font-title-md text-title-md text-text-primary">Entrenamientos</h2>
              {comunidades.length > 0 && (
                <Campo etiqueta="Comunidad (opcional)">
                  <select
                    aria-label="Comunidad"
                    className={campo}
                    value={b.comunidadId ?? ''}
                    onChange={(e) => set('comunidadId', e.target.value || null)}
                  >
                    <option value="">Sin comunidad</option>
                    {comunidades.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.nombre}
                      </option>
                    ))}
                  </select>
                </Campo>
              )}
              <Campo etiqueta="Punto de encuentro">
                <input
                  aria-label="Punto de encuentro"
                  className={campo}
                  value={b.lugarEncuentro}
                  maxLength={120}
                  placeholder="Parque Jipiro, entrada principal"
                  onChange={(e) => set('lugarEncuentro', e.target.value)}
                />
              </Campo>
              <div className="rounded-xl overflow-hidden border border-border-subtle">
                <MapaEditable
                  className="h-48 bg-surface-container"
                  puntos={b.punto ? [b.punto] : []}
                  onTocar={(p) => set('punto', { lat: p.lat, lng: p.lng })}
                />
              </div>
              <div className="flex items-center gap-3 text-xs text-text-secondary">
                <button
                  type="button"
                  className="h-9 px-3 rounded-full border border-border-subtle text-text-primary"
                  onClick={() =>
                    navigator.geolocation?.getCurrentPosition((pos) =>
                      set('punto', { lat: pos.coords.latitude, lng: pos.coords.longitude })
                    )
                  }
                >
                  Usar mi ubicación
                </button>
                <span data-testid="punto-elegido">
                  {b.punto
                    ? `${b.punto.lat.toFixed(4)}, ${b.punto.lng.toFixed(4)}`
                    : 'Toca el mapa para marcar el punto'}
                </span>
              </div>
              <fieldset className="space-y-1">
                <legend className="text-xs font-semibold uppercase tracking-wide text-text-secondary">
                  Días de entrenamiento
                </legend>
                <div className="flex flex-wrap gap-1">
                  {DIAS_CORTOS.map((d, i) => {
                    const n = i + 1
                    const activo = b.dias.includes(n)
                    return (
                      <button
                        type="button"
                        key={d}
                        aria-pressed={activo}
                        onClick={() =>
                          set('dias', activo ? b.dias.filter((x) => x !== n) : [...b.dias, n])
                        }
                        className={`w-11 h-9 rounded-lg text-xs font-semibold border ${activo ? 'border-primary-600 bg-primary-600 text-on-primary' : 'border-border-subtle text-text-secondary'}`}
                      >
                        {d}
                      </button>
                    )
                  })}
                </div>
              </fieldset>
              <div className="grid grid-cols-2 gap-3">
                <Campo etiqueta="Hora">
                  <input
                    aria-label="Hora"
                    type="time"
                    className={campo}
                    value={b.hora ?? ''}
                    onChange={(e) => set('hora', e.target.value || null)}
                  />
                </Campo>
                <Campo etiqueta="Ritmo (min/km)">
                  <input
                    aria-label="Ritmo"
                    className={campo}
                    value={ritmo}
                    placeholder="5:30"
                    onChange={(e) => setRitmo(e.target.value)}
                  />
                </Campo>
                <Campo etiqueta="Nivel">
                  <select
                    aria-label="Nivel"
                    className={campo}
                    value={b.nivel}
                    onChange={(e) => set('nivel', e.target.value as NivelGrupo)}
                  >
                    {(Object.keys(ETIQUETA_NIVEL) as NivelGrupo[]).map((n) => (
                      <option key={n} value={n}>
                        {ETIQUETA_NIVEL[n]}
                      </option>
                    ))}
                  </select>
                </Campo>
                <Campo etiqueta="Cupo (opcional)">
                  <input
                    aria-label="Cupo"
                    type="number"
                    min={2}
                    max={1000}
                    className={campo}
                    value={b.cupo ?? ''}
                    placeholder="Sin límite"
                    onChange={(e) => set('cupo', e.target.value ? Number(e.target.value) : null)}
                  />
                </Campo>
              </div>
            </section>
          )}

          <section className="tarjeta-c2 p-4 space-y-4" aria-label="Imagen y privacidad">
            <div className="grid grid-cols-2 gap-3">
              {(['logo', 'portada'] as const).map((k) => (
                <label key={k} className="block space-y-1 cursor-pointer">
                  <span className="text-xs font-semibold uppercase tracking-wide text-text-secondary">
                    {k === 'logo' ? 'Logo (opcional)' : 'Portada (opcional)'}
                  </span>
                  <div className="h-20 rounded-xl border border-dashed border-border-subtle flex items-center justify-center overflow-hidden bg-surface-container">
                    {b[k] ? (
                      <img
                        src={urlPublica(b[k]) ?? ''}
                        alt=""
                        className="w-full h-full object-cover"
                      />
                    ) : (
                      <span className="material-symbols-outlined text-text-secondary">
                        add_photo_alternate
                      </span>
                    )}
                  </div>
                  <input
                    type="file"
                    accept="image/png,image/jpeg,image/webp"
                    className="sr-only"
                    aria-label={k === 'logo' ? 'Subir logo' : 'Subir portada'}
                    onChange={(e) => void subir(k, e.target.files?.[0])}
                  />
                </label>
              ))}
            </div>
            <div className="flex items-center justify-between gap-3">
              <div>
                <p className="text-text-primary font-semibold">
                  {b.conAprobacion ? 'Privado' : 'Público'}
                </p>
                <p className="text-[11px] text-text-secondary">
                  {b.conAprobacion
                    ? 'Tú apruebas cada solicitud de ingreso'
                    : 'Cualquier corredor puede unirse'}
                </p>
              </div>
              <Interruptor
                etiqueta="Requiere solicitud de ingreso"
                activo={b.conAprobacion}
                onCambiar={(v) => set('conAprobacion', v)}
              />
            </div>
          </section>

          <MensajeError mensaje={error} />
          {!id && (
            <p className="text-[11px] text-text-secondary">
              Puedes registrar hasta {LIMITE_CREACION.grupo} grupos y {LIMITE_CREACION.comunidad}{' '}
              comunidades, y no más de {LIMITE_DIARIO} por día. Al registrar quedas como
              administrador. Cualquier corredor puede reportarlo si incumple las normas.
            </p>
          )}
          <button
            type="submit"
            disabled={guardando}
            data-testid="guardar-club"
            className="w-full h-12 rounded-xl bg-primary-600 text-on-primary font-semibold disabled:opacity-60"
          >
            {guardando ? 'Guardando…' : id ? 'Guardar cambios' : titulo}
          </button>
        </form>
      )}
    </main>
  )
}
