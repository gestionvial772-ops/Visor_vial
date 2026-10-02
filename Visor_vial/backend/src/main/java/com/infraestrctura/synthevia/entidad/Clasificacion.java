package com.infraestrctura.synthevia.entidad;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "clasificacion")
public class Clasificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_clasificacion")
    private Integer idClasificacion;

    @Column(name = "id_reporte", nullable = false, unique = true)
    private Integer idReporte;

    @Column(name = "severidad", nullable = false)
    private Short severidad;

    @Column(name = "etiqueta_detectada")
    private String etiquetaDetectada;

    @Column(name = "nivel_confianza")
    private BigDecimal nivelConfianza;

    @Column(name = "modelo_ia_utilizado", nullable = false)
    private String modeloIaUtilizado;

    @Column(name = "version_modelo")
    private String versionModelo;

    @Column(name = "fecha_procesamiento", nullable = false)
    private OffsetDateTime fechaProcesamiento;

    @Column(name = "tiene_dano", nullable = false)
    private Boolean tieneDano = Boolean.FALSE;

    @Column(name = "cantidad_danos", nullable = false)
    private Short cantidadDanos = 0;

    @Column(name = "reporte_breve", columnDefinition = "TEXT")
    private String reporteBreve;

    @PrePersist
    protected void prePersist() {

        if (fechaProcesamiento == null) {
            fechaProcesamiento = OffsetDateTime.now();
        }
    }

    public Integer getIdClasificacion() {
        return idClasificacion;
    }

    public void setIdClasificacion(
            Integer idClasificacion) {

        this.idClasificacion = idClasificacion;
    }

    public Integer getIdReporte() {
        return idReporte;
    }

    public void setIdReporte(Integer idReporte) {
        this.idReporte = idReporte;
    }

    public Short getSeveridad() {
        return severidad;
    }

    public void setSeveridad(Short severidad) {
        this.severidad = severidad;
    }

    public String getEtiquetaDetectada() {
        return etiquetaDetectada;
    }

    public void setEtiquetaDetectada(
            String etiquetaDetectada) {

        this.etiquetaDetectada = etiquetaDetectada;
    }

    public BigDecimal getNivelConfianza() {
        return nivelConfianza;
    }

    public void setNivelConfianza(
            BigDecimal nivelConfianza) {

        this.nivelConfianza = nivelConfianza;
    }

    public String getModeloIaUtilizado() {
        return modeloIaUtilizado;
    }

    public void setModeloIaUtilizado(
            String modeloIaUtilizado) {

        this.modeloIaUtilizado = modeloIaUtilizado;
    }

    public String getVersionModelo() {
        return versionModelo;
    }

    public void setVersionModelo(
            String versionModelo) {

        this.versionModelo = versionModelo;
    }

    public OffsetDateTime getFechaProcesamiento() {
        return fechaProcesamiento;
    }

    public void setFechaProcesamiento(
            OffsetDateTime fechaProcesamiento) {

        this.fechaProcesamiento = fechaProcesamiento;
    }

    public Boolean getTieneDano() {
        return tieneDano;
    }

    public void setTieneDano(Boolean tieneDano) {

        this.tieneDano = tieneDano;
    }

    public Short getCantidadDanos() {
        return cantidadDanos;
    }

    public void setCantidadDanos(Short cantidadDanos) {

        this.cantidadDanos = cantidadDanos;
    }

    public String getReporteBreve() {
        return reporteBreve;
    }

    public void setReporteBreve(String reporteBreve) {

        this.reporteBreve = reporteBreve;
    }
}