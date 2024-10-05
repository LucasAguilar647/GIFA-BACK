package com.gifa_api.service.impl;

import com.gifa_api.dto.proveedoresYPedidos.GestorDePedidosDTO;
import com.gifa_api.dto.proveedoresYPedidos.PedidoDTO;
import com.gifa_api.model.*;
import com.gifa_api.repository.IPedidoRepository;
import com.gifa_api.repository.ItemDeInventarioRepository;
import com.gifa_api.service.IGestorDePedidosService;
import com.gifa_api.service.IPedidoService;
import com.gifa_api.service.IProveedorDeItemService;
import com.gifa_api.utils.enums.EstadoPedido;
import com.gifa_api.utils.mappers.PedidosMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PedidoServiceImpl implements IPedidoService {
    private final IPedidoRepository pedidoRepository;
    private final ItemDeInventarioRepository itemDeInventarioRepository;
    private final IGestorDePedidosService gestorDePedidosService;
    private final IProveedorDeItemService proveedorDeItemService;
    private final PedidosMapper pedidosMapper;

    @Override
    public void createPedido(ItemDeInventario itemDeInventario, Integer cantidad) {

        Pedido pedido = Pedido
                .builder()
                .estadoPedido(EstadoPedido.PENDIENTE)
                .item(itemDeInventario)
                .cantidad(cantidad)
                .fecha(LocalDate.now())
                .motivo("Solcitud de stock ")
                .build();
        pedidoRepository.save(pedido);

    }


    //    @Scheduled(fixedRate = 1000, initialDelay = 30000)
    @Scheduled(fixedRate = 86400000)  // Ejecuta cada 24 horas (86400000 milisegundos)
    public void hacerPedidos() {

        List<ItemDeInventario> itemsDeInventario = itemDeInventarioRepository.findAll();
        GestorDePedidos gestorDePedidos = gestorDePedidosService.getGestorDePedidos();


        for (ItemDeInventario item : itemsDeInventario) {
            if (item.getUmbral() > item.getStock()) {
                int cantidadMinima = (item.getUmbral() - item.getStock()) + item.getStock();
                int cantidad = gestorDePedidos.getCantDePedidoAutomatico() + cantidadMinima;
                // seleccionar el proveer mas barato y ver si me alcanza
                ProveedorDeItem proveerDeItemMasEconomico = proveedorDeItemService.proveedorMasEconomico(item.getId());
                if ((proveerDeItemMasEconomico.getPrecio() * cantidad) < gestorDePedidos.getPresupuesto()) {

                    if (!existeElPedidoByItemId(item)) {
                        createPedido(item, cantidad);
                    }

                }
            }
        }

    }

    public boolean existeElPedidoByItemId(ItemDeInventario item) {
        return pedidoRepository.existsByItemId(item.getId());
    }

    @Override
    public List<PedidoDTO> obtenerPedidos() {
       return  pedidosMapper.mapToPedidoDTO(pedidoRepository.findAll());
    }
}



