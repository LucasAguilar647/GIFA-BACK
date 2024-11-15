package com.gifa_api.client;

import com.gifa_api.dto.traccar.*;
import com.gifa_api.model.Dispositivo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;


@Component
@RequiredArgsConstructor
public class TraccarClient implements ITraccarCliente {

    private final RestTemplate restTemplate;

    private  String username = System.getenv("TRACCAR_USERNAME");

    private  String password = System.getenv("TRACCAR_PASSWORD");

    private String baseUrl = System.getenv("TRACCAR_BASE_URL");

    @Override
    public CrearDispositivoResponseDTO postCrearDispositivoTraccar(Dispositivo dispositivo) {
        // Crear la entidad que encapsula los encabezados y el cuerpo
        CrearDispositivoRequestDTO request = CrearDispositivoRequestDTO.builder().name(dispositivo.getNombre()).uniqueId(dispositivo.getUnicoId()).build();
        HttpHeaders headers = getHeaders();
        HttpEntity<CrearDispositivoRequestDTO> entity = new HttpEntity<>(request, headers);

        // Realizar la solicitud POST
        ResponseEntity<CrearDispositivoResponseDTO> response = restTemplate.exchange(
                baseUrl + "/devices",
                HttpMethod.POST,
                entity,
                CrearDispositivoResponseDTO.class
        );


        if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            // Manejar el caso de error si es necesario
            throw new RuntimeException("Error en la creación del dispositivo: " + response.getStatusCode());
        }
    }

    @Override
    public List<PosicionDispositivoDTO> getPosicionDispositivoTraccar(Integer deviceId) {
        HttpHeaders headers = getHeaders();
        HttpEntity<CrearDispositivoRequestDTO> entity = new HttpEntity<>(headers);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + "/positions");
        if (deviceId != null) {
            builder.queryParam("deviceId", deviceId);
        }

        ResponseEntity<PosicionDispositivoDTO[]> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                PosicionDispositivoDTO[].class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            return Arrays.asList(response.getBody());
        } else {
            // Manejar el caso de error si es necesario
            throw new RuntimeException("Error al obtener las posiciones: " + response.getStatusCode());
        }
    }



    @Override
    public List<DispositivoResponseDTO> getDispositivos() {
        HttpHeaders headers = getHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + "/devices");

        ResponseEntity<DispositivoResponseDTO[]> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                DispositivoResponseDTO[].class
        );


        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {

            return Arrays.asList(response.getBody());
        } else {
            throw new RuntimeException("Error al obtener los dispositivos: " + response.getStatusCode());
        }
    }

    @Override
    public DispositivoResponseDTO getDispositivo(Integer deviceId) {
        HttpHeaders headers = getHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + "/devices");

        if (deviceId != null) {
            builder.queryParam("id", deviceId);
        }

        ResponseEntity<DispositivoResponseDTO[]> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                DispositivoResponseDTO[].class
        );

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            DispositivoResponseDTO[] dispositivos = response.getBody();
            return Arrays.stream(dispositivos)
                    .filter(dispositivo -> dispositivo.getId().equals(deviceId)) // Comparar los IDs
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No se encontró el dispositivo con ID: " + deviceId));
        } else {
            throw new RuntimeException("Error al obtener los dispositivos: " + response.getStatusCode());
        }
    }

    @Override
    public KilometrosResponseDTO getKilometros(Integer deviceId, OffsetDateTime from, OffsetDateTime to) {
        // Crear encabezados HTTP necesarios para la solicitud
        HttpHeaders headers = getHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // Construir la URL con parámetros de consulta
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + "/reports/summary")
                .queryParam("from", from) // Agregar parámetro "from"
                .queryParam("to", to); // Agregar parámetro "to"

        if (deviceId != null) {
            builder.queryParam("deviceId", deviceId); // Agregar el ID del dispositivo como parámetro
        } else {
            throw new IllegalArgumentException("Se debe proporcionar un deviceId.");
        }

        // Realizar la solicitud HTTP con RestTemplate
        ResponseEntity<KilometrosResponseDTO> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                KilometrosResponseDTO.class // Esperar un único objeto en la respuesta
        );

        // Verificar el estado de la respuesta y retornar el objeto
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody();
        } else {
            throw new RuntimeException("Error al obtener los kilometros del dispositivo con ID: " + deviceId + ". Código de respuesta: " + response.getStatusCode());
        }
    }



    private String getBasicAuthHeader() {
        String auth = this.username + ":" + this.password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.US_ASCII));
        return "Basic " + new String(encodedAuth);
    }

    private HttpHeaders getHeaders() {
        String basicAuthHeader = getBasicAuthHeader();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", basicAuthHeader);
        headers.set("Content-Type", "application/json");
        return headers;
    }
}
