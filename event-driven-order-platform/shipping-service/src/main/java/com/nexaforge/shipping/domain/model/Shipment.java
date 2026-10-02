package com.nexaforge.shipping.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shipments", indexes = {
        @Index(name = "idx_shipment_order_id", columnList = "orderId", unique = true)
})
public class Shipment {

    @Id
    private String shipmentId;

    @Column(nullable = false, unique = true)
    private String orderId;

    @Column(nullable = false)
    private String trackingNumber;

    @Column(nullable = false, length = 32)
    private String carrier;

    @Column(nullable = false, length = 32)
    private String status;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Shipment() {}

    public static Shipment create(String orderId) {
        Shipment s = new Shipment();
        s.shipmentId = "SHP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        s.orderId = orderId;
        s.trackingNumber = "TRK" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        s.carrier = "NexaShip Express";
        s.status = "LABEL_CREATED";
        s.createdAt = Instant.now();
        return s;
    }

    public void cancel() { this.status = "CANCELLED"; }

    public String getShipmentId() { return shipmentId; }
    public String getOrderId() { return orderId; }
    public String getTrackingNumber() { return trackingNumber; }
    public String getCarrier() { return carrier; }
    public String getStatus() { return status; }
}
