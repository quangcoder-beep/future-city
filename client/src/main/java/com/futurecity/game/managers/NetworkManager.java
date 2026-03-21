package com.futurecity.game.managers;

import com.badlogic.gdx.math.Vector3;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.futurecity.game.entities.MainCharactor;
import com.futurecity.game.entities.RemotePlayer;
import com.futurecity.game.inputs.IInputController;
import com.futurecity.game.ui.GameHUD;
import com.futurecity.shared.Network;
import com.futurecity.shared.entities.PlayerState;
import com.futurecity.shared.enums.InteractionAction;
import com.futurecity.shared.enums.InteractionUIType;
import com.futurecity.shared.packets.*;
import com.futurecity.shared.packets.request.BuyRequest;
import com.futurecity.shared.packets.request.DeliveryAcceptRequest;
import com.futurecity.shared.packets.request.DeliveryCancelRequest;
import com.futurecity.shared.packets.request.DeliveryCompleteRequest;
import com.futurecity.shared.packets.request.DeliveryHandoverRequest;
import com.futurecity.shared.packets.request.DeliveryPickupRequest;
import com.futurecity.shared.packets.request.InteractionRequest;
import com.futurecity.shared.packets.request.InventoryRequest;
import com.futurecity.shared.packets.request.LoginRequest;
import com.futurecity.shared.packets.request.MovementRequest;
import com.futurecity.shared.packets.request.PathfindingRequest;
import com.futurecity.shared.packets.request.ShopSubscribeRequest;
import com.futurecity.shared.packets.request.OrderRequest;
import com.futurecity.shared.packets.request.ShopListRequest;
import com.futurecity.shared.packets.request.DriverPendingOrdersRequest;
import com.futurecity.shared.packets.resonse.BuyResponse;
import com.futurecity.shared.packets.resonse.DeliveryAcceptResponse;
import com.futurecity.shared.packets.resonse.DeliveryCancelResponse;
import com.futurecity.shared.packets.resonse.DeliveryCompleteResponse;
import com.futurecity.shared.packets.resonse.DeliveryGPSUpdate;
import com.futurecity.shared.packets.resonse.InteractionResponse;
import com.futurecity.shared.packets.resonse.PlayerInteractionNotification;
import com.futurecity.shared.packets.resonse.InventoryResponse;
import com.futurecity.shared.packets.resonse.KickPacket;
import com.futurecity.shared.packets.resonse.LoginResponse;
import com.futurecity.shared.packets.resonse.MovementResponse;
import com.futurecity.shared.packets.resonse.BatchMovementResponse;
import com.futurecity.shared.packets.resonse.NewOrderNotification;
import com.futurecity.shared.packets.resonse.PathfindingResponse;
import com.futurecity.shared.packets.resonse.ShopListResponse;
import com.futurecity.shared.packets.resonse.OrderResponse;
import com.futurecity.shared.packets.resonse.ShopSubscribeResponse;
import com.futurecity.shared.packets.resonse.OrderCancelResponse;
import com.futurecity.shared.packets.resonse.DriverPendingOrdersResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkManager {
    private MainCharactor attachedPlayer;
    private final Map<Integer, RemotePlayer> remotePlayers = new ConcurrentHashMap<>();
    private final Map<Integer, Long> remotePlayerLastUpdate = new ConcurrentHashMap<>(); // AoI cleanup
    private int serverSessionId = -1;
    private Client kryoClient;
    private float inputUpdateTimer = 0f;
    private static final float INPUT_UPDATE_RATE = 1f / 20f;

    public interface PlayerSpawnCallback {
        void spawnRemotePlayer(int id, PlayerState initialState);
    }
    private PlayerSpawnCallback spawnCallback;
    private Runnable onKickCallback;

    public void setSpawnCallback(PlayerSpawnCallback callback) { this.spawnCallback = callback; }
    public void setOnKickCallback(Runnable callback) { this.onKickCallback = callback; }
    public Map<Integer, RemotePlayer> getRemotePlayers() { return remotePlayers; }

    public List<com.futurecity.game.entities.Interactable> getRemoteInteractables() {
        List<com.futurecity.game.entities.Interactable> list = new java.util.ArrayList<>();
        for (RemotePlayer rp : remotePlayers.values()) list.add(rp);
        return list;
    }

    private GameHUD gameHUD;
    public void setGameHUD(GameHUD hud) { this.gameHUD = hud; }

    public NetworkManager() {
        this.kryoClient = new Client(1048576, 1048576);
        Network.register(kryoClient);
        kryoClient.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof MovementResponse) handleMovementResponse((MovementResponse) object);
                else if (object instanceof BatchMovementResponse) handleBatchMovementResponse((BatchMovementResponse) object);
                else if (object instanceof LoginResponse) {
                    LoginResponse res = (LoginResponse) object;
                    if (res.status.equals("SUCCESS") && res.newId > 0) serverSessionId = res.newId;
                    if (loginListener != null) loginListener.onLoginResponse(res);
                } else if (object instanceof KickPacket) {
                    // Tài khoản bị kick do đăng nhập trùng — hiển thị dialog rồi disconnect
                    com.badlogic.gdx.Gdx.app.postRunnable(() -> {
                        if (gameHUD != null) {
                            gameHUD.showKickDialog(kryoClient, onKickCallback);
                        }
                    });
                } else if (object instanceof PlayerLeft) {
                    PlayerLeft left = (PlayerLeft) object;
                    RemotePlayer p = remotePlayers.remove(left.id);
                    if (p != null) p.getInstance().transform.setToTranslation(0, -1000, 0);
                } else if (object instanceof PlayerInteractionNotification) handlePlayerInteractionNotification((PlayerInteractionNotification) object);
                else if (object instanceof InteractionResponse) handleInteractionResponse((InteractionResponse) object);
                else if (object instanceof BuyResponse) handleBuyResponse((BuyResponse) object);
                else if (object instanceof DeliveryAcceptResponse) handleDeliveryAcceptResponse((DeliveryAcceptResponse) object);
                else if (object instanceof DeliveryCancelResponse) handleDeliveryCancelResponse((DeliveryCancelResponse) object);
                else if (object instanceof DeliveryCompleteResponse) handleDeliveryCompleteResponse((DeliveryCompleteResponse) object);
                else if (object instanceof NewOrderNotification) handleNewOrderNotification((NewOrderNotification) object);
                else if (object instanceof ShopSubscribeResponse) handleShopSubscribeResponse((ShopSubscribeResponse) object);
                else if (object instanceof DriverPendingOrdersResponse) handleDriverPendingOrdersResponse((DriverPendingOrdersResponse) object);
                else if (object instanceof DeliveryGPSUpdate) handleDeliveryGPSUpdate((DeliveryGPSUpdate) object);
                else if (object instanceof InventoryResponse) handleInventoryResponse((InventoryResponse) object);
                else if (object instanceof com.futurecity.shared.packets.request.DeliveryConfirmRequest) {
                    System.out.println("[CLIENT] Received DeliveryConfirmRequest for order #" + ((com.futurecity.shared.packets.request.DeliveryConfirmRequest)object).orderId);
                    handleDeliveryConfirmRequest((com.futurecity.shared.packets.request.DeliveryConfirmRequest) object);
                }
                else if (object instanceof PathfindingResponse) {
                    PathfindingResponse res = (PathfindingResponse) object;
                    if (res.success && pathfindingListener != null) {
                        com.badlogic.gdx.Gdx.app.postRunnable(() -> pathfindingListener.onPathReceived(res.path));
                    }
                } else if (object instanceof ShopListResponse) {
                    handleShopListResponse((ShopListResponse) object);
                } else if (object instanceof OrderResponse) {
                    handleOrderResponse((OrderResponse) object);
                } else if (object instanceof OrderCancelResponse) {
                    handleOrderCancelResponse((OrderCancelResponse) object);
                } else if (!(object instanceof com.futurecity.shared.entities.PlayerState || 
                           object instanceof com.futurecity.shared.packets.resonse.MovementResponse ||
                           object instanceof com.esotericsoftware.kryonet.FrameworkMessage)) {
                    System.out.println("[CLIENT-DEBUG] Received packet: " + object.getClass().getSimpleName());
                }
            }
        });
    }

    private void handlePlayerInteractionNotification(PlayerInteractionNotification notif) {
        if (gameHUD != null) com.badlogic.gdx.Gdx.app.postRunnable(() -> gameHUD.showNotification(notif.message));
    }

    private void handleInteractionResponse(InteractionResponse res) {
        if (res.success && gameHUD != null && res.uiType != null) {
            com.badlogic.gdx.Gdx.app.postRunnable(() -> {
                if (res.uiType == InteractionUIType.SHOP_CLOTHES || res.uiType == InteractionUIType.SHOP_FOOD || res.uiType == InteractionUIType.SHOP_GENERAL)
                    gameHUD.showShopFromResponse(res);
                else if (res.uiType == InteractionUIType.NPC_DIALOGUE) gameHUD.showDialogueFromResponse(res);
                else if (res.uiType == InteractionUIType.HOUSE_INFO) gameHUD.showHouseFromResponse(res);
                else if (res.uiType == InteractionUIType.PLAYER_INTERACT) gameHUD.showPlayerFromResponse(res);
                else if (res.uiType == InteractionUIType.PICKUP_ACTION) gameHUD.showPickupPanel(res);
                else if (res.uiType == InteractionUIType.HANDOVER_ACTION) gameHUD.showHandoverPanel(res);
            });
        } else if (!res.success && gameHUD != null) {
            com.badlogic.gdx.Gdx.app.postRunnable(() -> gameHUD.showNotification(res.message));
        }
    }

    private void handleBuyResponse(BuyResponse res) {
        com.badlogic.gdx.Gdx.app.postRunnable(() -> {
            if (gameHUD == null) return;
            if (res.success) {
                gameHUD.onBuySuccess(res.remainingCoins, res.itemId);
                gameHUD.showNotification("Purchase Successful!");
            } else {
                gameHUD.onBuyFailed(res.message);
                gameHUD.showNotification(res.message);
            }
        });
    }

    private void handleInventoryResponse(InventoryResponse res) {
        com.badlogic.gdx.Gdx.app.postRunnable(() -> {
            if (gameHUD != null) {
                gameHUD.updateInventory(res);
            }
        });
    }

    private void handleDeliveryAcceptResponse(DeliveryAcceptResponse res) {
        com.badlogic.gdx.Gdx.app.postRunnable(() -> {
            if (gameHUD == null) return;
            if (res.success) {
                gameHUD.showNotification("Accepted! Destination: " + res.destinationName);
                gameHUD.closeAllPanels();
                sendInventoryRequest();
                gameHUD.setNavigationTarget(new Vector3(res.destX, res.destY, res.destZ));
            } else gameHUD.showNotification(res.message);
        });
    }

    private void handleDeliveryCancelResponse(DeliveryCancelResponse res) {
        com.badlogic.gdx.Gdx.app.postRunnable(() -> {
            if (gameHUD == null) return;
            if (res.success) {
                gameHUD.showNotification("Delivery Canceled. Penalty: " + res.penalty);
                gameHUD.onBuySuccess(res.remainingCoins, -1);
                sendInventoryRequest();
            } else gameHUD.showNotification(res.message);
        });
    }

    private void handleDeliveryCompleteResponse(DeliveryCompleteResponse res) {
        com.badlogic.gdx.Gdx.app.postRunnable(() -> {
            if (gameHUD == null) return;
            if (res.success) {
                gameHUD.showNotification(res.message);
                gameHUD.showSuccessFX();
                gameHUD.setNavigationTarget(null);
                gameHUD.onBuySuccess(res.totalCoins, -1);
                sendInventoryRequest();
            } else gameHUD.showNotification(res.message);
        });
    }

    private void handleDeliveryConfirmRequest(com.futurecity.shared.packets.request.DeliveryConfirmRequest req) {
        System.out.println("[CLIENT] Handling DeliveryConfirmRequest from " + req.courierName);
        com.badlogic.gdx.Gdx.app.postRunnable(() -> {
            if (gameHUD != null) {
                gameHUD.showDeliveryConfirmDialog(req.courierName, req.itemName, (Boolean accept) -> {
                    System.out.println("[CLIENT] Buyer response: " + (accept ? "ACCEPT" : "REJECT"));
                    com.futurecity.shared.packets.resonse.DeliveryConfirmResponse res = new com.futurecity.shared.packets.resonse.DeliveryConfirmResponse();
                    res.orderId = req.orderId;
                    res.accept = accept;
                    kryoClient.sendTCP(res);
                });
            }
        });
    }

    private void handleDeliveryGPSUpdate(DeliveryGPSUpdate notif) {
        com.badlogic.gdx.Gdx.app.postRunnable(() -> {
            if (gameHUD != null) {
                gameHUD.setNavigationTarget(new Vector3(notif.destX, notif.destY, notif.destZ));
                gameHUD.setDeliveryTargetName(notif.buyerName);
            }
        });
    }

    private void handleNewOrderNotification(NewOrderNotification notif) {
        com.badlogic.gdx.Gdx.app.postRunnable(() -> {
            if (gameHUD != null) gameHUD.showNotification("New Order at " + notif.shopName + ": " + notif.itemName);
        });
    }

    private void handleShopSubscribeResponse(ShopSubscribeResponse res) {
        com.badlogic.gdx.Gdx.app.postRunnable(() -> {
            if (gameHUD != null) gameHUD.showNotification(res.message);
        });
    }

    private void handleDriverPendingOrdersResponse(DriverPendingOrdersResponse res) {
        com.badlogic.gdx.Gdx.app.postRunnable(() -> {
            if (gameHUD != null && gameHUD.getDashboard() != null) {
                gameHUD.getDashboard().updatePendingOrders(res);
            }
        });
    }

    private void handleShopListResponse(ShopListResponse res) {
        com.badlogic.gdx.Gdx.app.postRunnable(() -> {
            if (gameHUD != null && gameHUD.getDashboard() != null) {
                gameHUD.getDashboard().updateShops(res.shops);
            }
        });
    }

    private void handleOrderResponse(OrderResponse res) {
        com.badlogic.gdx.Gdx.app.postRunnable(() -> {
            if (gameHUD != null) {
                gameHUD.showNotification(res.message);
                if (res.success) {
                    gameHUD.onBuySuccess(res.remainingCoins, -1); // Update UI coins
                }
            }
        });
    }

    private void handleOrderCancelResponse(OrderCancelResponse res) {
        com.badlogic.gdx.Gdx.app.postRunnable(() -> {
            if (gameHUD != null) {
                gameHUD.showNotification(res.message);
                if (res.success) {
                    gameHUD.onBuySuccess(res.remainingCoins, -1); // Update UI coins (Refund or Penalty)
                    sendInventoryRequest();
                }
            }
        });
    }

    public interface PathfindingListener { void onPathReceived(List<Vector3> path); }
    private PathfindingListener pathfindingListener;
    public void setPathfindingListener(PathfindingListener l) { this.pathfindingListener = l; }

    public interface LoginListener { void onLoginResponse(LoginResponse r); }
    private LoginListener loginListener;
    public void setLoginListener(LoginListener l) { this.loginListener = l; }

    public void connect(String host) throws IOException {
        if (!kryoClient.isConnected()) {
            new Thread(kryoClient).start();
            kryoClient.connect(5000, host, Network.TCP_PORT, Network.UDP_PORT);
        }
    }

    public void disconnect() {
        if (kryoClient != null) {
            kryoClient.stop();
            serverSessionId = -1;
            remotePlayers.clear();
            remotePlayerLastUpdate.clear();
        }
    }

    public void loginWithToken(String token) {
        LoginRequest req = new LoginRequest();
        req.token = token;
        kryoClient.sendTCP(req);
    }

    private void handleMovementResponse(MovementResponse r) {
        if (r.state == null) return;
        int pid = r.state.id;
        int mid = (attachedPlayer != null) ? attachedPlayer.getState().id : serverSessionId;
        if (pid == mid) {
            if (attachedPlayer == null) return;
            // Dùng Client-Side Prediction reconciliation thay vì ghi đè trực tiếp
            attachedPlayer.onServerStateReceived(r.state);
        } else if (remotePlayers.containsKey(pid)) remotePlayers.get(pid).setTargetState(r.state);
        else if (spawnCallback != null) spawnCallback.spawnRemotePlayer(pid, r.state);
    }

    /** Xử lý batch: unpack từng state trong mảng như handleMovementResponse */
    private void handleBatchMovementResponse(BatchMovementResponse batch) {
        if (batch.states == null || batch.count == 0) return;
        long now = System.currentTimeMillis();
        for (int i = 0; i < batch.count; i++) {
            com.futurecity.shared.entities.PlayerState s = batch.states[i];
            if (s == null) continue;
            // Cập nhật timestamp để AoI cleanup biết player còn đang được nhận
            remotePlayerLastUpdate.put(s.id, now);
            // Tái sử dụng logic handleMovementResponse
            MovementResponse r = new MovementResponse();
            r.state = s;
            handleMovementResponse(r);
        }
    }

    public void attachPlayer(MainCharactor p) { this.attachedPlayer = p; }
    private MapManager mm;
    public void setMapManager(MapManager m) { this.mm = m; }

    public void update(float d, float cy) {
        if (attachedPlayer != null && serverSessionId != -1 && attachedPlayer.getState().id != serverSessionId) attachedPlayer.getState().id = serverSessionId;
        if (kryoClient != null && kryoClient.isConnected() && attachedPlayer != null) {
            if (gameHUD != null && gameHUD.isAnyPanelOpen()) return;
            inputUpdateTimer += d;
            if (inputUpdateTimer >= INPUT_UPDATE_RATE) {
                inputUpdateTimer -= INPUT_UPDATE_RATE;
                sendPlayerInput(cy);
            }
        }
        // AoI cleanup: xóa remote player không nhận update trong 3 giây (đã ra khỏi AoI)
        long now = System.currentTimeMillis();
        remotePlayerLastUpdate.entrySet().removeIf(e -> {
            if (now - e.getValue() > 3000) {
                RemotePlayer p = remotePlayers.remove(e.getKey());
                if (p != null) p.getInstance().transform.setToTranslation(0, -1000, 0);
                return true;
            }
            return false;
        });
    }

    private void sendPlayerInput(float cy) {
        IInputController i = attachedPlayer.getInput();
        MovementRequest req = new MovementRequest();
        req.horizontal = i.getHorizontal();
        req.vertical = i.getVertical();
        req.isRunning = i.isRunPressed();
        req.cameraYaw = cy;
        if (mm != null) {
            Vector3 p = attachedPlayer.getState().position;
            req.groundHeight = mm.getTerrainHeight(p.x, p.y, p.z);
        } else req.groundHeight = 0f;
        kryoClient.sendUDP(req);
    }

    public void sendInteraction(String tid, InteractionAction a) {
        InteractionRequest req = new InteractionRequest();
        req.id = attachedPlayer.getState().id;
        req.targetId = tid;
        req.action = a;
        kryoClient.sendTCP(req);
        if (tid.startsWith("player_") && gameHUD != null) gameHUD.showHandoverStatus("WAITING...");
    }

    public void sendBuyRequest(String sid, int iid) {
        BuyRequest req = new BuyRequest();
        req.playerId = attachedPlayer.getState().id;
        req.shopId = sid;
        req.itemId = iid;
        kryoClient.sendTCP(req);
    }

    public void sendDeliveryAcceptRequest(int oid) {
        DeliveryAcceptRequest req = new DeliveryAcceptRequest();
        req.playerId = attachedPlayer.getState().id;
        req.orderId = oid;
        kryoClient.sendTCP(req);
    }

    public void sendDeliveryCancelRequest(int oid) {
        DeliveryCancelRequest req = new DeliveryCancelRequest();
        req.playerId = attachedPlayer.getState().id;
        req.orderId = oid;
        kryoClient.sendTCP(req);
    }

    public void sendDeliveryCompleteRequest(int oid) {
        DeliveryCompleteRequest req = new DeliveryCompleteRequest();
        req.playerId = attachedPlayer.getState().id;
        req.orderId = oid;
        kryoClient.sendTCP(req);
    }

    public void sendInventoryRequest() {
        InventoryRequest req = new InventoryRequest();
        req.playerId = attachedPlayer.getState().id;
        kryoClient.sendTCP(req);
    }

    public void sendShopSubscribeRequest(String sid, boolean sub) {
        ShopSubscribeRequest req = new ShopSubscribeRequest();
        req.playerId = attachedPlayer.getState().id;
        req.shopId = sid;
        req.subscribe = sub;
        kryoClient.sendTCP(req);
    }

    public void sendPathfindingRequest(Vector3 target) {
        if (attachedPlayer == null) return;
        PathfindingRequest req = new PathfindingRequest();
        req.currentPos = new Vector3(attachedPlayer.getState().position);
        req.targetPos = new Vector3(target);
        kryoClient.sendTCP(req);
    }

    public void sendPacket(OrderRequest req) { if (req != null) kryoClient.sendTCP(req); }
    public void sendPacket(ShopListRequest req) { if (req != null) kryoClient.sendTCP(req); }
    public void sendPacket(DriverPendingOrdersRequest req) { if (req != null) kryoClient.sendTCP(req); }
    
    public void sendOrderRequest(String shopId, int itemId) {
        if (attachedPlayer == null) return;
        OrderRequest req = new OrderRequest();
        req.playerId = attachedPlayer.getState().id;
        req.shopId = shopId;
        req.itemId = itemId;
        req.destX = attachedPlayer.getPosition().x;
        req.destY = attachedPlayer.getPosition().y;
        req.destZ = attachedPlayer.getPosition().z;
        kryoClient.sendTCP(req);
    }
    
    public void sendOrderCancelRequest(int oid) {
        com.futurecity.shared.packets.request.OrderCancelRequest req = new com.futurecity.shared.packets.request.OrderCancelRequest();
        req.playerId = serverSessionId;
        req.orderId = oid;
        kryoClient.sendTCP(req);
    }
    
    public void sendDeliveryPickupRequest(int oid) {
        DeliveryPickupRequest req = new DeliveryPickupRequest();
        req.orderId = oid;
        kryoClient.sendTCP(req);
    }
    
    public void sendDeliveryHandoverRequest(int oid) {
        DeliveryHandoverRequest req = new DeliveryHandoverRequest();
        req.orderId = oid;
        kryoClient.sendTCP(req);
    }

    public int getServerSessionId() { return serverSessionId; }
}
