package com.infraestrctura.synthevia.entidad;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "via_segmento")
public class ViaSegmento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_via")
    private Integer idVia;

    @Column(name = "codigo_segmento")
    private String codigoSegmento;

    @Column(name = "nombre_via")
    private String nombreVia;

    @Column(name = "fecha_registro")
    private OffsetDateTime fechaRegistro;

    public Integer getIdVia()                { return idVia; }
    public void    setIdVia(Integer v)        { this.idVia = v; }

    public String  getCodigoSegmento()       { return codigoSegmento; }
    public void    setCodigoSegmento(String v){ this.codigoSegmento = v; }

    public String  getNombreVia()            { return nombreVia; }
    public void    setNombreVia(String v)     { this.nombreVia = v; }

    public OffsetDateTime getFechaRegistro()  { return fechaRegistro; }
    public void           setFechaRegistro(OffsetDateTime v) { this.fechaRegistro = v; }
}
