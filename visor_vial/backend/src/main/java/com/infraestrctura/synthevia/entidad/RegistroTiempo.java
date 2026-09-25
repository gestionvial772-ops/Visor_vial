package com.infraestrctura.synthevia.entidad;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "registro_tiempo")
public class RegistroTiempo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_registro")
    private Integer idRegistro;

    @Column(name = "id_reporte", nullable = false)
    private Integer idReporte;

    @Column(name = "etapa", nullable = false)
    private String etapa;

    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime timestamp;

    @Column(name = "origen_evento")
    private String origenEvento;

    @PrePersist
    protected void prePersist() {

        if (timestamp == null) {
            timestamp = OffsetDateTime.now();
        }
    }

    public Integer getIdRegistro() {
        return idRegistro;
    }

    public void setIdRegistro(Integer idRegistro) {
        this.idRegistro = idRegistro;
    }

    public Integer getIdReporte() {
        return idReporte;
    }

    public void setIdReporte(Integer idReporte) {
        this.idReporte = idReporte;
    }

    public String getEtapa() {
        return etapa;
    }

    public void setEtapa(String etapa) {
        this.etapa = etapa;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(
            OffsetDateTime timestamp) {

        this.timestamp = timestamp;
    }

    public String getOrigenEvento() {
        return origenEvento;
    }

    public void setOrigenEvento(
            String origenEvento) {

        this.origenEvento = origenEvento;
    }
}