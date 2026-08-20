import React from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import Mapa from './components/map/Mapa'
import Login from './pages/auth/Login'
import Register from './pages/auth/Register'

export default function App() {
  return (
    <Routes>
      <Route path='/login' element ={<Login/>}/>
      <Route path= '/register' element= {<Register/>}/>
      <Route path="/" element={<Navigate to="/login" replace />} />

    </Routes>
  )
}

