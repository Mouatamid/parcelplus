package com.parcelplus.shipment.domain;

public enum ShipmentStatus {
    CREATED, PICKED_UP, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, EXCEPTION;

    public boolean canTransitionTo(ShipmentStatus next) {
        return switch (this) {
            case CREATED          -> next == PICKED_UP || next == EXCEPTION;
            case PICKED_UP        -> next == IN_TRANSIT || next == EXCEPTION;
            case IN_TRANSIT       -> next == IN_TRANSIT || next == OUT_FOR_DELIVERY || next == EXCEPTION;
            case OUT_FOR_DELIVERY -> next == DELIVERED || next == EXCEPTION;
            case EXCEPTION        -> next == IN_TRANSIT || next == OUT_FOR_DELIVERY;
            case DELIVERED        -> false;
        };
    }
}