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
    private String estado;

    @Column(name = "reporte_original_id")
    private Integer reporteOriginalId;

    /**
     * Anulacion logica. La fila no se borra porque registro_tiempo tiene un
     * trigger que la vuelve inalterable (regla RNF11) y su bitacora debe
     * conservarse para la trazabilidad de las mediciones.
     */
    @Column(name = "eliminado_en")
    private OffsetDateTime eliminadoEn;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @PrePersist
    protected void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = OffsetDateTime.now();
        }
        if (estado == null) {
            estado = "PENDIENTE";
        }
    }

    public Integer getIdReporte() {
        return idReporte;
    }

    public void setIdReporte(Integer v) {
        this.idReporte = v;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer v) {
        this.idUsuario = v;
    }

    public Integer getIdVia() {
        return idVia;
    }

    public void setIdVia(Integer v) {
        this.idVia = v;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String v) {
        this.imagenUrl = v;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String v) {
        this.descripcion = v;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String v) {
        this.estado = v;
    }

    public Integer getReporteOriginalId() {
        return reporteOriginalId;
    }

    public void setReporteOriginalId(Integer v) {
        this.reporteOriginalId = v;
    }

    public OffsetDateTime getEliminadoEn() {
        return eliminadoEn;
    }

    public void setEliminadoEn(OffsetDateTime v) {
        this.eliminadoEn = v;
    }

    public boolean estaAnulado() {
        return eliminadoEn != null;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(OffsetDateTime v) {
        this.fechaCreacion = v;
    }
}
