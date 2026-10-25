import React from 'react'
import { cerrarTecladoAlTocarFuera } from '../lib/teclado'
import ReactDOM from 'react-dom/client'
import App from './App.tsx'
import '../index.css'
import { activarVistaPreviaSiCorresponde } from '../lib/modoVistaPrevia'

activarVistaPreviaSiCorresponde()

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
)

cerrarTecladoAlTocarFuera()
