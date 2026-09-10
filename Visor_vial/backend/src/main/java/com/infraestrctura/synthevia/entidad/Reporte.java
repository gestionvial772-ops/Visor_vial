package com.infraestrctura.synthevia.entidad;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "reporte")
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reporte")
    private Integer idReporte;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "id_via")
    private Integer idVia;

    @Column(name = "imagen_url", nullable = false)
    private String imagenUrl;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "estado", nullable = false)
    private String estado; // PENDIENTE | EN_PROCESO | RESUELTO | DUPLICADO

    @Column(name = "es_preprueba", nullable = false)
    private Boolean esPreprueba = false;

    @Column(name = "reporte_original_id")
    private Integer reporteOriginalId;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    // ── Getters y Setters ──────────────────────────────────────────────────
    public Integer getIdReporte()                     { return idReporte; }
    public void    setIdReporte(Integer v)             { this.idReporte = v; }

    public Integer getIdUsuario()                     { return idUsuario; }
    public void    setIdUsuario(Integer v)             { this.idUsuario = v; }

    public Integer getIdVia()                         { return idVia; }
    public void    setIdVia(Integer v)                 { this.idVia = v; }

    public String  getImagenUrl()                     { return imagenUrl; }
    public void    setImagenUrl(String v)              { this.imagenUrl = v; }

    public String  getDescripcion()                   { return descripcion; }
    public void    setDescripcion(String v)            { this.descripcion = v; }

    public String  getEstado()                        { return estado; }
    public void    setEstado(String v)                 { this.estado = v; }

    public Boolean getEsPreprueba()                   { return esPreprueba; }
    public void    setEsPreprueba(Boolean v)           { this.esPreprueba = v; }

    public Integer getReporteOriginalId()             { return reporteOriginalId; }
    public void    setReporteOriginalId(Integer v)     { this.reporteOriginalId = v; }

    public OffsetDateTime getFechaCreacion()           { return fechaCreacion; }
    public void           setFechaCreacion(OffsetDateTime v) { this.fechaCreacion = v; }
}
