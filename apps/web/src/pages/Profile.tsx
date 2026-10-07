export function Profile() {
  return (
    <div className="max-w-4xl mx-auto">
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-8">
        <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-4">Perfil 👤</h1>
        <p className="text-gray-600 dark:text-gray-300 mb-6">
          Tus estadísticas, historial y configuración.
        </p>

        <div className="border-2 border-dashed border-gray-300 dark:border-gray-600 rounded-lg p-12 text-center mb-6">
          <div className="text-6xl mb-4">👤</div>
          <h3 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
            Información de Usuario
          </h3>
          <p className="text-gray-600 dark:text-gray-400 mb-4">
            Aquí verás tu avatar, estadísticas y actividades recientes.
          </p>
          <p className="text-sm text-gray-500 dark:text-gray-500">
            Autenticación y perfil pendientes de implementación (Fase 1).
          </p>
        </div>

        <div className="grid md:grid-cols-3 gap-4 mb-6">
          <div className="bg-gradient-to-br from-primary-500 to-primary-600 rounded-lg p-6 text-white">
            <div className="text-3xl font-bold">0 km</div>
            <div className="text-primary-100">Distancia Total</div>
          </div>
          <div className="bg-gradient-to-br from-secondary-500 to-secondary-600 rounded-lg p-6 text-white">
            <div className="text-3xl font-bold">0</div>
            <div className="text-secondary-100">Actividades</div>
          </div>
          <div className="bg-gradient-to-br from-blue-500 to-blue-600 rounded-lg p-6 text-white">
            <div className="text-3xl font-bold">0</div>
            <div className="text-blue-100">Días de Racha</div>
          </div>
        </div>

        <div className="bg-gray-50 dark:bg-gray-900 rounded-lg p-4">
          <h4 className="font-semibold text-gray-900 dark:text-white mb-2">Features de Perfil:</h4>
          <ul className="text-sm text-gray-600 dark:text-gray-400 space-y-1">
            <li>✓ Estadísticas agregadas (km, tiempo, elevación)</li>
            <li>✓ Historial de actividades</li>
            <li>✓ Gráficos de progreso</li>
            <li>✓ Configuración de privacidad</li>
            <li>✓ Ubicación de casa (home zone fuzzing)</li>
          </ul>
        </div>
      </div>
    </div>
  )
}
