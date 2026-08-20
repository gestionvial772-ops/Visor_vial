import React from 'react'
import reactDom from 'react-dom'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import './styles.css'

import 'mapbox-gl/dist/mapbox-gl.css'
ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
    <App />
    </BrowserRouter>
    
  </React.StrictMode>
)

