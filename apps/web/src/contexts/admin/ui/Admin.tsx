import { useEffect, useMemo, useState, type ReactNode } from 'react'
import { Navigate, Route, Routes, useNavigate, useSearchParams } from 'react-router-dom'
import { useIdentidad } from '../../identidad/api'
import { Ajustes } from './Ajustes'
import { Estadisticas } from './Estadisticas'
import { Moderacion } from './Moderacion'
import { NavegacionAdmin, type Destino } from './NavegacionAdmin'
import { Resumen } from './Resumen'
import { RutasEventos } from './RutasEventos'
import { Usuarios } from './Usuarios'
import { Boton, Icono, useCarga } from './kit'
import { fuenteDemo, fuenteSupabase, type FuenteAdmin } from './fuente'
import './escenario.css'

function Marco({ children }: { children: ReactNode }) {
  return (
    <div className="adm" style={{ display: 'grid', placeItems: 'center', padding: 16 }}>
      {children}
    </div>
  )
}

function LoginAdmin() {
  const iniciar = useIdentidad((s) => s.iniciarSesion)
  const [email, setEmail] = useState('')
  const [pass, setPass] = useState('')
  const [error, setError] = useState<string | null>(null)
  return (
    <Marco>
      <form
        onSubmit={(e) => {
          e.preventDefault()
          void iniciar(email, pass).then((r) => {
            if (!r.ok) setError(r.error)
          })
        }}
        className="adm-card adm-pad"
        style={{ width: '100%', maxWidth: 380, display: 'grid', gap: 14 }}
      >
        <div className="adm-fila">
          <span className="adm-marca-logo" aria-hidden style={{ width: 40, height: 40 }}>
            R
          </span>
          <div>
            <p style={{ fontWeight: 800, fontSize: 20, lineHeight: 1.1 }}>Runcity</p>
            <p className="adm-t3" style={{ fontSize: 12 }}>
              Panel de administración
            </p>
          </div>
        </div>
        <input
          aria-label="Correo"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="Correo"
          className="adm-input"
          style={{ height: 44 }}
        />
        <input
          aria-label="Contraseña"
          type="password"
          value={pass}
          onChange={(e) => setPass(e.target.value)}
          placeholder="Contraseña"
          className="adm-input"
          style={{ height: 44 }}
        />
        {error && <p style={{ fontSize: 13, color: 'hsl(var(--peligro-texto))' }}>{error}</p>}
        <button className="adm-btn adm-b-pri full" style={{ height: 44 }}>
          Entrar al panel
        </button>
        <p className="adm-t3" style={{ fontSize: 12 }}>
          Solo cuentas con rol de administrador.
        </p>
      </form>
    </Marco>
  )
}

/** Panel /admin: solo para quien tenga rol admin en user_roles (verificado en el servidor con RLS). */
export function Admin() {
  const sesion = useIdentidad((s) => s.sesion)
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const demo = import.meta.env.DEV && params.has('demo')
  const f = useMemo(() => (demo ? fuenteDemo() : fuenteSupabase()), [demo])
  const [permitido, setPermitido] = useState<boolean | null>(null)
  useEffect(() => {
    if (!f || (!sesion && !f.demo)) return
    void f.esAdmin().then(setPermitido)
  }, [f, sesion])

  if (!f?.demo && !sesion) return <LoginAdmin />
  if (!f || permitido === false)
    return (
      <Marco>
        <div
          className="adm-card adm-pad"
          style={{ maxWidth: 420, textAlign: 'center' }}
          data-testid="admin-sin-acceso"
        >
          <span style={{ color: 'hsl(var(--peligro-texto))' }}>
            <Icono nombre="lock" tam={44} />
          </span>
          <h1 className="adm-h2" style={{ fontSize: 24, margin: '6px 0' }}>
            Acceso restringido
          </h1>
          <p className="adm-t2">
            {f
              ? 'Tu cuenta no tiene rol de administrador.'
              : 'El panel necesita conexión con el servidor.'}
          </p>
          <div style={{ marginTop: 16, display: 'flex', justifyContent: 'center' }}>
            <Boton onClick={() => navigate('/')}>Volver a la app</Boton>
          </div>
        </div>
      </Marco>
    )
  if (permitido === null) return <div className="adm" />
  return <Panel f={f} nombre={sesion?.nombre || 'Administrador'} sufijo={demo ? '?demo' : ''} />
}

function Panel({ f, nombre, sufijo }: { f: FuenteAdmin; nombre: string; sufijo: string }) {
  const m = useCarga(() => f.metricas(), [f])
  const pendientes = m.datos?.reportes_pendientes
  const destinos: Destino[] = [
    { ruta: '', etiqueta: 'Resumen', icono: 'dashboard', grupo: 'operacion' },
    {
      ruta: 'moderacion',
      etiqueta: 'Moderación',
      icono: 'shield',
      grupo: 'operacion',
      // Solo se muestra con el conteo real de casos abiertos de admin_metricas.
      insignia: pendientes,
      globo: pendientes
        ? `Moderación · ${pendientes} ${pendientes === 1 ? 'caso' : 'casos'}`
        : undefined,
    },
    { ruta: 'usuarios', etiqueta: 'Usuarios', icono: 'group', grupo: 'comunidad' },
    {
      ruta: 'rutas',
      etiqueta: 'Rutas y eventos',
      icono: 'map',
      tambien: ['eventos', 'clubes'],
      grupo: 'comunidad',
    },
    { ruta: 'estadisticas', etiqueta: 'Reportes', icono: 'monitoring', grupo: 'analisis' },
    {
      ruta: 'ajustes',
      etiqueta: 'Ajustes',
      icono: 'tune',
      tambien: ['push', 'versiones'],
      grupo: 'analisis',
    },
  ]
  return (
    <div className="adm adm-app">
      <NavegacionAdmin
        destinos={destinos}
        usuario={{ nombre, rol: 'Administrador' }}
        sufijo={sufijo}
      />
      <main className="adm-main">
        <Routes>
          <Route index element={<Resumen f={f} nombre={nombre.split(' ')[0]} sufijo={sufijo} />} />
          <Route path="usuarios" element={<Usuarios f={f} />} />
          <Route path="estadisticas" element={<Estadisticas f={f} />} />
          <Route path="moderacion" element={<Moderacion f={f} />} />
          <Route path="rutas" element={<RutasEventos f={f} pestana="rutas" sufijo={sufijo} />} />
          <Route
            path="eventos"
            element={<RutasEventos f={f} pestana="eventos" sufijo={sufijo} />}
          />
          <Route path="clubes" element={<RutasEventos f={f} pestana="clubes" sufijo={sufijo} />} />
          <Route path="ajustes" element={<Ajustes f={f} />} />
          <Route path="push" element={<Navigate to={`/admin/ajustes${sufijo}`} replace />} />
          <Route path="versiones" element={<Navigate to={`/admin/ajustes${sufijo}`} replace />} />
          <Route path="*" element={<Navigate to={`/admin${sufijo}`} replace />} />
        </Routes>
      </main>
    </div>
  )
}
