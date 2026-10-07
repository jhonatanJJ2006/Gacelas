export function Events() {
  return (
    <div className="max-w-4xl mx-auto">
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-8">
        <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-4">
          Eventos 📅
        </h1>
        <p className="text-gray-600 dark:text-gray-300 mb-6">
          Carreras grupales, competencias y entrenamientos organizados.
        </p>

        <div className="border-2 border-dashed border-gray-300 dark:border-gray-600 rounded-lg p-12 text-center">
          <div className="text-6xl mb-4">🏃‍♀️🏃‍♂️</div>
          <h3 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
            Eventos Próximos
          </h3>
          <p className="text-gray-600 dark:text-gray-400 mb-4">
            Descubre eventos de running cerca de ti.
          </p>
          <p className="text-sm text-gray-500 dark:text-gray-500">
            Funcionalidad pendiente de implementación según roadmap (Fase 5).
          </p>
        </div>

        <div className="mt-6 space-y-4">
          <div className="bg-gray-50 dark:bg-gray-900 rounded-lg p-4">
            <h4 className="font-semibold text-gray-900 dark:text-white mb-2">
              Tipos de Eventos:
            </h4>
            <div className="grid md:grid-cols-3 gap-3 text-sm">
              <div className="bg-white dark:bg-gray-800 p-3 rounded border border-gray-200 dark:border-gray-700">
                <div className="font-medium text-gray-900 dark:text-white">Carrera Grupal</div>
                <div className="text-gray-600 dark:text-gray-400">Running social</div>
              </div>
              <div className="bg-white dark:bg-gray-800 p-3 rounded border border-gray-200 dark:border-gray-700">
                <div className="font-medium text-gray-900 dark:text-white">Competencia</div>
                <div className="text-gray-600 dark:text-gray-400">Carreras oficiales</div>
              </div>
              <div className="bg-white dark:bg-gray-800 p-3 rounded border border-gray-200 dark:border-gray-700">
                <div className="font-medium text-gray-900 dark:text-white">Entrenamiento</div>
                <div className="text-gray-600 dark:text-gray-400">Sesiones de práctica</div>
              </div>
            </div>
          </div>

          <div className="bg-primary-50 dark:bg-primary-900/20 border border-primary-200 dark:border-primary-800 rounded-lg p-4">
            <h4 className="font-semibold text-primary-900 dark:text-primary-100 mb-2">
              🎯 Live Tracking en Eventos
            </h4>
            <p className="text-sm text-primary-700 dark:text-primary-300">
              Durante eventos, podrás ver la ubicación en tiempo real de todos los participantes en un mapa compartido.
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
