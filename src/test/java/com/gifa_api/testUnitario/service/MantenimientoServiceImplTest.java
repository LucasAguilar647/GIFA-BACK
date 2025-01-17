package com.gifa_api.testUnitario.service;

import com.gifa_api.dto.mantenimiento.*;
import com.gifa_api.exception.BadRequestException;
import com.gifa_api.exception.NotFoundException;
import com.gifa_api.model.*;
import com.gifa_api.repository.IMantenimientoRepository;
import com.gifa_api.repository.IUsuarioRepository;
import com.gifa_api.repository.IVehiculoRepository;
import com.gifa_api.service.impl.MantenimientoServiceImpl;
import com.gifa_api.utils.enums.EstadoMantenimiento;
import com.gifa_api.utils.enums.EstadoVehiculo;
import com.gifa_api.utils.enums.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MantenimientoServiceImplTest {

    @InjectMocks
    private MantenimientoServiceImpl mantenimientoService;

    @Mock
    private IMantenimientoRepository mantenimientoRepository;

    @Mock
    private IUsuarioRepository usuarioRepository;

    @Mock
    private IVehiculoRepository vehiculoRepository;

    private RegistrarMantenimientoDTO mantenimiento;
    @BeforeEach
    void setUp(){
         mantenimiento = RegistrarMantenimientoDTO.builder().asunto("asunto").build();
    }
    @Test
    void crearMantenimiento_debeLanzarNotFoundException_siVehiculoNoExiste() {
        when(vehiculoRepository.findById(mantenimiento.getVehiculo_id())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> mantenimientoService.crearMantenimiento(mantenimiento));
        verify(mantenimientoRepository,never()).save(any(Mantenimiento.class));
        verify(vehiculoRepository,times(1)).findById(mantenimiento.getVehiculo_id());
    }

    @Test
    void finalizarMantenimiento_debeLanzarNotFoundException_siMantenimientoNoExiste() {
        when(mantenimientoRepository.findById(anyInt())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> mantenimientoService.finalizarMantenimiento(1));
    }

    @Test
    void crearMantenimiento_asuntoNoPuedeSerVacio(){
        mantenimiento.setAsunto("");
        verificarNoRegistroDeMantenimientoInvalido();
    }

    @Test
    void crearMantenimiento_asuntoNoPuedeSerNulo(){
        mantenimiento.setAsunto(null);
        verificarNoRegistroDeMantenimientoInvalido();
    }

    @Test
    void crearMantenimiento_debeGuardarMantenimiento() {
        Vehiculo vehiculo = new Vehiculo();
        when(vehiculoRepository.findById(mantenimiento.getVehiculo_id())).thenReturn(Optional.of(vehiculo));

        mantenimientoService.crearMantenimiento(mantenimiento);

        verify(mantenimientoRepository, times(1)).save(any(Mantenimiento.class));
        verify(vehiculoRepository,times(1)).findById(mantenimiento.getVehiculo_id());
    }

    @Test
    void asignarMantenimiento_debeAsignarOperadorYActualizarEstado() {
        Mantenimiento mantenimiento = new Mantenimiento();
        Usuario operador = new Usuario();
        operador.setRol(Rol.OPERADOR);

        when(mantenimientoRepository.findById(anyInt())).thenReturn(Optional.of(mantenimiento));

        mantenimientoService.asignarMantenimiento(1, operador);

        assertEquals(operador, mantenimiento.getOperador());
        assertEquals(EstadoMantenimiento.APROBADO, mantenimiento.getEstadoMantenimiento());
    }

    @Test
    void finalizarMantenimiento_debeCambiarEstadoYGuardar() {
        // Arrange
        Mantenimiento mantenimiento = new Mantenimiento();
        Vehiculo vehiculo = new Vehiculo();
        mantenimiento.setVehiculo(vehiculo);

        when(mantenimientoRepository.findById(anyInt())).thenReturn(Optional.of(mantenimiento));

        mantenimientoService.finalizarMantenimiento(1);

        assertEquals(EstadoMantenimiento.FINALIZADO, mantenimiento.getEstadoMantenimiento());
        // si se finaliza el mantenimiento, el vehiculo no deberia estar reparado?
        assertEquals(EstadoVehiculo.EN_REPARACION, vehiculo.getEstadoVehiculo());
        verify(mantenimientoRepository, times(1)).save(mantenimiento);
    }

    public void verificarNoRegistroDeMantenimientoInvalido(){
        assertThrows(BadRequestException.class, () -> mantenimientoService.crearMantenimiento(mantenimiento));
        verify(mantenimientoRepository,never()).save(any(Mantenimiento.class));
    }
}
