package com.gifa_api.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegistrarItemDeInventarioDTO {

    private String nombre;
    private Integer umbral;
    private Integer stock;

}

