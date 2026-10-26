import React from 'react'
import ReactDOM from 'react-dom/client'
import '../index.css'
import { cerrarTecladoAlTocarFuera } from '../shared/lib/teclado'
import { IntroAnimada } from '../shared/ui/IntroAnimada'
import App from './App.tsx'
import { activarVistaPreviaSiCorresponde } from './modoVistaPrevia'

activarVistaPreviaSiCorresponde()

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
    <IntroAnimada />
  </React.StrictMode>
)

cerrarTecladoAlTocarFuera()
