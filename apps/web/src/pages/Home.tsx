export function Home() {
  return (
    <div className="max-w-4xl mx-auto">
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow-md p-8">
        <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-4">
          Bienvenido a Gacelas 🦌
        </h1>
        <p className="text-gray-600 dark:text-gray-300 mb-6">
          Tu aplicación de running con seguimiento en tiempo real.
        </p>

        <div className="bg-primary-50 dark:bg-primary-900/20 border border-primary-200 dark:border-primary-800 rounded-lg p-6 mb-6">
          <h2 className="text-lg font-semibold text-primary-900 dark:text-primary-100 mb-2">
            🚧 En Desarrollo
          </h2>
          <p className="text-primary-700 dark:text-primary-300">
            Esta es la arquitectura inicial de Gacelas. Las funcionalidades completas se
            implementarán según el roadmap documentado.
          </p>
        </div>

        <div className="grid md:grid-cols-2 gap-4">
          <div className="border border-gray-200 dark:border-gray-700 rounded-lg p-4">
            <h3 className="font-semibold text-gray-900 dark:text-white mb-2">📍 Rutas</h3>
            <p className="text-sm text-gray-600 dark:text-gray-400">
              Graba, guarda y comparte tus rutas favoritas de running.
            </p>
          </div>

          <div className="border border-gray-200 dark:border-gray-700 rounded-lg p-4">
            <h3 className="font-semibold text-gray-900 dark:text-white mb-2">👥 Comunidad</h3>
            <p className="text-sm text-gray-600 dark:text-gray-400">
              Sigue a otros corredores y comparte tus actividades.
            </p>
          </div>

          <div className="border border-gray-200 dark:border-gray-700 rounded-lg p-4">
            <h3 className="font-semibold text-gray-900 dark:text-white mb-2">📅 Eventos</h3>
            <p className="text-sm text-gray-600 dark:text-gray-400">
              Únete a carreras grupales y competencias locales.
            </p>
          </div>

          <div className="border border-gray-200 dark:border-gray-700 rounded-lg p-4">
            <h3 className="font-semibold text-gray-900 dark:text-white mb-2">📡 Live Tracking</h3>
            <p className="text-sm text-gray-600 dark:text-gray-400">
              Comparte tu ubicación en tiempo real mientras corres.
            </p>
          </div>
        </div>

        <div className="mt-8 p-4 bg-gray-50 dark:bg-gray-900 rounded-lg">
          <h3 className="font-semibold text-gray-900 dark:text-white mb-2">📚 Documentación</h3>
          <p className="text-sm text-gray-600 dark:text-gray-400 mb-2">
            Consulta la documentación completa en la carpeta{' '}
            <code className="bg-gray-200 dark:bg-gray-700 px-2 py-1 rounded">/docs</code>:
          </p>
          <ul className="text-sm text-gray-600 dark:text-gray-400 list-disc list-inside space-y-1">
            <li>DESIGN.md - Sistema de diseño y UX</li>
            <li>SYSTEM.md - Arquitectura del sistema</li>
            <li>DATA_MODEL.md - Modelo de datos</li>
            <li>ROADMAP.md - Hoja de ruta del proyecto</li>
          </ul>
        </div>
      </div>
    </div>
  )
}
