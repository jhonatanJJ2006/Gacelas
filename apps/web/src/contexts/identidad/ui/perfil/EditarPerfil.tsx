import { useRef, useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { leerFoto } from '../../../../shared/lib/fotos'
import { MensajeError } from '../../../../shared/ui/ui'
import { useIdentidad } from '../../application/estado'
import { FotoPerfil } from './Perfil'

const entrada =
  'w-full h-12 px-4 bg-surface-container border border-border-subtle rounded-xl text-text-primary placeholder-text-secondary focus:outline-none focus:ring-2 focus:ring-primary-600 focus:border-transparent'
const GENEROS = ['Masculino', 'Femenino', 'Otro', 'Prefiero no decir']

export function EditarPerfil() {
  const navigate = useNavigate()
  const sesion = useIdentidad((s) => s.sesion)
  const actualizarPerfil = useIdentidad((s) => s.actualizarPerfil)
  const archivo = useRef<HTMLInputElement>(null)
  const [nombre, setNombre] = useState(sesion?.nombre ?? '')
  const [username, setUsername] = useState(sesion?.username ?? '')
  const [bio, setBio] = useState(sesion?.bio ?? '')
  const [ubicacion, setUbicacion] = useState(sesion?.ubicacion ?? '')
  const [edad, setEdad] = useState(sesion?.edad ?? '')
  const [genero, setGenero] = useState(sesion?.genero ?? 'Prefiero no decir')
  const [foto, setFoto] = useState<string | null>(sesion?.foto ?? null)
  const [error, setError] = useState<string | null>(null)
  const ajustes = useIdentidad((s) => s.ajustes)
  const actualizarAjustes = useIdentidad((s) => s.actualizarAjustes)
  const [fis, setFis] = useState({
    pesoKg: ajustes.pesoKg?.toString() ?? '',
    alturaCm: ajustes.alturaCm?.toString() ?? '',
  })
  const [nivel, setNivel] = useState(ajustes.nivelDeclarado)
  const [favorita, setFavorita] = useState(ajustes.distanciaFavorita)
  const [metaKm, setMetaKm] = useState(ajustes.metaKmSemana)
  const [dias, setDias] = useState(ajustes.metaSemanal)

  if (!sesion) return <Navigate to="/bienvenida" replace />

  const fallar = (m: string) => {
    setError(m)
    requestAnimationFrame(() =>
      document
        .querySelector('[role="alert"]')
        ?.scrollIntoView?.({ block: 'center', behavior: 'smooth' })
    )
  }

  const handleGuardar = async () => {
    if (nombre.trim().length < 2) return fallar('Ingresa tu nombre')
    const usuario = username.trim().replace(/^@/, '')
    if (!/^[a-zA-Z0-9_.]{3,20}$/.test(usuario))
      return fallar('El usuario debe tener de 3 a 20 letras, números, punto o guion bajo')
    try {
      await actualizarPerfil({
        ...sesion,
        nombre: nombre.trim(),
        username: usuario,
        bio,
        ubicacion: ubicacion.trim(),
        edad,
        genero,
        foto,
      })
      const num = (v: string, min: number, max: number) => {
        const n = Number(v)
        return v.trim() && n >= min && n <= max ? Math.round(n) : null
      }
      await actualizarAjustes({
        pesoKg: num(fis.pesoKg, 25, 250),
        alturaCm: num(fis.alturaCm, 100, 230),
        nivelDeclarado: nivel,
        distanciaFavorita: favorita,
        metaKmSemana: metaKm,
        metaSemanal: dias,
      })
    } catch (e) {
      return fallar(e instanceof Error ? e.message : 'No se pudo guardar el perfil')
    }
    navigate('/perfil')
  }

  const elegirFoto = async (f: File | undefined) => {
    if (!f) return
    if (!f.type.startsWith('image/')) return setError('Elige una imagen')
    setFoto(await leerFoto(f))
  }

  return (
    <div className="w-full min-h-screen bg-surface flex flex-col">
      <header className="px-margin h-16 flex items-center bg-bg-secondary">
        <div className="flex items-center gap-1 w-full">
          <button
            onClick={() => navigate('/perfil')}
            aria-label="Volver"
            className="w-12 h-12 -ml-3 shrink-0 flex items-center justify-center text-text-primary"
          >
            <span className="material-symbols-outlined">arrow_back</span>
          </button>
          <h1 className="flex-1 min-w-0 font-title-md text-title-md text-text-primary">
            Editar perfil
          </h1>
          <button
            onClick={() => void handleGuardar()}
            className="px-2 py-2 text-primary-500 font-label-lg text-label-lg"
          >
            Guardar
          </button>
        </div>
      </header>

      <main className="lg:w-full lg:max-w-3xl lg:mx-auto flex-1 px-margin py-6 space-y-6 pb-24">
        <div className="flex flex-col items-center">
          <div className="relative">
            <FotoPerfil foto={foto} nombre={nombre || sesion.nombre} className="w-24 h-24" />
            <button
              onClick={() => archivo.current?.click()}
              aria-label="Elegir foto"
              className="absolute bottom-0 right-0 w-8 h-8 rounded-full bg-primary-600 flex items-center justify-center text-on-primary shadow-lg"
            >
              <span className="material-symbols-outlined text-[18px]">camera_alt</span>
            </button>
          </div>
          <input
            ref={archivo}
            type="file"
            accept="image/*"
            className="hidden"
            data-testid="input-foto"
            onChange={(e) => void elegirFoto(e.target.files?.[0])}
          />
          <div className="flex gap-4 mt-3">
            <button
              onClick={() => archivo.current?.click()}
              className="inline-block py-2 text-primary-500 font-label-lg text-label-lg hover:text-primary-400"
            >
              Cambiar foto
            </button>
            {foto && (
              <button
                onClick={() => setFoto(null)}
                className="text-text-secondary font-label-lg text-label-lg hover:text-danger"
              >
                Quitar foto
              </button>
            )}
          </div>
        </div>

        <div className="space-y-4">
          <div>
            <label htmlFor="nombre" className="block text-sm font-medium text-text-primary mb-2">
              Nombre completo
            </label>
            <input
              id="nombre"
              type="text"
              value={nombre}
              onChange={(e) => setNombre(e.target.value)}
              className={entrada}
            />
          </div>
          <div>
            <label htmlFor="username" className="block text-sm font-medium text-text-primary mb-2">
              Nombre de usuario
            </label>
            <input
              id="username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className={entrada}
            />
          </div>
          <div>
            <label htmlFor="ubicacion" className="block text-sm font-medium text-text-primary mb-2">
              Ubicación
            </label>
            <input
              id="ubicacion"
              type="text"
              value={ubicacion}
              onChange={(e) => setUbicacion(e.target.value)}
              className={entrada}
            />
          </div>
          <div>
            <label htmlFor="bio" className="block text-sm font-medium text-text-primary mb-2">
              Bio
            </label>
            <textarea
              id="bio"
              rows={4}
              maxLength={150}
              value={bio}
              onChange={(e) => setBio(e.target.value.slice(0, 150))}
              placeholder="Cuéntanos sobre ti..."
              className={`${entrada} h-auto py-3 resize-none`}
            />
            <p className="text-xs text-text-secondary mt-1">{bio.length}/150 caracteres</p>
          </div>
        </div>

        <div className="bg-surface-container rounded-2xl p-4">
          <h2 className="font-title-md text-title-md text-text-primary mb-3">
            Información adicional
          </h2>
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <label htmlFor="edad" className="font-body-md text-body-md text-text-primary">
                Edad
              </label>
              <input
                id="edad"
                type="number"
                min={5}
                max={120}
                placeholder="25"
                value={edad}
                onChange={(e) => setEdad(e.target.value.replace(/\D/g, '').slice(0, 3))}
                className="w-20 h-10 px-3 bg-surface-container-high border border-border-subtle rounded-lg text-text-primary text-center focus:outline-none focus:ring-2 focus:ring-primary-600"
              />
            </div>
            <div className="flex items-center justify-between">
              <label htmlFor="genero" className="font-body-md text-body-md text-text-primary">
                Género
              </label>
              <select
                id="genero"
                value={genero}
                onChange={(e) => setGenero(e.target.value)}
                className="w-40 h-10 px-3 bg-surface-container-high border border-border-subtle rounded-lg text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-600"
              >
                {GENEROS.map((g) => (
                  <option key={g}>{g}</option>
                ))}
              </select>
            </div>
          </div>
        </div>
        <Link
          to="/perfil/avatar"
          className="flex items-center justify-center gap-2 h-11 rounded-xl border border-primary/50 text-primary font-semibold"
        >
          <span className="material-symbols-outlined">view_in_ar</span>Personalizar avatar 3D
        </Link>

        <section className="bg-surface-container rounded-2xl p-4 space-y-3">
          <div className="flex justify-between items-baseline">
            <h2 className="font-title-md text-title-md text-text-primary">Datos fisiológicos</h2>
            <span className="text-body-sm text-text-secondary">Privado</span>
          </div>
          <div className="grid grid-cols-2 gap-3">
            {(
              [
                ['pesoKg', 'Peso (kg)', '68'],
                ['alturaCm', 'Altura (cm)', '174'],
              ] as const
            ).map(([k, t, ph]) => (
              <label key={k} className="block">
                <span className="block text-sm text-text-secondary mb-1">{t}</span>
                <input
                  type="number"
                  inputMode="numeric"
                  aria-label={t}
                  placeholder={ph}
                  value={fis[k]}
                  onChange={(e) =>
                    setFis({ ...fis, [k]: e.target.value.replace(/[^\d]/g, '').slice(0, 3) })
                  }
                  className={entrada}
                />
              </label>
            ))}
          </div>
        </section>

        <section className="bg-surface-container rounded-2xl p-4 space-y-3">
          <h2 className="font-title-md text-title-md text-text-primary">Perfil atlético</h2>
          <div className="space-y-2" role="radiogroup" aria-label="Nivel de corredor">
            {(
              [
                ['principiante', 'Principiante', '0 a 5 km por salida'],
                ['intermedio', 'Intermedio', '5 a 15 km de fondo'],
                ['avanzado', 'Avanzado', '+15 km fondo y ultra trail'],
              ] as const
            ).map(([id, t, d]) => (
              <button
                key={id}
                role="radio"
                aria-checked={nivel === id}
                onClick={() => setNivel(id)}
                className={`w-full flex items-center justify-between p-3 rounded-xl border text-left ${nivel === id ? 'border-primary bg-primary-600/10' : 'border-border-subtle'}`}
              >
                <span>
                  <span className="block text-text-primary font-semibold">{t}</span>
                  <span className="block text-body-sm text-text-secondary">{d}</span>
                </span>
                <span className="material-symbols-outlined text-primary">
                  {nivel === id ? 'radio_button_checked' : 'radio_button_unchecked'}
                </span>
              </button>
            ))}
          </div>
          <div>
            <span className="block text-sm text-text-secondary mb-1">Distancia favorita</span>
            <div className="grid grid-cols-4 gap-2">
              {(['5K', '10K', '21K', '42K'] as const).map((d) => (
                <button
                  key={d}
                  aria-pressed={favorita === d}
                  onClick={() => setFavorita(d)}
                  className={`h-10 rounded-lg border text-label-md ${favorita === d ? 'border-primary bg-primary-600/15 text-primary' : 'border-border-subtle text-text-secondary'}`}
                >
                  {d}
                </button>
              ))}
            </div>
          </div>
        </section>

        <section className="bg-surface-container rounded-2xl p-4 space-y-3">
          <h2 className="font-title-md text-title-md text-text-primary">
            Objetivos de entrenamiento
          </h2>
          <label className="block">
            <span className="flex justify-between text-sm text-text-secondary mb-1">
              <span>Meta de kilometraje</span>
              <span className="text-primary font-semibold" data-testid="meta-km">
                {metaKm} km / semana
              </span>
            </span>
            <input
              type="range"
              min={5}
              max={80}
              step={5}
              aria-label="Meta de kilometraje semanal"
              value={metaKm}
              onChange={(e) => setMetaKm(Number(e.target.value))}
              className="w-full accent-primary-600"
            />
          </label>
          <div>
            <span className="block text-sm text-text-secondary mb-1">
              Días de entrenamiento por semana
            </span>
            <div className="grid grid-cols-7 gap-1">
              {[1, 2, 3, 4, 5, 6, 7].map((n) => (
                <button
                  key={n}
                  aria-pressed={dias === n}
                  aria-label={`${n} días por semana`}
                  onClick={() => setDias(n)}
                  className={`h-9 rounded-lg text-label-md ${dias === n ? 'bg-primary-600 text-on-primary' : 'bg-surface-container-high text-text-secondary'}`}
                >
                  {n}
                </button>
              ))}
            </div>
          </div>
        </section>
        <MensajeError mensaje={error} />
      </main>
    </div>
  )
}
