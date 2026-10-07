import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { Layout } from './components/Layout'
import { Home } from './pages/Home'
import { Routes as RoutesPage } from './pages/Routes'
import { Community } from './pages/Community'
import { Events } from './pages/Events'
import { Profile } from './pages/Profile'
import './index.css'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Home />} />
          <Route path="routes" element={<RoutesPage />} />
          <Route path="community" element={<Community />} />
          <Route path="events" element={<Events />} />
          <Route path="profile" element={<Profile />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

export default App
