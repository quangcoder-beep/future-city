package com.example.server_spring.services;

import com.badlogic.gdx.math.Vector3;
import com.example.server_spring.entity.ServerInteractable;
import com.example.server_spring.entity.ServerPlayer;
import com.example.server_spring.repository.NpcRepository;
import com.example.server_spring.repository.OrderRepository;
import com.example.server_spring.repository.PlayerRepository;
import com.example.server_spring.repository.ShopRepository;
import com.example.server_spring.repository.ShopRepository.ShopItemData;
import com.example.server_spring.repository.SubscriptionRepository;
import com.example.server_spring.repository.OrderRepository.OrderData;
import com.futurecity.shared.enums.InteractionUIType;
import com.futurecity.shared.packets.resonse.InteractionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Handles interaction between players and objects (Shop/NPC).
 */
@Service
public class InteractionService {

    private static final float MAX_DISTANCE = 200.0f;

    @Autowired
    private ShopRepository shopRepo;
    @Autowired
    private OrderRepository orderRepo;
    @Autowired
    private PlayerRepository playerRepo;
    @Autowired
    private SubscriptionRepository subscriptionRepo;
    @Autowired
    private NpcRepository npcRepo;

    public InteractionResponse processInteraction(ServerPlayer player, ServerInteractable target) {
        InteractionResponse resp = new InteractionResponse();
        resp.id = player.getId();
        resp.targetId = target.getId();

        float dist = Vector3.dst(
                player.getState().position.x, player.getState().position.y, player.getState().position.z,
                target.getPosition().x, target.getPosition().y, target.getPosition().z);
        if (dist > MAX_DISTANCE) {
            resp.success = false;
            resp.message = "Too far! " + (int) dist + "m";
            resp.newState = target.getState();
            return resp;
        }

        boolean ok = target.interact(player);
        resp.success = ok;
        resp.message = ok ? "Success!" : "Failed!";
        resp.newState = target.getState();
        resp.uiType = target.getUIType();
        resp.displayName = target.getDisplayName();

        if (ok)
            populateUIData(resp, player, target);
        return resp;
    }

    private void populateUIData(InteractionResponse resp, ServerPlayer player, ServerInteractable target) {
        InteractionUIType type = target.getUIType();
        if (type == InteractionUIType.SHOP_CLOTHES || type == InteractionUIType.SHOP_FOOD
                || type == InteractionUIType.SHOP_GENERAL) {
            populateShopData(resp, player, target.getId());
        } else if (type == InteractionUIType.NPC_DIALOGUE) {
            resp.dialogueLines = npcRepo.getNpcDialogues(target.getId());
        }
    }

    private void populateShopData(InteractionResponse resp, ServerPlayer player, String shopId) {
        try {
            resp.playerCoins = playerRepo.getPlayerCoins(player.getDbUserId());
            resp.isSubscribed = subscriptionRepo.isPlayerSubscribed(player.getDbUserId(), shopId);

            List<ShopItemData> items = shopRepo.getShopItems(shopId);
            resp.itemIds = new int[items.size()];
            resp.itemNames = new String[items.size()];
            resp.itemPrices = new int[items.size()];
            resp.itemStocks = new int[items.size()];
            for (int i = 0; i < items.size(); i++) {
                resp.itemIds[i] = items.get(i).itemId;
                resp.itemNames[i] = items.get(i).itemName;
                resp.itemPrices[i] = items.get(i).price;
                resp.itemStocks[i] = items.get(i).currentStock;
            }

            List<OrderData> orders = orderRepo.getPendingOrders(shopId);
            resp.orderIds = new int[orders.size()];
            resp.orderItemNames = new String[orders.size()];
            resp.orderRewards = new int[orders.size()];
            resp.orderDestinations = new String[orders.size()];
            for (int i = 0; i < orders.size(); i++) {
                resp.orderIds[i] = orders.get(i).orderId;
                resp.orderItemNames[i] = orders.get(i).itemName;
                resp.orderRewards[i] = orders.get(i).reward;
                resp.orderDestinations[i] = String.format("(%.0f, %.0f)", orders.get(i).destX, orders.get(i).destZ);
            }
        } catch (Exception e) {
            System.err.println("[INTERACT] Error populating shop: " + e.getMessage());
        }
    }
}
