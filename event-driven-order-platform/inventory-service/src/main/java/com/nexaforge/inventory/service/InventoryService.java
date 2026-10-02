package com.nexaforge.inventory.service;

import com.nexaforge.inventory.domain.model.InventoryReservation;
import com.nexaforge.inventory.repository.InventoryReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages inventory reservations.
 * Idempotent: duplicate reservation attempts return the existing reservation.
 */
@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private final InventoryReservationRepository reservationRepository;

    public InventoryService(InventoryReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public InventoryReservation reserveInventory(String orderId, String itemsJson) {
        return reservationRepository.findByOrderId(orderId)
                .map(existing -> {
                    log.info("Inventory already reserved for order {}: {}", orderId, existing.getReservationId());
                    return existing;
                })
                .orElseGet(() -> {
                    InventoryReservation reservation = InventoryReservation.create(orderId, itemsJson);
                    InventoryReservation saved = reservationRepository.save(reservation);
                    log.info("Inventory reserved: {} for order {}", saved.getReservationId(), orderId);
                    return saved;
                });
    }

    @Transactional
    public void releaseReservation(String orderId) {
        reservationRepository.findByOrderId(orderId).ifPresent(reservation -> {
            if (!"RELEASED".equals(reservation.getStatus())) {
                reservation.release();
                reservationRepository.save(reservation);
                log.info("Inventory reservation released for order {}: {}", orderId, reservation.getReservationId());
            }
        });
    }
}
