import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

export function ConfiguracionNotificaciones() {
  const navigate = useNavigate();
  const [notifLikes, setNotifLikes] = useState(true);
  const [notifComentarios, setNotifComentarios] = useState(true);
  const [notifSeguidores, setNotifSeguidores] = useState(true);
  const [notifLive, setNotifLive] = useState(true);
  const [notifEventos, setNotifEventos] = useState(true);
  const [notifEmail, setNotifEmail] = useState(false);

  return (
    <div className="w-full min-h-screen bg-surface flex flex-col">
      <header className="px-margin py-4 border-b border-border-subtle">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/ajustes')}
            className="w-10 h-10 rounded-full bg-surface-container flex items-center justify-center text-text-secondary hover:text-text-primary transition-colors"
          >
            <span className="material-symbols-outlined">arrow_back</span>
          </button>
          <h1 className="font-headline-md text-headline-md text-text-primary">
            Notificaciones
          </h1>
        </div>
      </header>

      <main className="flex-1 px-margin py-6 space-y-6 pb-24">
        <section>
          <h2 className="font-title-md text-title-md text-text-primary mb-3">
            Notificaciones push
          </h2>
          <div className="space-y-2">
            <div className="flex items-center justify-between px-4 py-3 bg-surface-container rounded-xl">
              <div className="flex items-center gap-3">
                <span className="material-symbols-outlined text-[20px] text-danger">
                  favorite
                </span>
                <div>
                  <div className="font-body-md text-body-md text-text-primary font-semibold">
                    Likes
                  </div>
                  <p className="font-body-sm text-body-sm text-text-secondary">
                    Cuando alguien da like a tu actividad
                  </p>
                </div>
              </div>
              <label className="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  className="sr-only peer"
                  checked={notifLikes}
                  onChange={(e) => setNotifLikes(e.target.checked)}
                />
                <div className="w-11 h-6 bg-surface-container-high peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-primary-600 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-600"></div>
              </label>
            </div>

            <div className="flex items-center justify-between px-4 py-3 bg-surface-container rounded-xl">
              <div className="flex items-center gap-3">
                <span className="material-symbols-outlined text-[20px] text-primary-500">
                  chat_bubble
                </span>
                <div>
                  <div className="font-body-md text-body-md text-text-primary font-semibold">
                    Comentarios
                  </div>
                  <p className="font-body-sm text-body-sm text-text-secondary">
                    Cuando alguien comenta tu actividad
                  </p>
                </div>
              </div>
              <label className="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  className="sr-only peer"
                  checked={notifComentarios}
                  onChange={(e) => setNotifComentarios(e.target.checked)}
                />
                <div className="w-11 h-6 bg-surface-container-high peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-primary-600 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-600"></div>
              </label>
            </div>

            <div className="flex items-center justify-between px-4 py-3 bg-surface-container rounded-xl">
              <div className="flex items-center gap-3">
                <span className="material-symbols-outlined text-[20px] text-secondary-500">
                  person_add
                </span>
                <div>
                  <div className="font-body-md text-body-md text-text-primary font-semibold">
                    Nuevos seguidores
                  </div>
                  <p className="font-body-sm text-body-sm text-text-secondary">
                    Cuando alguien te sigue
                  </p>
                </div>
              </div>
              <label className="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  className="sr-only peer"
                  checked={notifSeguidores}
                  onChange={(e) => setNotifSeguidores(e.target.checked)}
                />
                <div className="w-11 h-6 bg-surface-container-high peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-primary-600 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-600"></div>
              </label>
            </div>

            <div className="flex items-center justify-between px-4 py-3 bg-surface-container rounded-xl">
              <div className="flex items-center gap-3">
                <span className="material-symbols-outlined text-[20px] text-secondary-500">
                  sensors
                </span>
                <div>
                  <div className="font-body-md text-body-md text-text-primary font-semibold">
                    Carreras en vivo
                  </div>
                  <p className="font-body-sm text-body-sm text-text-secondary">
                    Cuando alguien inicia seguimiento en vivo
                  </p>
                </div>
              </div>
              <label className="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  className="sr-only peer"
                  checked={notifLive}
                  onChange={(e) => setNotifLive(e.target.checked)}
                />
                <div className="w-11 h-6 bg-surface-container-high peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-primary-600 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-600"></div>
              </label>
            </div>

            <div className="flex items-center justify-between px-4 py-3 bg-surface-container rounded-xl">
              <div className="flex items-center gap-3">
                <span className="material-symbols-outlined text-[20px] text-tertiary">
                  event
                </span>
                <div>
                  <div className="font-body-md text-body-md text-text-primary font-semibold">
                    Eventos
                  </div>
                  <p className="font-body-sm text-body-sm text-text-secondary">
                    Recordatorios de eventos y nuevos eventos
                  </p>
                </div>
              </div>
              <label className="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  className="sr-only peer"
                  checked={notifEventos}
                  onChange={(e) => setNotifEventos(e.target.checked)}
                />
                <div className="w-11 h-6 bg-surface-container-high peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-primary-600 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-600"></div>
              </label>
            </div>
          </div>
        </section>

        <section>
          <h2 className="font-title-md text-title-md text-text-primary mb-3">
            Notificaciones por email
          </h2>
          <div className="space-y-2">
            <div className="flex items-center justify-between px-4 py-3 bg-surface-container rounded-xl">
              <div className="flex items-center gap-3">
                <span className="material-symbols-outlined text-[20px] text-text-secondary">
                  mail
                </span>
                <div>
                  <div className="font-body-md text-body-md text-text-primary font-semibold">
                    Resumen semanal
                  </div>
                  <p className="font-body-sm text-body-sm text-text-secondary">
                    Recibe un resumen de tu actividad cada semana
                  </p>
                </div>
              </div>
              <label className="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  className="sr-only peer"
                  checked={notifEmail}
                  onChange={(e) => setNotifEmail(e.target.checked)}
                />
                <div className="w-11 h-6 bg-surface-container-high peer-focus:outline-none peer-focus:ring-2 peer-focus:ring-primary-600 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-600"></div>
              </label>
            </div>
          </div>
        </section>

        <section>
          <h2 className="font-title-md text-title-md text-text-primary mb-3">
            Frecuencia
          </h2>
          <div className="bg-surface-container rounded-xl p-4">
            <p className="font-body-sm text-body-sm text-text-secondary mb-3">
              Los likes se agrupan cada 10 o cada 6 horas para evitar notificaciones excesivas.
            </p>
            <div className="flex items-center gap-2 text-primary-500">
              <span className="material-symbols-outlined text-[18px]">info</span>
              <span className="font-label-md text-label-md">
                Configuración optimizada
              </span>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}
