package com.infraestrctura.synthevia.entidad;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "coordenada")
public class Coordenada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_coordenada")
    private Integer idCoordenada;

    @Column(name = "id_reporte", nullable = false, unique = true)
    private Integer idReporte;

    @Column(name = "punto_capturado", nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point puntoCapturado;

    @Column(name = "precision_reportada_metros", precision = 10, scale = 3)
    private BigDecimal precisionReportadaMetros;

    @Column(name = "fecha_captura")
    private OffsetDateTime fechaCaptura;

    @PrePersist
    protected void prePersist() {

        if (fechaCaptura == null) {
            fechaCaptura = OffsetDateTime.now();
        }

        if (puntoCapturado != null
                && puntoCapturado.getSRID() == 0) {

            puntoCapturado.setSRID(4326);
        }
    }

    public Integer getIdCoordenada() {
        return idCoordenada;
    }

    public void setIdCoordenada(Integer idCoordenada) {
        this.idCoordenada = idCoordenada;
    }

    public Integer getIdReporte() {
        return idReporte;
    }

    public void setIdReporte(Integer idReporte) {
        this.idReporte = idReporte;
    }

    public Point getPuntoCapturado() {
        return puntoCapturado;
    }

    public void setPuntoCapturado(Point puntoCapturado) {
        this.puntoCapturado = puntoCapturado;
    }

    public BigDecimal getPrecisionReportadaMetros() {
        return precisionReportadaMetros;
    }

    public void setPrecisionReportadaMetros(
            BigDecimal precisionReportadaMetros) {

        this.precisionReportadaMetros = precisionReportadaMetros;
    }

    public OffsetDateTime getFechaCaptura() {
        return fechaCaptura;
    }

    public void setFechaCaptura(
            OffsetDateTime fechaCaptura) {

        this.fechaCaptura = fechaCaptura;
    }

}
