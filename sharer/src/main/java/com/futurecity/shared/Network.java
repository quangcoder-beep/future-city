package com.futurecity.shared;

import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;
import com.futurecity.shared.entities.PlayerState;
import com.futurecity.shared.enums.InteractionAction;
import com.futurecity.shared.enums.InteractionUIType;
import com.futurecity.shared.enums.ItemType;
import com.futurecity.shared.packets.*;
import com.futurecity.shared.packets.request.BuyRequest;
import com.futurecity.shared.packets.request.DeliveryAcceptRequest;
import com.futurecity.shared.packets.request.DeliveryCancelRequest;
import com.futurecity.shared.packets.request.DeliveryCompleteRequest;
import com.futurecity.shared.packets.request.InteractionRequest;
import com.futurecity.shared.packets.request.InventoryRequest;
import com.futurecity.shared.packets.request.LoginRequest;
import com.futurecity.shared.packets.request.MovementRequest;
import com.futurecity.shared.packets.request.DeliveryHandoverRequest;
import com.futurecity.shared.packets.request.DeliveryPickupRequest;
import com.futurecity.shared.packets.request.OrderCancelRequest;
import com.futurecity.shared.packets.request.OrderRequest;
import com.futurecity.shared.packets.request.PathfindingRequest;
import com.futurecity.shared.packets.request.ShopListRequest;
import com.futurecity.shared.packets.request.ShopSubscribeRequest;
import com.futurecity.shared.packets.resonse.BuyResponse;
import com.futurecity.shared.packets.resonse.DeliveryAcceptResponse;
import com.futurecity.shared.packets.resonse.DeliveryCancelResponse;
import com.futurecity.shared.packets.resonse.DeliveryCompleteResponse;
import com.futurecity.shared.packets.resonse.DeliveryGPSUpdate;
import com.futurecity.shared.packets.resonse.InteractionResponse;
import com.futurecity.shared.packets.resonse.InventoryResponse;
import com.futurecity.shared.packets.resonse.KickPacket;
import com.futurecity.shared.packets.resonse.LoginResponse;
import com.futurecity.shared.packets.resonse.MovementResponse;
import com.futurecity.shared.packets.resonse.BatchMovementResponse;
import com.futurecity.shared.packets.resonse.NewOrderNotification;
import com.futurecity.shared.packets.resonse.OrderCancelResponse;
import com.futurecity.shared.packets.resonse.OrderResponse;
import com.futurecity.shared.packets.resonse.PathfindingResponse;
import com.futurecity.shared.packets.resonse.ShopListResponse;
import com.futurecity.shared.packets.resonse.ShopSubscribeResponse;

import java.util.ArrayList;

/**
 * Tổng đài đăng ký tất cả các lớp sẽ được phép truyền qua mạng.
 * BẮT BUỘC: Mọi class gói tin phải được đăng ký TRƯỚC khi gửi/nhận.
 */
public class Network {
    public static final int TCP_PORT = 54555;
    public static final int UDP_PORT = 54777;

    /**
     * Đăng ký các lớp cần thiết vào Kryo của EndPoint (Client hoặc Server).
     * Thứ tự đăng ký PHẢI GIỐNG NHAU ở cả hai phía.
     */
    public static void register(EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();

        // --- CORE LIBS ---
        kryo.register(Vector3.class);
        kryo.register(Quaternion.class);
        kryo.register(String[].class);
        kryo.register(int[].class);
        kryo.register(float[].class);
        kryo.register(ArrayList.class);

        // --- ENUMS ---
        kryo.register(PlayerState.class);
        kryo.register(InteractionAction.class);
        kryo.register(InteractionUIType.class);
        kryo.register(ItemType.class);

        // --- AUTH & MOVEMENT ---
        kryo.register(LoginRequest.class);
        kryo.register(LoginResponse.class);
        kryo.register(KickPacket.class); // Thông báo kick session cũ khi đăng nhập trùng tài khoản

        kryo.register(MovementRequest.class);
        kryo.register(MovementResponse.class);
        kryo.register(BatchMovementResponse.class); // Batch broadcast: N states trong 1 gói
        kryo.register(PlayerState[].class);          // Cần thiết cho mảng trong BatchMovementResponse
        kryo.register(PlayerLeft.class);

        // --- INTERACTION ---
        kryo.register(InteractionRequest.class);
        kryo.register(InteractionResponse.class);

        // --- PATHFINDING(Tìm đường) ---
        kryo.register(PathfindingRequest.class);
        kryo.register(PathfindingResponse.class);

        // --- SHOP (MUA HÀNG) ---
        kryo.register(BuyRequest.class);
        kryo.register(BuyResponse.class);

        // --- ORDER (ĐẶT HÀNG) ---
        kryo.register(OrderRequest.class);
        kryo.register(OrderResponse.class);
        kryo.register(OrderCancelRequest.class);
        kryo.register(OrderCancelResponse.class);

        // --- DELIVERY (GIAO HÀNG) ---
        kryo.register(DeliveryAcceptRequest.class);
        kryo.register(DeliveryAcceptResponse.class);
        kryo.register(DeliveryCancelRequest.class);
        kryo.register(DeliveryCancelResponse.class);
        kryo.register(DeliveryCompleteRequest.class);
        kryo.register(DeliveryCompleteResponse.class);
        kryo.register(DeliveryGPSUpdate.class);

        // XĂ¡c nháº­n giao hĂ ng
        kryo.register(com.futurecity.shared.packets.request.DeliveryConfirmRequest.class);
        kryo.register(com.futurecity.shared.packets.resonse.DeliveryConfirmResponse.class);

        // --- XEM DANH SĂCH SHOP ---
        kryo.register(ShopListRequest.class);
        kryo.register(ShopListResponse.class);
        kryo.register(ShopListResponse.ShopInfo.class);
        kryo.register(ShopListResponse.ShopItemInfo.class);

        // --- SHOP SUBSCRIBE (ÄÄ‚NG KĂ THĂ”NG BĂO) ---
        kryo.register(ShopSubscribeRequest.class);
        kryo.register(ShopSubscribeResponse.class);
        kryo.register(NewOrderNotification.class);

        // --- INVENTORY (TĂI Äá»’) ---
        kryo.register(InventoryRequest.class);
        kryo.register(InventoryResponse.class);
        kryo.register(InventoryResponse.OrderInfo.class);

        // --- DRIVER APP (APP TĂ€I Xáº¾) ---
        kryo.register(com.futurecity.shared.packets.request.DriverPendingOrdersRequest.class);
        kryo.register(com.futurecity.shared.packets.resonse.DriverPendingOrdersResponse.class);
        kryo.register(com.futurecity.shared.packets.resonse.DriverPendingOrdersResponse.OrderInfo.class);
        kryo.register(java.util.ArrayList.class); // Ensure ArrayList is registered for the orders list

        // New in Phase 26
        kryo.register(DeliveryPickupRequest.class);
        kryo.register(DeliveryHandoverRequest.class);
    }
}

