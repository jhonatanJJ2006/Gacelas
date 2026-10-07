export function Routes() {
  return (
    <div className="max-w-4xl mx-auto">
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-8">
        <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-4">Rutas 🗺️</h1>
        <p className="text-gray-600 dark:text-gray-300 mb-6">
          Explora, crea y comparte rutas de running.
        </p>

        <div className="border-2 border-dashed border-gray-300 dark:border-gray-600 rounded-lg p-12 text-center">
          <div className="text-6xl mb-4">🗺️</div>
          <h3 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
            Mapa con Google Maps
          </h3>
          <p className="text-gray-600 dark:text-gray-400 mb-4">
            Aquí se mostrará el mapa interactivo con rutas cercanas.
          </p>
          <p className="text-sm text-gray-500 dark:text-gray-500">
            Funcionalidad pendiente de implementación según roadmap.
          </p>
        </div>

        <div className="mt-6 grid gap-4">
          <div className="bg-gray-50 dark:bg-gray-900 rounded-lg p-4">
            <h4 className="font-semibold text-gray-900 dark:text-white mb-2">
              Features Planificadas:
            </h4>
            <ul className="text-sm text-gray-600 dark:text-gray-400 space-y-1">
              <li>✓ Búsqueda de rutas por ubicación (PostGIS)</li>
              <li>✓ Filtros por distancia, dificultad, tipo</li>
              <li>✓ Crear rutas desde actividades o dibujando</li>
              <li>✓ Guardar rutas favoritas</li>
              <li>✓ Likes y comentarios en rutas</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  )
}
