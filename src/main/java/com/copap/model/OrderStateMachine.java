package com.copap.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> transitions =
            new EnumMap<>(OrderStatus.class);

    static {
        transitions.put(OrderStatus.NEW,
                EnumSet.of(OrderStatus.VALIDATED, OrderStatus.PAYMENT_PENDING, OrderStatus.FAILED));

        transitions.put(OrderStatus.VALIDATED,
                EnumSet.of(OrderStatus.INVENTORY_RESERVED, OrderStatus.FAILED));

        transitions.put(OrderStatus.INVENTORY_RESERVED,
                EnumSet.of(OrderStatus.PAYMENT_PENDING, OrderStatus.FAILED));

        transitions.put(OrderStatus.PAYMENT_PENDING,
                EnumSet.of(OrderStatus.PAID, OrderStatus.FAILED));

        transitions.put(OrderStatus.PAID,
                EnumSet.of(OrderStatus.SHIPPED));

        transitions.put(OrderStatus.SHIPPED,
                EnumSet.noneOf(OrderStatus.class));

        transitions.put(OrderStatus.FAILED,
                EnumSet.noneOf(OrderStatus.class));
    }

    private OrderStateMachine() {}

    public static boolean canTransition(OrderStatus from, OrderStatus to) {
        return transitions.getOrDefault(from, Set.of()).contains(to);
    }
}
