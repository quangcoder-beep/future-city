package com.futurecity.shared.packets.resonse;

import java.util.List;

public class DriverPendingOrdersResponse {
    public boolean success;
    public List<OrderInfo> orders;

    public static class OrderInfo {
        public int orderId;
        public String shopName;
        public String itemName;
        public int reward;
        public String destinationName;
        public float shopX, shopY, shopZ;
        public float destX, destY, destZ;
    }
}

