import { lazy, Suspense, useEffect, useState } from 'react'
import { BrowserRouter, Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom'
import { useApp } from '../store/app'
import { AppLayout } from './AppLayout'
import { ShellEscritorio } from './ShellEscritorio'

const Bienvenida = lazy(() =>
  import('../contexts/identidad/ui/acceso/Bienvenida').then((m) => ({ default: m.Bienvenida }))
)
const InicioSesion = lazy(() =>
  import('../contexts/identidad/ui/acceso/InicioSesion').then((m) => ({ default: m.InicioSesion }))
)
const Registro = lazy(() =>
  import('../contexts/identidad/ui/acceso/Registro').then((m) => ({ default: m.Registro }))
)
const RecuperarContrasena = lazy(() =>
  import('../contexts/identidad/ui/acceso/RecuperarContrasena').then((m) => ({
    default: m.RecuperarContrasena,
  }))
)
const NuevaContrasena = lazy(() =>
  import('../contexts/identidad/ui/acceso/NuevaContrasena').then((m) => ({
    default: m.NuevaContrasena,
  }))
)
const PermisosUbicacion = lazy(() =>
  import('../contexts/identidad/ui/ajustes/PermisosUbicacion').then((m) => ({
    default: m.PermisosUbicacion,
  }))
)

const Inicio = lazy(() => import('./inicio/Inicio').then((m) => ({ default: m.Inicio })))
const Correr = lazy(() =>
  import('../contexts/actividad/ui/Correr').then((m) => ({ default: m.Correr }))
)
const CarreraEnCurso = lazy(() =>
  import('../contexts/actividad/ui/CarreraEnCurso').then((m) => ({ default: m.CarreraEnCurso }))
)
const Cronometro = lazy(() =>
  import('../contexts/actividad/ui/Cronometro').then((m) => ({ default: m.Cronometro }))
)
const ResumenPostCarrera = lazy(() =>
  import('../contexts/actividad/ui/ResumenPostCarrera').then((m) => ({
    default: m.ResumenPostCarrera,
  }))
)

const ListaRutas = lazy(() =>
  import('../contexts/rutas/ui/ListaRutas').then((m) => ({ default: m.ListaRutas }))
)
const BuscadorRutas = lazy(() =>
  import('../contexts/rutas/ui/BuscadorRutas').then((m) => ({ default: m.BuscadorRutas }))
)
const NavegacionRuta = lazy(() =>
  import('../contexts/rutas/ui/NavegacionRuta').then((m) => ({ default: m.NavegacionRuta }))
)

const Feed = lazy(() => import('../contexts/comunidad/ui/Feed').then((m) => ({ default: m.Feed })))
const Eventos = lazy(() =>
  import('../contexts/eventos/ui/Eventos').then((m) => ({ default: m.Eventos }))
)
const DetalleEvento = lazy(() =>
  import('../contexts/eventos/ui/DetalleEvento').then((m) => ({ default: m.DetalleEvento }))
)

const Corredores = lazy(() =>
  import('../contexts/comunidad/ui/Corredores').then((m) => ({ default: m.Corredores }))
)
const Clubes = lazy(() =>
  import('../contexts/eventos/ui/Clubes').then((m) => ({ default: m.Clubes }))
)
const DetalleClub = lazy(() =>
  import('../contexts/eventos/ui/DetalleClub').then((m) => ({ default: m.DetalleClub }))
)
const EventoEnVivo = lazy(() =>
  import('../contexts/envivo/ui/EventoEnVivo').then((m) => ({ default: m.EventoEnVivo }))
)
const DibujarRuta = lazy(() =>
  import('../contexts/rutas/ui/DibujarRuta').then((m) => ({ default: m.DibujarRuta }))
)
const DetalleRuta = lazy(() =>
  import('../contexts/rutas/ui/DetalleRuta').then((m) => ({ default: m.DetalleRuta }))
)
const ListaEnVivo = lazy(() =>
  import('../contexts/envivo/ui/EnVivo').then((m) => ({ default: m.ListaEnVivo }))
)
const DetalleEnVivo = lazy(() =>
  import('../contexts/envivo/ui/EnVivo').then((m) => ({ default: m.DetalleEnVivo }))
)
const Perfil = lazy(() =>
  import('../contexts/identidad/ui/perfil/Perfil').then((m) => ({ default: m.Perfil }))
)
const DetalleCarrera = lazy(() =>
  import('../contexts/identidad/ui/perfil/DetalleCarrera').then((m) => ({
    default: m.DetalleCarrera,
  }))
)
const EditarPerfil = lazy(() =>
  import('../contexts/identidad/ui/perfil/EditarPerfil').then((m) => ({ default: m.EditarPerfil }))
)
const MisReportes = lazy(() =>
  import('../contexts/analitica/ui/MisReportes').then((m) => ({ default: m.MisReportes }))
)
const Avatar3D = lazy(() =>
  import('../contexts/identidad/ui/perfil/Avatar3D').then((m) => ({ default: m.Avatar3D }))
)

const AjustesPreferencias = lazy(() =>
  import('../contexts/identidad/ui/ajustes/AjustesPreferencias').then((m) => ({
    default: m.AjustesPreferencias,
  }))
)
const Brujula = lazy(() =>
  import('../contexts/identidad/ui/ajustes/Utilidades').then((m) => ({ default: m.Brujula }))
)
const MapasOffline = lazy(() =>
  import('../contexts/identidad/ui/ajustes/Utilidades').then((m) => ({ default: m.MapasOffline }))
)
const PrivacidadUbicacion = lazy(() =>
  import('../contexts/identidad/ui/ajustes/PrivacidadUbicacion').then((m) => ({
    default: m.PrivacidadUbicacion,
  }))
)
const ConfiguracionNotificaciones = lazy(() =>
  import('../contexts/identidad/ui/ajustes/ConfiguracionNotificaciones').then((m) => ({
    default: m.ConfiguracionNotificaciones,
  }))
)
const Notificaciones = lazy(() =>
  import('../contexts/avisos/ui/Notificaciones').then((m) => ({
    default: m.Notificaciones,
  }))
)

const Terminos = lazy(() =>
  import('../contexts/legal/ui/Legal').then((m) => ({ default: m.Terminos }))
)
const PoliticaPrivacidad = lazy(() =>
  import('../contexts/legal/ui/Legal').then((m) => ({ default: m.PoliticaPrivacidad }))
)
const ReaceptarTerminos = lazy(() =>
  import('../contexts/legal/ui/Legal').then((m) => ({ default: m.ReaceptarTerminos }))
)
const AnfitrionLogros = lazy(() =>
  import('../contexts/comunidad/ui/Social').then((m) => ({ default: m.AnfitrionLogros }))
)
const ModalSinConexion = lazy(() =>
  import('../contexts/comunidad/ui/Social').then((m) => ({ default: m.ModalSinConexion }))
)
const Admin = lazy(() => import('../contexts/admin/ui/Admin').then((m) => ({ default: m.Admin })))
const MapaVivo = lazy(() =>
  import('../contexts/envivo/ui/MapaVivo').then((m) => ({ default: m.MapaVivo }))
)
const PublicadorUbicacion = lazy(() =>
  import('../contexts/envivo/ui/MapaVivo').then((m) => ({ default: m.PublicadorUbicacion }))
)

function LoadingFallback() {
  return (
    <div className="min-h-screen bg-surface flex items-center justify-center">
      <div className="flex flex-col items-center gap-2">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
        <span className="text-sm text-text-secondary">Cargando...</span>
      </div>
    </div>
  )
}

/** Rutas protegidas: sin sesion se redirige a /bienvenida. */
export function RutaProtegida() {
  const sesion = useApp((s) => s.sesion)
  const location = useLocation()
  if (!sesion) return <Navigate to="/bienvenida" replace state={{ desde: location.pathname }} />
  return (
    <>
      <Outlet />
      <ReaceptarTerminos />
      <PublicadorUbicacion />
      <AnfitrionLogros />
      <ModalSinConexion />
    </>
  )
}

/** Pantallas de acceso: con sesion abierta se va directo a Inicio. */
export function SoloInvitados() {
  // Se mira la sesion al entrar: tras registrarse, la pantalla navega por su cuenta.
  const [sesionAlEntrar] = useState(() => useApp.getState().sesion)
  if (sesionAlEntrar) return <Navigate to="/inicio" replace />
  return <Outlet />
}

/** Aplica el tema claro u oscuro de los ajustes al documento. */
function TemaDocumento() {
  const modoOscuro = useApp((s) => s.ajustes.modoOscuro)
  useEffect(() => {
    document.documentElement.classList.toggle('tema-claro', !modoOscuro)
    document.documentElement.classList.toggle('dark', modoOscuro)
  }, [modoOscuro])
  return null
}

export function AppRoutes() {
  return (
    <Suspense fallback={<LoadingFallback />}>
      <TemaDocumento />
      <Routes>
        <Route element={<SoloInvitados />}>
          <Route path="/bienvenida" element={<Bienvenida />} />
          <Route path="/inicio-sesion" element={<InicioSesion />} />
          <Route path="/registro" element={<Registro />} />
          <Route path="/recuperar-contrasena" element={<RecuperarContrasena />} />
        </Route>

        <Route path="/nueva-contrasena" element={<NuevaContrasena />} />
        <Route path="/legal/terminos" element={<Terminos />} />
        <Route path="/legal/privacidad" element={<PoliticaPrivacidad />} />

        <Route element={<RutaProtegida />}>
          <Route path="/permisos-ubicacion" element={<PermisosUbicacion />} />
          <Route element={<ShellEscritorio />}>
            <Route path="/" element={<AppLayout />}>
              <Route index element={<Navigate to="/inicio" replace />} />
              <Route path="inicio" element={<Inicio />} />
              <Route path="rutas" element={<ListaRutas />} />
              <Route path="comunidad" element={<Feed />} />
              <Route path="comunidad/eventos" element={<Eventos />} />
              <Route path="perfil" element={<Perfil />} />
            </Route>

            <Route path="/correr" element={<Correr />} />
            <Route path="/correr/en-curso" element={<CarreraEnCurso />} />
            <Route path="/correr/cronometro" element={<Cronometro />} />
            <Route path="/correr/resumen" element={<ResumenPostCarrera />} />

            <Route path="/rutas/buscar" element={<BuscadorRutas />} />
            <Route path="/rutas/navegacion" element={<NavegacionRuta />} />
            <Route path="/rutas/crear" element={<DibujarRuta />} />
            <Route path="/rutas/:id" element={<DetalleRuta />} />

            <Route path="/comunidad/eventos/:id" element={<DetalleEvento />} />
            <Route path="/comunidad/eventos/:id/en-vivo" element={<EventoEnVivo />} />
            <Route path="/comunidad/clubes" element={<Clubes />} />
            <Route path="/comunidad/clubes/:id" element={<DetalleClub />} />
            <Route path="/comunidad/corredores" element={<Corredores />} />
            <Route path="/comunidad/en-vivo" element={<ListaEnVivo />} />
            <Route path="/comunidad/mapa" element={<MapaVivo />} />
            <Route path="/comunidad/en-vivo/:id" element={<DetalleEnVivo />} />

            <Route path="/perfil/editar" element={<EditarPerfil />} />
            <Route path="/perfil/carrera/:id" element={<DetalleCarrera />} />
            <Route path="/perfil/avatar" element={<Avatar3D />} />
            <Route path="/perfil/reportes" element={<MisReportes />} />

            <Route path="/ajustes" element={<AjustesPreferencias />} />
            <Route path="/ajustes/privacidad" element={<PrivacidadUbicacion />} />
            <Route path="/ajustes/notificaciones" element={<ConfiguracionNotificaciones />} />
            <Route path="/ajustes/brujula" element={<Brujula />} />
            <Route path="/ajustes/mapas-offline" element={<MapasOffline />} />
            <Route path="/notificaciones" element={<Notificaciones />} />
          </Route>
        </Route>

        <Route path="/admin/*" element={<Admin />} />
        <Route path="*" element={<Navigate to="/bienvenida" replace />} />
      </Routes>
    </Suspense>
  )
}

function App() {
  const listo = useApp((s) => s.listo)
  const cargar = useApp((s) => s.cargar)
  useEffect(() => {
    void cargar()
  }, [cargar])
  if (!listo) return <LoadingFallback />
  return (
    <BrowserRouter>
      <AppRoutes />
    </BrowserRouter>
  )
}

export default App
