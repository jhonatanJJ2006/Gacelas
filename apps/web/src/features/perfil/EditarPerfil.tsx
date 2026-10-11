import { useRef, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { MensajeError } from '../../components/ui'
import { useApp } from '../../store/app'
import { FotoPerfil } from './Perfil'
import { leerFoto } from '../../lib/logica'

const entrada =
  'w-full h-12 px-4 bg-surface-container border border-border-subtle rounded-xl text-text-primary placeholder-text-secondary focus:outline-none focus:ring-2 focus:ring-primary-600 focus:border-transparent'
const GENEROS = ['Masculino', 'Femenino', 'Otro', 'Prefiero no decir']

export function EditarPerfil() {
  const navigate = useNavigate()
  const sesion = useApp((s) => s.sesion)
  const actualizarPerfil = useApp((s) => s.actualizarPerfil)
  const archivo = useRef<HTMLInputElement>(null)
  const [nombre, setNombre] = useState(sesion?.nombre ?? '')
  const [username, setUsername] = useState(sesion?.username ?? '')
  const [bio, setBio] = useState(sesion?.bio ?? '')
  const [ubicacion, setUbicacion] = useState(sesion?.ubicacion ?? '')
  const [edad, setEdad] = useState(sesion?.edad ?? '')
  const [genero, setGenero] = useState(sesion?.genero ?? 'Prefiero no decir')
  const [foto, setFoto] = useState<string | null>(sesion?.foto ?? null)
  const [error, setError] = useState<string | null>(null)

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
      <header className="px-margin py-4 border-b border-border-subtle">
        <div className="flex items-center justify-between">
          <button
            onClick={() => navigate('/perfil')}
            aria-label="Volver"
            className="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center text-text-secondary hover:text-text-primary transition-colors"
          >
            <span className="material-symbols-outlined">arrow_back</span>
          </button>
          <h1 className="font-headline-md text-headline-md text-text-primary">Editar perfil</h1>
          <button
            onClick={() => void handleGuardar()}
            className="px-4 py-2 bg-primary-600 hover:bg-primary-700 text-bg-primary font-label-lg text-label-lg rounded-lg transition-colors"
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
              className="absolute bottom-0 right-0 w-8 h-8 rounded-full bg-primary-600 flex items-center justify-center text-bg-primary shadow-lg"
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
              className="text-primary-500 font-label-lg text-label-lg hover:text-primary-400"
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
        <MensajeError mensaje={error} />
      </main>
    </div>
  )
}
