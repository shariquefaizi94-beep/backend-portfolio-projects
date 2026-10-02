package com.nexaforge.shipping.service;

import com.nexaforge.shipping.domain.model.Shipment;
import com.nexaforge.shipping.repository.ShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShippingService {
    private static final Logger log = LoggerFactory.getLogger(ShippingService.class);
    private final ShipmentRepository shipmentRepository;

    public ShippingService(ShipmentRepository shipmentRepository) {
        this.shipmentRepository = shipmentRepository;
    }

    @Transactional
    public Shipment arrangeShipping(String orderId) {
        return shipmentRepository.findByOrderId(orderId)
                .map(existing -> {
                    log.info("Shipment already exists for order {}: {}", orderId, existing.getShipmentId());
                    return existing;
                })
                .orElseGet(() -> {
                    Shipment shipment = Shipment.create(orderId);
                    Shipment saved = shipmentRepository.save(shipment);
                    log.info("Shipment arranged: {} tracking: {}", saved.getShipmentId(), saved.getTrackingNumber());
                    return saved;
                });
    }
}
