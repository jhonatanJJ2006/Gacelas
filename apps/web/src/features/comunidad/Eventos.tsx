import { Link } from 'react-router-dom'

export function Eventos() {
  const eventos = [
    {
      id: '1',
      titulo: 'Carrera Parque Jipiro',
      fecha: 'Sábado 11 de Octubre',
      hora: '6:00 AM',
      ubicacion: 'Parque Jipiro, Loja',
      participantes: 15,
      distancia: 10,
    },
    {
      id: '2',
      titulo: 'Trail Running Podocarpus',
      fecha: 'Domingo 19 de Octubre',
      hora: '7:00 AM',
      ubicacion: 'Entrada Cajanuma',
      participantes: 8,
      distancia: 15,
    },
    {
      id: '3',
      titulo: 'Entrenamiento Intervalos Villonaco',
      fecha: 'Miércoles 15 de Octubre',
      hora: '6:30 AM',
      ubicacion: 'Base Cerro Villonaco',
      participantes: 12,
      distancia: 8,
    },
  ]

  return (
    <div className="flex flex-col w-full">
      <div className="px-margin pt-space-md pb-space-sm">
        <div className="flex items-center justify-between mb-4">
          <h1 className="font-headline-md text-headline-md text-text-primary">Eventos</h1>
          <button className="w-10 h-10 rounded-full bg-primary-600 flex items-center justify-center text-bg-primary hover:bg-primary-700 transition-colors shadow-lg">
            <span className="material-symbols-outlined">add</span>
          </button>
        </div>

        <div className="flex gap-2 mb-4">
          <button className="px-4 py-2 rounded-full font-label-md text-label-md bg-primary-600 text-bg-primary">
            Próximos
          </button>
          <button className="px-4 py-2 rounded-full font-label-md text-label-md bg-surface-container text-text-secondary hover:text-text-primary">
            Mis eventos
          </button>
          <button className="px-4 py-2 rounded-full font-label-md text-label-md bg-surface-container text-text-secondary hover:text-text-primary">
            Pasados
          </button>
        </div>
      </div>

      <div className="px-margin space-y-4 pb-space-3xl">
        {eventos.map((evento) => (
          <Link
            key={evento.id}
            to={`/comunidad/eventos/${evento.id}`}
            className="block bg-surface-container rounded-2xl border border-border-subtle overflow-hidden hover:border-primary-600/50 transition-colors"
          >
            <div className="h-40 bg-gradient-to-br from-primary-600 to-secondary-600 relative overflow-hidden">
              <div className="absolute inset-0 flex items-center justify-center">
                <span className="material-symbols-outlined text-[80px] text-white/20">event</span>
              </div>
              <div className="absolute top-4 right-4 bg-black/60 backdrop-blur-md px-3 py-1.5 rounded-full">
                <span className="text-white font-label-md text-label-md">
                  {evento.distancia} km
                </span>
              </div>
            </div>

            <div className="p-4">
              <h3 className="font-title-md text-title-md text-text-primary mb-2">
                {evento.titulo}
              </h3>

              <div className="space-y-2 mb-3">
                <div className="flex items-center gap-2 text-text-secondary">
                  <span className="material-symbols-outlined text-[18px] text-primary-500">
                    calendar_today
                  </span>
                  <span className="font-body-sm text-body-sm">
                    {evento.fecha}, {evento.hora}
                  </span>
                </div>
                <div className="flex items-center gap-2 text-text-secondary">
                  <span className="material-symbols-outlined text-[18px] text-secondary-500">
                    location_on
                  </span>
                  <span className="font-body-sm text-body-sm">{evento.ubicacion}</span>
                </div>
              </div>

              <div className="flex items-center justify-between pt-3 border-t border-border-subtle">
                <div className="flex items-center gap-2">
                  <div className="flex -space-x-2">
                    {[...Array(Math.min(3, evento.participantes))].map((_, i) => (
                      <div
                        key={i}
                        className="w-8 h-8 rounded-full bg-surface-variant border-2 border-surface-container"
                      ></div>
                    ))}
                  </div>
                  <span className="font-label-md text-label-md text-text-secondary">
                    {evento.participantes > 3
                      ? `+${evento.participantes - 3}`
                      : evento.participantes}{' '}
                    participantes
                  </span>
                </div>
                <button className="px-4 py-2 bg-primary-600 hover:bg-primary-700 text-bg-primary font-label-lg text-label-lg rounded-lg transition-colors">
                  Inscribirse
                </button>
              </div>
            </div>
          </Link>
        ))}
      </div>
    </div>
  )
}
