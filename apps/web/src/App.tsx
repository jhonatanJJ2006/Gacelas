import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AppLayout } from './components/AppLayout';
import { Bienvenida } from './features/acceso/Bienvenida';
import { InicioSesion } from './features/acceso/InicioSesion';
import { Registro } from './features/acceso/Registro';
import { RecuperarContrasena } from './features/acceso/RecuperarContrasena';
import { PermisosUbicacion } from './features/ajustes/PermisosUbicacion';
import { Inicio } from './features/inicio/Inicio';
import { Correr } from './features/correr/Correr';
import { CarreraEnCurso } from './features/correr/CarreraEnCurso';
import { ResumenPostCarrera } from './features/correr/ResumenPostCarrera';
import { ListaRutas } from './features/rutas/ListaRutas';
import { Feed } from './features/comunidad/Feed';
import { Perfil } from './features/perfil/Perfil';
import { AjustesPreferencias } from './features/ajustes/AjustesPreferencias';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/bienvenida" element={<Bienvenida />} />
        <Route path="/inicio-sesion" element={<InicioSesion />} />
        <Route path="/registro" element={<Registro />} />
        <Route path="/recuperar-contrasena" element={<RecuperarContrasena />} />
        <Route path="/permisos-ubicacion" element={<PermisosUbicacion />} />

        <Route path="/" element={<AppLayout />}>
          <Route index element={<Navigate to="/inicio" replace />} />
          <Route path="inicio" element={<Inicio />} />
          <Route path="rutas" element={<ListaRutas />} />
          <Route path="comunidad" element={<Feed />} />
          <Route path="perfil" element={<Perfil />} />
        </Route>

        <Route path="/correr" element={<Correr />} />
        <Route path="/correr/en-curso" element={<CarreraEnCurso />} />
        <Route path="/correr/resumen" element={<ResumenPostCarrera />} />
        <Route path="/ajustes" element={<AjustesPreferencias />} />

        <Route path="*" element={<Navigate to="/bienvenida" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
