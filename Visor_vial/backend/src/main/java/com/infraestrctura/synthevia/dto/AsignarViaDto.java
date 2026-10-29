package com.infraestrctura.synthevia.dto;
import jakarta.validation.constraints.NotNull;


public record AsignarViaDto (
    @NotNull 
    Integer idVia
){}

