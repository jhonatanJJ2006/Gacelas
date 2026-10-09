import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

export function BuscadorRutas() {
  const navigate = useNavigate()
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedRoute, setSelectedRoute] = useState<'primary' | 'alt' | null>('primary')

  const routes = [
    {
      id: 'primary',
      name: 'Jipiro → Puerta → Zamora Huayco',
      distance: 8.5,
      duration: '45',
      elevation: 120,
      color: 'primary',
    },
    {
      id: 'alt',
      name: 'Ruta Universitaria Directa',
      distance: 6.8,
      duration: '36',
      elevation: 85,
      color: 'secondary',
    },
  ]

  return (
    <div className="w-full min-h-screen bg-[#070b13] flex flex-col relative">
      <div className="absolute top-0 inset-x-0 z-50 flex justify-between items-center px-6 pt-3 pb-2 text-xs font-semibold text-slate-300 pointer-events-none bg-gradient-to-b from-black/80 via-black/40 to-transparent">
        <span>06:45</span>
        <div className="flex items-center gap-1.5">
          <span className="text-[10px]">98%</span>
        </div>
      </div>

      <div className="absolute inset-0 z-0 bg-[#0b1220] overflow-hidden">
        <svg className="w-full h-full object-cover scale-110 opacity-90" viewBox="0 0 600 900">
          <defs>
            <pattern id="urban-grid" width="40" height="40" patternUnits="userSpaceOnUse">
              <path d="M 40 0 L 0 0 0 40" fill="none" stroke="#162238" strokeWidth="0.8" />
            </pattern>
            <linearGradient id="routeGlow" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#10B981" />
              <stop offset="50%" stopColor="#F59E0B" />
              <stop offset="100%" stopColor="#D97706" />
            </linearGradient>
          </defs>

          <rect width="600" height="900" fill="#090f1a" />
          <rect width="600" height="900" fill="url(#urban-grid)" />

          <path
            d="M-50,150 Q180,80 340,190 T650,120 L650,0 L-50,0 Z"
            fill="#0d182b"
            opacity="0.6"
          />

          <path
            d="M 330,0 C 310,220 340,380 290,560 C 260,680 280,800 270,900"
            fill="none"
            stroke="#1e3a5f"
            strokeWidth="12"
            strokeLinecap="round"
            opacity="0.7"
          />

          <path
            d="M 280,110 C 380,100 450,150 430,230 C 380,280 300,270 260,210 Z"
            fill="#064e3b"
            opacity="0.35"
            stroke="#10b981"
            strokeWidth="1"
          />

          {selectedRoute === 'primary' && (
            <path
              d="M 330,210 C 310,260 270,320 280,380 C 290,450 270,520 310,570 C 330,590 350,610 380,620"
              fill="none"
              stroke="#D97706"
              strokeWidth="8"
              strokeLinecap="round"
              opacity="0.95"
            />
          )}

          {selectedRoute === 'alt' && (
            <path
              d="M 250,220 C 240,320 220,450 250,560 C 270,610 320,620 370,630"
              fill="none"
              stroke="#10B981"
              strokeWidth="8"
              strokeLinecap="round"
              opacity="0.95"
            />
          )}

          <circle cx="330" cy="210" r="12" fill="#D97706" opacity="0.9">
            <animate attributeName="r" values="12;16;12" dur="2s" repeatCount="indefinite" />
          </circle>

          <circle cx="380" cy="620" r="10" fill="#10B981" />
        </svg>
      </div>

      <div className="relative z-30 flex-1 flex flex-col">
        <div className="pt-12 px-4">
          <button
            onClick={() => navigate('/rutas')}
            className="w-10 h-10 rounded-full bg-black/60 backdrop-blur-md flex items-center justify-center text-white shadow-lg"
          >
            <span className="material-symbols-outlined">arrow_back</span>
          </button>
        </div>

        <div className="flex-1" />

        <div className="bg-gradient-to-t from-[#070b13] via-[#070b13]/95 to-transparent p-6 pb-8">
          <div className="mb-4">
            <div className="relative">
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="¿A dónde quieres correr?"
                className="w-full h-14 pl-14 pr-4 bg-[#141c2e] border-2 border-[#1f2b45] rounded-2xl text-white placeholder-slate-400 focus:outline-none focus:border-[#D97706] transition-colors"
              />
              <span className="material-symbols-outlined absolute left-5 top-1/2 -translate-y-1/2 text-slate-400 text-[24px]">
                search
              </span>
            </div>
          </div>

          <div className="space-y-3">
            {routes.map((route) => (
              <button
                key={route.id}
                onClick={() => setSelectedRoute(route.id as 'primary' | 'alt')}
                className={`w-full p-4 rounded-2xl border-2 transition-all ${
                  selectedRoute === route.id
                    ? route.color === 'primary'
                      ? 'bg-[#D97706]/10 border-[#D97706]'
                      : 'bg-[#10B981]/10 border-[#10B981]'
                    : 'bg-[#141c2e] border-[#1f2b45] hover:border-[#334155]'
                }`}
              >
                <div className="flex items-center justify-between mb-2">
                  <h3 className="text-white font-semibold text-left">{route.name}</h3>
                  <span
                    className={`material-symbols-outlined ${
                      selectedRoute === route.id
                        ? route.color === 'primary'
                          ? 'text-[#D97706]'
                          : 'text-[#10B981]'
                        : 'text-slate-500'
                    }`}
                  >
                    {selectedRoute === route.id ? 'check_circle' : 'radio_button_unchecked'}
                  </span>
                </div>
                <div className="flex items-center gap-4 text-sm text-slate-400">
                  <span className="flex items-center gap-1">
                    <span className="material-symbols-outlined text-[16px]">straighten</span>
                    {route.distance} km
                  </span>
                  <span className="flex items-center gap-1">
                    <span className="material-symbols-outlined text-[16px]">schedule</span>
                    {route.duration} min
                  </span>
                  <span className="flex items-center gap-1">
                    <span className="material-symbols-outlined text-[16px]">trending_up</span>
                    {route.elevation}m
                  </span>
                </div>
              </button>
            ))}
          </div>

          <button
            onClick={() => navigate('/rutas/navegacion')}
            disabled={!selectedRoute}
            className="w-full mt-6 h-14 bg-gradient-to-r from-[#D97706] to-[#F59E0B] hover:from-[#B45309] hover:to-[#D97706] text-white font-bold rounded-2xl shadow-lg disabled:opacity-50 disabled:cursor-not-allowed transition-all active:scale-95"
          >
            Comenzar navegación
          </button>
        </div>
      </div>
    </div>
  )
}
