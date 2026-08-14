import React, { useEffect, useRef, useState } from 'react'
import mapboxgl from 'mapbox-gl'
import reportes from '../../mocks/reportes.json'

export default function Mapa() {
  const mapContainer = useRef(null)
  const mapRef = useRef(null)
  const [status, setStatus] = useState('iniciando')

  useEffect(() => {
    const token = import.meta.env.VITE_MAPBOX_TOKEN || 'pk.eyJ1IjoiaGFsZm9uc28xMjM0NSIsImEiOiJjbW8wYnJ1dncwNm5lMndweWttbTNhYmg2In0.LC3mGJP8ib3exVL-VGZyyg'
    mapboxgl.accessToken = token

    const hasWebGL = (() => {
      try {
        const canvas = document.createElement('canvas')
        return !!(window.WebGLRenderingContext && (canvas.getContext('webgl') || canvas.getContext('experimental-webgl')))
      } catch (e) {
        return false
      }
    })()

    console.log('Mapa: WebGL disponible?', hasWebGL)
    console.log('Mapa: token usado=', token)

    if (!hasWebGL) {
      setStatus('error-webgl')
      return
    }

    try {
      mapRef.current = new mapboxgl.Map({
        container: mapContainer.current,
        style: 'mapbox://styles/mapbox/streets-v12',
        center: [-78.87, -8.11],
        zoom: 12,
        attributionControl: true
      })

      mapRef.current.on('load', () => {
        console.log('Mapa: evento load')
        setStatus('cargado')
      })

      mapRef.current.on('error', (e) => {
        console.error('Mapa error event:', e)
        setStatus('error-load')
      })

      // add markers from mocks
      reportes.forEach((r) => {
        if (!r.coordenadas) return
        const el = document.createElement('div')
        el.className = 'marker'
        el.style.width = '18px'
        el.style.height = '18px'
        el.style.background = 'red'
        el.style.borderRadius = '50%'

        new mapboxgl.Marker(el)
          .setLngLat([r.coordenadas.lng, r.coordenadas.lat])
          .setPopup(new mapboxgl.Popup({ offset: 12 }).setText(r.descripcion || 'Reporte'))
          .addTo(mapRef.current)
      })

      // click to show coords
      mapRef.current.on('click', (e) => {
        const coords = e.lngLat
        new mapboxgl.Popup()
          .setLngLat(coords)
          .setHTML(`<strong>Coordenadas</strong><div>${coords.lng.toFixed(6)}, ${coords.lat.toFixed(6)}</div>`)
          .addTo(mapRef.current)
      })
    } catch (err) {
      console.error('Error inicializando mapa:', err)
      setStatus('error-excepcion')
    }

    return () => mapRef.current && mapRef.current.remove()
  }, [])

  return (
    <div style={{ width: '100%', height: '100%', position: 'relative' }}>
      <div ref={mapContainer} style={{ width: '100%', height: '100%', background: '#eee' }} />
      <div style={{ position: 'absolute', right: 8, top: 8, padding: 8, background: 'rgba(255,255,255,0.9)', borderRadius: 6 }}>
        <div style={{ fontSize: 12 }}>Estado: <strong>{status}</strong></div>
        <div style={{ fontSize: 11, marginTop: 4 }}>Token: <span style={{ fontFamily: 'monospace' }}>{(import.meta.env.VITE_MAPBOX_TOKEN || '').slice(0, 8) || 'default'}</span></div>
      </div>
    </div>
  )
}

