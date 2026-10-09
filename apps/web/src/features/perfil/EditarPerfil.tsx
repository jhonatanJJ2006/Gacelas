import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

export function EditarPerfil() {
  const navigate = useNavigate()
  const [nombre, setNombre] = useState('Jhonatan Runner')
  const [username, setUsername] = useState('jhonatan_runs')
  const [bio, setBio] = useState('Corredor de Loja. Me encanta trail running.')
  const [ubicacion, setUbicacion] = useState('Loja, Ecuador')

  const handleGuardar = () => {
    navigate('/perfil')
  }

  return (
    <div className="w-full min-h-screen bg-surface flex flex-col">
      <header className="px-margin py-4 border-b border-border-subtle">
        <div className="flex items-center justify-between">
          <button
            onClick={() => navigate('/perfil')}
            className="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center text-text-secondary hover:text-text-primary transition-colors"
          >
            <span className="material-symbols-outlined">arrow_back</span>
          </button>
          <h1 className="font-headline-md text-headline-md text-text-primary">Editar perfil</h1>
          <button
            onClick={handleGuardar}
            className="px-4 py-2 bg-primary-600 hover:bg-primary-700 text-bg-primary font-label-lg text-label-lg rounded-lg transition-colors"
          >
            Guardar
          </button>
        </div>
      </header>

      <main className="flex-1 px-margin py-6 space-y-6 pb-24">
        <div className="flex flex-col items-center">
          <div className="relative">
            <div className="w-24 h-24 rounded-full bg-surface-variant border-2 border-primary-600"></div>
            <button className="absolute bottom-0 right-0 w-8 h-8 rounded-full bg-primary-600 flex items-center justify-center text-bg-primary shadow-lg">
              <span className="material-symbols-outlined text-[18px]">camera_alt</span>
            </button>
          </div>
          <button className="mt-3 text-primary-500 font-label-lg text-label-lg hover:text-primary-400">
            Cambiar foto
          </button>
        </div>

        <div className="space-y-4">
          <div>
            <label htmlFor="nombre" className="block text-sm font-medium text-text-primary mb-2">
              Nombre completo
            </label>
            <input
              type="text"
              id="nombre"
              value={nombre}
              onChange={(e) => setNombre(e.target.value)}
              className="w-full h-12 px-4 bg-surface-container border border-border-subtle rounded-xl text-text-primary placeholder-text-secondary focus:outline-none focus:ring-2 focus:ring-primary-600 focus:border-transparent"
            />
          </div>

          <div>
            <label htmlFor="username" className="block text-sm font-medium text-text-primary mb-2">
              Nombre de usuario
            </label>
            <input
              type="text"
              id="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="w-full h-12 px-4 bg-surface-container border border-border-subtle rounded-xl text-text-primary placeholder-text-secondary focus:outline-none focus:ring-2 focus:ring-primary-600 focus:border-transparent"
            />
          </div>

          <div>
            <label htmlFor="ubicacion" className="block text-sm font-medium text-text-primary mb-2">
              Ubicación
            </label>
            <div className="relative">
              <input
                type="text"
                id="ubicacion"
                value={ubicacion}
                onChange={(e) => setUbicacion(e.target.value)}
                className="w-full h-12 pl-12 pr-4 bg-surface-container border border-border-subtle rounded-xl text-text-primary placeholder-text-secondary focus:outline-none focus:ring-2 focus:ring-primary-600 focus:border-transparent"
              />
              <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-text-secondary">
                location_on
              </span>
            </div>
          </div>

          <div>
            <label htmlFor="bio" className="block text-sm font-medium text-text-primary mb-2">
              Bio
            </label>
            <textarea
              id="bio"
              value={bio}
              onChange={(e) => setBio(e.target.value)}
              rows={4}
              className="w-full px-4 py-3 bg-surface-container border border-border-subtle rounded-xl text-text-primary placeholder-text-secondary focus:outline-none focus:ring-2 focus:ring-primary-600 focus:border-transparent resize-none"
              placeholder="Cuéntanos sobre ti..."
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
              <span className="font-body-md text-body-md text-text-primary">Edad</span>
              <input
                type="number"
                placeholder="25"
                className="w-20 h-10 px-3 bg-surface-container-high border border-border-subtle rounded-lg text-text-primary text-center focus:outline-none focus:ring-2 focus:ring-primary-600"
              />
            </div>
            <div className="flex items-center justify-between">
              <span className="font-body-md text-body-md text-text-primary">Género</span>
              <select className="w-32 h-10 px-3 bg-surface-container-high border border-border-subtle rounded-lg text-text-primary focus:outline-none focus:ring-2 focus:ring-primary-600">
                <option>Masculino</option>
                <option>Femenino</option>
                <option>Otro</option>
                <option>Prefiero no decir</option>
              </select>
            </div>
          </div>
        </div>
      </main>
    </div>
  )
}
