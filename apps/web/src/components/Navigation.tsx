import { NavLink } from 'react-router-dom'

export function Navigation() {
  const navItems = [
    { to: '/', label: 'Inicio' },
    { to: '/routes', label: 'Rutas' },
    { to: '/community', label: 'Comunidad' },
    { to: '/events', label: 'Eventos' },
    { to: '/profile', label: 'Perfil' },
  ]

  return (
    <nav className="bg-white dark:bg-gray-800 shadow-sm border-b border-gray-200 dark:border-gray-700">
      <div className="container mx-auto px-4">
        <div className="flex items-center justify-between h-16">
          <div className="flex items-center space-x-2">
            <span className="text-2xl">🦌</span>
            <span className="text-xl font-bold text-gray-900 dark:text-white">
              Gacelas
            </span>
          </div>
          
          <div className="flex space-x-8">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === '/'}
                className={({ isActive }) =>
                  `text-sm font-medium transition-colors ${
                    isActive
                      ? 'text-primary-600 dark:text-primary-400'
                      : 'text-gray-600 dark:text-gray-300 hover:text-primary-600 dark:hover:text-primary-400'
                  }`
                }
              >
                {item.label}
              </NavLink>
            ))}
          </div>
        </div>
      </div>
    </nav>
  )
}
