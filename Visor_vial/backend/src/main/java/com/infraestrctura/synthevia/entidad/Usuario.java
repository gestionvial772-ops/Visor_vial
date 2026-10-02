package com.infraestrctura.synthevia.entidad;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;


@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "dni", unique = true, length = 8, nullable = false)
    private String dni;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "apellidos", nullable = false)
    private String apellidos;

    @Column(name = "correo_electronico", unique = true, nullable = false)
    private String correoElectronico;

    @Column(name = "contrasena_hash")
    private String contrasenaHash;

    @Column(name = "rol", nullable = false)
    private String rol;

    @Column(name = "reportes_hoy")
    private Short reportesHoy;

    @Column(name = "fecha_ultimo_reporte")
    private LocalDate fechaUltimoReporte;

    @Column(name = "fecha_creacion")
    private OffsetDateTime fechaCreacion;

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getCorreoElectronico() {
        return correoElectronico;
    }

    public void setCorreoElectronico(String correoElectronico) {
        this.correoElectronico = correoElectronico;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public void setContrasenaHash(String contrasenaHash) {
        this.contrasenaHash = contrasenaHash;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public Short getReportesHoy() {
        return reportesHoy;
    }

    public void setReportesHoy(Short reportesHoy) {
        this.reportesHoy = reportesHoy;
    }

    public LocalDate getFechaUltimoReporte() {
        return fechaUltimoReporte;
    }

    public void setFechaUltimoReporte(LocalDate fechaUltimoReporte) {
        this.fechaUltimoReporte = fechaUltimoReporte;
    }

    public OffsetDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(OffsetDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}