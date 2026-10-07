export function Community() {
  return (
    <div className="max-w-4xl mx-auto">
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-8">
        <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-4">
          Comunidad 👥
        </h1>
        <p className="text-gray-600 dark:text-gray-300 mb-6">
          Conecta con otros corredores y comparte tus logros.
        </p>

        <div className="border-2 border-dashed border-gray-300 dark:border-gray-600 rounded-lg p-12 text-center">
          <div className="text-6xl mb-4">📱</div>
          <h3 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
            Feed Social
          </h3>
          <p className="text-gray-600 dark:text-gray-400 mb-4">
            Aquí verás las actividades de los corredores que sigues.
          </p>
          <p className="text-sm text-gray-500 dark:text-gray-500">
            Funcionalidad pendiente de implementación según roadmap (Fase 2).
          </p>
        </div>

        <div className="mt-6 grid md:grid-cols-2 gap-4">
          <div className="bg-gray-50 dark:bg-gray-900 rounded-lg p-4">
            <h4 className="font-semibold text-gray-900 dark:text-white mb-2">
              Features Sociales:
            </h4>
            <ul className="text-sm text-gray-600 dark:text-gray-400 space-y-1">
              <li>✓ Seguir/dejar de seguir usuarios</li>
              <li>✓ Feed de actividades</li>
              <li>✓ Likes y comentarios</li>
              <li>✓ Notificaciones</li>
            </ul>
          </div>

          <div className="bg-gray-50 dark:bg-gray-900 rounded-lg p-4">
            <h4 className="font-semibold text-gray-900 dark:text-white mb-2">
              Clubes:
            </h4>
            <ul className="text-sm text-gray-600 dark:text-gray-400 space-y-1">
              <li>✓ Crear y unirse a clubes</li>
              <li>✓ Feed del club</li>
              <li>✓ Eventos de club</li>
              <li>✓ Roles (owner, admin, member)</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  )
}
