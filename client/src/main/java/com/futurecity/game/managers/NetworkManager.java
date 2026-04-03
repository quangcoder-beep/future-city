package com.futurecity.game.managers;

import com.badlogic.gdx.math.Vector3;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.futurecity.game.core.Main;
import com.futurecity.game.entities.MainCharactor;
import com.futurecity.game.entities.RemotePlayer;
import com.futurecity.game.inputs.IInputController;
import com.futurecity.game.ui.GameHUD;
import com.futurecity.game.ui.NameplateManager;
import com.futurecity.shared.Network;
import com.futurecity.shared.entities.PlayerState;
import com.futurecity.shared.enums.InteractionAction;
import com.futurecity.shared.enums.InteractionUIType;
import com.futurecity.shared.packets.*;
import com.futurecity.shared.packets.request.*;
import com.futurecity.shared.packets.resonse.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NetworkManager
 * Manages communication between Client and Server using KryoNet.
 */
public class NetworkManager {
    private final Main game;
    private MainCharactor attachedPlayer;
    private final Map<Integer, RemotePlayer> remotePlayers = new ConcurrentHashMap<>();
    private final Map<Integer, Long> remotePlayerLastUpdate = new ConcurrentHashMap<>();
    private final java.util.Set<Integer> pendingSpawns = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private int serverSessionId = -1;
    private Client kryoClient;
    private float inputUpdateTimer = 0f;
    private static final float INPUT_UPDATE_RATE = 1f / 20f;

    public interface PlayerSpawnCallback {
        void spawnRemotePlayer(int id, PlayerState initialState);
    }

    public interface PlayerLeftCallback {
        void onPlayerLeft(int id, RemotePlayer player);
    }

    private PlayerSpawnCallback spawnCallback;
    private PlayerLeftCallback playerLeftCallback;
    private Runnable onKickCallback;

    public void setPlayerLeftCallback(PlayerLeftCallback callback) {
        this.playerLeftCallback = callback;
    }
    private java.util.function.Consumer<Float> worldTimeListener;
    private NameplateManager nameplateManager;

    public void setWorldTimeListener(java.util.function.Consumer<Float> listener) {
        this.worldTimeListener = listener;
    }

    public void setSpawnCallback(PlayerSpawnCallback callback) {
        this.spawnCallback = callback;
    }

    public void setOnKickCallback(Runnable callback) {
        this.onKickCallback = callback;
    }

    public Map<Integer, RemotePlayer> getRemotePlayers() {
        return remotePlayers;
    }

    public List<com.futurecity.game.entities.Interactable> getRemoteInteractables() {
        List<com.futurecity.game.entities.Interactable> list = new java.util.ArrayList<>();
        for (RemotePlayer rp : remotePlayers.values())
            list.add(rp);
        return list;
    }

    private GameHUD gameHUD;

    public void setGameHUD(GameHUD hud) {
        this.gameHUD = hud;
    }

    public NetworkManager(Main game) {
        this.game = game;
        this.kryoClient = new Client(1048576, 1048576);
        Network.register(kryoClient);
        kryoClient.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                try {
                    if (object instanceof MovementResponse)
                        handleMovementResponse((MovementResponse) object);
                    else if (object instanceof BatchMovementResponse)
                        handleBatchMovementResponse((BatchMovementResponse) object);
                    else if (object instanceof LoginResponse) {
                        LoginResponse res = (LoginResponse) object;
                        if (res.status.equals("SUCCESS") && res.newId > 0) {
                            serverSessionId = res.newId;
                            
                            // Cleanup self-ghost if it was spawned before LoginResponse reached the client
                            RemotePlayer ghost = remotePlayers.remove(res.newId);
                            if (ghost != null) {
                                com.badlogic.gdx.Gdx.app.postRunnable(() -> {
                                    ghost.getInstance().transform.setToTranslation(0, -1000, 0);
                                });
                            }

                            // Lưu định danh vào main để HUD dùng sau này
                            if (res.nickname != null)
                                game.nickname = res.nickname;
                            if (res.avatarUrl != null)
                                game.avatarUrl = res.avatarUrl;
                            game.credits = res.credits;
                            game.deliveries = res.deliveries;
                            game.reputation = res.reputation;
                        }
                        if (loginListener != null)
                            loginListener.onLoginResponse(res);
                    } else if (object instanceof KickPacket) {
                        com.badlogic.gdx.Gdx.app.postRunnable(() -> {
                            if (gameHUD != null)
                                gameHUD.showKickDialog(kryoClient, onKickCallback);
                        });
                    } else if (object instanceof PlayerLeft) {
                        PlayerLeft left = (PlayerLeft) object;
                        RemotePlayer p = remotePlayers.remove(left.id);
                        if (p != null) {
                            if (playerLeftCallback != null) {
                                playerLeftCallback.onPlayerLeft(left.id, p);
                            }
                        }
                    } else if (object instanceof PlayerInteractionNotification)
                        handlePlayerInteractionNotification((PlayerInteractionNotification) object);
                    else if (object instanceof InteractionResponse)
                        handleInteractionResponse((InteractionResponse) object);
                    else if (object instanceof BuyResponse)
                        handleBuyResponse((BuyResponse) object);
                    else if (object instanceof DeliveryAcceptResponse)
                        handleDeliveryAcceptResponse((DeliveryAcceptResponse) object);
                    else if (object instanceof DeliveryCancelResponse)
                        handleDeliveryCancelResponse((DeliveryCancelResponse) object);
                    else if (object instanceof DeliveryCompleteResponse)
                        handleDeliveryCompleteResponse((DeliveryCompleteResponse) object);
                    else if (object instanceof NewOrderNotification)
                        handleNewOrderNotification((NewOrderNotification) object);
                    else if (object instanceof ShopSubscribeResponse)
                        handleShopSubscribeResponse((ShopSubscribeResponse) object);
                    else if (object instanceof DriverPendingOrdersResponse)
                        handleDriverPendingOrdersResponse((DriverPendingOrdersResponse) object);
                    else if (object instanceof DeliveryGPSUpdate)
                        handleDeliveryGPSUpdate((DeliveryGPSUpdate) object);
                    else if (object instanceof WorldTimeResponse) {
                        float time = ((WorldTimeResponse) object).time;
                        if (worldTimeListener != null)
                            com.badlogic.gdx.Gdx.app.postRunnable(() -> worldTimeListener.accept(time));
                    } else if (object instanceof InventoryResponse)
                        handleInventoryResponse((InventoryResponse) object);
                    else if (object instanceof com.futurecity.shared.packets.request.DeliveryConfirmRequest)
                        handleDeliveryConfirmRequest((com.futurecity.shared.packets.request.DeliveryConfirmRequest) object);
                    else if (object instanceof PathfindingResponse) {
                        PathfindingResponse res = (PathfindingResponse) object;
                        if (res.success && pathfindingListener != null)
                            com.badlogic.gdx.Gdx.app.postRunnable(() -> pathfindingListener.onPathReceived(res.path));
                    } else if (object instanceof ShopListResponse)
                        handleShopListResponse((ShopListResponse) object);
                    else if (object instanceof OrderResponse)
                        handleOrderResponse((OrderResponse) object);
                    else if (object instanceof OrderCancelResponse)
                        handleOrderCancelResponse((OrderCancelResponse) object);
                    else if (object instanceof NicknameUpdate) {
                        NicknameUpdate nu = (NicknameUpdate) object;
                        if (nu.id == serverSessionId && attachedPlayer != null) {
                            attachedPlayer.getState().username = nu.newNickname;
                            game.nickname = nu.newNickname;
                        } else if (remotePlayers.containsKey(nu.id)) {
                            remotePlayers.get(nu.id).setUsername(nu.newNickname);
                        }
                        if (nameplateManager != null) {
                            nameplateManager.updateNickname(nu.id, nu.newNickname);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[CLIENT NETWORK ERROR] Exception in received(): " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    private void handlePlayerInteractionNotification(PlayerInteractionNotification notif) {
        if (gameHUD != null)
            com.badlogic.gdx.Gdx.app.postRunnable(() -> gameHUD.showNotification(notif.message));
    }

    private void handleInteractionResponse(InteractionResponse res) {
        if (res.success && gameHUD != null && res.uiType != null) {
            com.badlogic.gdx.Gdx.app.postRunnable(() -> {
                if (res.uiType == InteractionUIType.SHOP_CLOTHES || res.uiType == InteractionUIType.SHOP_FOOD
                        || res.uiType == InteractionUIType.SHOP_GENERAL)
                    gameHUD.showShopFromResponse(res);
                else if (res.uiType == InteractionUIType.NPC_DIALOGUE)
                    gameHUD.showDialogueFromResponse(res);
                else if (res.uiType == InteractionUIType.HOUSE_INFO)
                    gameHUD.showHouseFromResponse(res);
                else if (res.uiType == InteractionUIType.PLAYER_INTERACT)
                    gameHUD.showPlayerFromResponse(res);
                else if (res.uiType == InteractionUIType.PICKUP_ACTION)
                    gameHUD.showPickupPanel(res);
                else if (res.uiType == InteractionUIType.HANDOVER_ACTION)
                    gameHUD.showHandoverPanel(res);
            });
        } else if (!res.success && gameHUD != null) {
            com.badlogic.gdx.Gdx.app.postRunnable(() -> gameHUD.showNotification(res.message));
        }
    }

    private void handleBuyResponse(BuyResponse res) {
        com.badlogic.gdx.Gdx.app.postRunnable(() -> {
            if (gameHUD == null)
                return;
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
            if (gameHUD != null)
                gameHUD.updateInventory(res);
        });
    }

    private void handleDeliveryAcceptResponse(DeliveryAcceptResponse res) {
        com.badlogic.gdx.Gdx.app.postRunnable(() -> {
            if (gameHUD == null)
                return;
            if (res.success) {
                gameHUD.showNotification("Accepted! Destination: " + res.destinationName);
                gameHUD.removeNotification("order_" + res.orderId);
                gameHUD.closeAllPanels();
                sendInventoryRequest();
                gameHUD.setNavigationTarget(new Vector3(res.destX, res.destY, res.destZ));
            } else
                gameHUD.showNotification(res.message);
        });
    }

    private void handleDeliveryCancelResponse(DeliveryCancelResponse res) {
        com.badlogic.gdx.Gdx.app.postRunnable(() -> {
            if (gameHUD == null)
                return;
            if (res.success) {
                gameHUD.showNotification("Delivery Canceled. Penalty: " + res.penalty);
                gameHUD.onBuySuccess(res.remainingCoins, -1);
                sendInventoryRequest();
            } else
                gameHUD.showNotification(res.message);
        });
    }

    private void handleDeliveryCompleteResponse(DeliveryCompleteResponse res) {
        com.badlogic.gdx.Gdx.app.postRunnable(() -> {
            if (gameHUD == null)
                return;
            if (res.success) {
                gameHUD.showNotification(res.message);
                gameHUD.showSuccessFX();
                gameHUD.setNavigationTarget(null);
                gameHUD.onBuySuccess(res.totalCoins, -1);
                sendInventoryRequest();
            } else
                gameHUD.showNotification(res.message);
        });
    }

    private void handleDeliveryConfirmRequest(com.futurecity.shared.packets.request.DeliveryConfirmRequest req) {
        com.badlogic.gdx.Gdx.app.postRunnable(() -> {
            if (gameHUD != null) {
                gameHUD.showDeliveryConfirmDialog(req.courierName, req.itemName, (Boolean accept) -> {
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
            if (gameHUD != null)
                gameHUD.showNotification("New Order at " + notif.shopName + ": " + notif.itemName,
                        "order_" + notif.orderId);
        });
    }

    private void handleShopSubscribeResponse(ShopSubscribeResponse res) {
        com.badlogic.gdx.Gdx.app.postRunnable(() -> {
            if (gameHUD != null)
                gameHUD.showNotification(res.message);
        });
    }

    private void handleDriverPendingOrdersResponse(DriverPendingOrdersResponse res) {
        com.badlogic.gdx.Gdx.app.postRunnable(() -> {
            if (gameHUD != null && gameHUD.getDashboard() != null)
                gameHUD.getDashboard().updatePendingOrders(res);
        });
    }

    private void handleShopListResponse(ShopListResponse res) {
        com.badlogic.gdx.Gdx.app.postRunnable(() -> {
            if (gameHUD != null && gameHUD.getDashboard() != null)
                gameHUD.getDashboard().updateShops(res.shops);
        });
    }

    private void handleOrderResponse(OrderResponse res) {
        com.badlogic.gdx.Gdx.app.postRunnable(() -> {
            if (gameHUD != null) {
                gameHUD.showNotification(res.message);
                if (res.success)
                    gameHUD.onBuySuccess(res.remainingCoins, -1);
            }
        });
    }

    private void handleOrderCancelResponse(OrderCancelResponse res) {
        com.badlogic.gdx.Gdx.app.postRunnable(() -> {
            if (gameHUD != null) {
                gameHUD.showNotification(res.message);
                if (res.success) {
                    gameHUD.onBuySuccess(res.remainingCoins, -1);
                    sendInventoryRequest();
                }
            }
        });
    }

    public interface PathfindingListener {
        void onPathReceived(List<Vector3> path);
    }

    private PathfindingListener pathfindingListener;

    public void setPathfindingListener(PathfindingListener l) {
        this.pathfindingListener = l;
    }

    public interface LoginListener {
        void onLoginResponse(LoginResponse r);
    }

    private LoginListener loginListener;

    public void setLoginListener(LoginListener l) {
        this.loginListener = l;
    }

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
        if (r.state == null)
            return;
        int pid = r.state.id;
        int mid = (attachedPlayer != null) ? attachedPlayer.getState().id : serverSessionId;
        
        // Guard: Never spawn a remote player for our own session ID
        if (pid == mid || (serverSessionId != -1 && pid == serverSessionId)) {
            if (attachedPlayer != null)
                attachedPlayer.onServerStateReceived(r.state);
            // Sync local stats for identity panel
            game.credits = r.state.credits;
            game.deliveries = r.state.deliveries;
            game.reputation = r.state.reputation;
        } else if (remotePlayers.containsKey(pid)) {
            remotePlayers.get(pid).setTargetState(r.state);
        } else if (spawnCallback != null && pendingSpawns.add(pid)) {
            // Identity Deduplication (Session Stalling Fix)
            int incomingDbId = r.state.dbUserId;
            if (incomingDbId > 0) {
                int staleIdToRemove = -1;
                RemotePlayer stalePlayer = null;
                for (Map.Entry<Integer, RemotePlayer> entry : remotePlayers.entrySet()) {
                    if (entry.getValue().getDbUserId() == incomingDbId) {
                        staleIdToRemove = entry.getKey();
                        stalePlayer = entry.getValue();
                        break;
                    }
                }
                
                if (staleIdToRemove != -1 && stalePlayer != null) {
                    System.out.println("[ID PRUNING] Found stale session " + staleIdToRemove + " for DB ID " + incomingDbId + ". Replacing with new session " + pid);
                    remotePlayers.remove(staleIdToRemove);
                    if (playerLeftCallback != null) {
                        playerLeftCallback.onPlayerLeft(staleIdToRemove, stalePlayer);
                    }
                }
            }

            // Only trigger spawn if it's not already in the pending queue
            spawnCallback.spawnRemotePlayer(pid, r.state);
        }
    }

    private void handleBatchMovementResponse(BatchMovementResponse batch) {
        if (batch.states == null || batch.count == 0)
            return;
        long now = System.currentTimeMillis();
        for (int i = 0; i < batch.count; i++) {
            com.futurecity.shared.entities.PlayerState s = batch.states[i];
            if (s == null)
                continue;
            remotePlayerLastUpdate.put(s.id, now);
            MovementResponse r = new MovementResponse();
            r.state = s;
            handleMovementResponse(r);
        }
    }

    public void attachPlayer(MainCharactor p) {
        this.attachedPlayer = p;
    }

    private MapManager mm;

    public void setMapManager(MapManager m) {
        this.mm = m;
    }

    public void setNameplateManager(NameplateManager nameplateManager) {
        this.nameplateManager = nameplateManager;
    }

    public void update(float d, float cy) {
        if (attachedPlayer != null && serverSessionId != -1 && attachedPlayer.getState().id != serverSessionId) {
            int oldId = attachedPlayer.getState().id;
            attachedPlayer.getState().id = serverSessionId;
            if (nameplateManager != null) {
                // Remove old label if it exists (for ID -1) and recreate/update for new ID
                nameplateManager.updateNickname(serverSessionId, attachedPlayer.getState().username);
            }
        }
        if (kryoClient != null && kryoClient.isConnected() && attachedPlayer != null) {
            if (gameHUD != null && gameHUD.isAnyPanelOpen())
                return;
            // eSports CSP: Send input every frame
            sendPlayerInput();
        }
        long now = System.currentTimeMillis();
        remotePlayerLastUpdate.entrySet().removeIf(e -> {
            if (now - e.getValue() > 3000) {
                RemotePlayer p = remotePlayers.remove(e.getKey());
                if (p != null)
                    p.getInstance().transform.setToTranslation(0, -1000, 0);
                return true;
            }
            return false;
        });
    }

    private void sendPlayerInput() {
        MainCharactor.InputSnapshot snap = attachedPlayer.getLatestSnapshot();
        if (snap == null) return;
        
        MovementRequest req = new MovementRequest();
        req.horizontal = snap.horizontal;
        req.vertical = snap.vertical;
        req.isRunning = snap.isRunning;
        req.cameraYaw = snap.cameraYaw;
        req.sequence = snap.sequence;
        req.deltaTime = snap.delta;
        
        kryoClient.sendUDP(req);
    }

    public void sendInteraction(String tid, InteractionAction a) {
        InteractionRequest req = new InteractionRequest();
        req.id = attachedPlayer.getState().id;
        req.targetId = tid;
        req.action = a;
        kryoClient.sendTCP(req);
        if (tid.startsWith("player_") && gameHUD != null)
            gameHUD.showHandoverStatus("WAITING...");
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
        if (attachedPlayer == null || mm == null) return;

        // Run A* locally on the client IN A BACKGROUND THREAD to prevent UI freezing
        // This eliminates GPS lag, reduces server CPU load, and maintains 60 FPS
        Vector3 start = new Vector3(attachedPlayer.getState().position);
        
        new Thread(() -> {
            List<Vector3> path = mm.findPath(start, target);
            if (pathfindingListener != null) {
                com.badlogic.gdx.Gdx.app.postRunnable(() -> {
                    pathfindingListener.onPathReceived(path);
                });
            }
        }).start();
    }

    public void sendPacket(OrderRequest req) {
        if (req != null)
            kryoClient.sendTCP(req);
    }

    public void sendPacket(ShopListRequest req) {
        if (req != null)
            kryoClient.sendTCP(req);
    }

    public void sendPacket(DriverPendingOrdersRequest req) {
        if (req != null)
            kryoClient.sendTCP(req);
    }

    public void sendOrderRequest(String shopId, int itemId) {
        if (attachedPlayer == null)
            return;
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

    public int getServerSessionId() {
        return serverSessionId;
    }

    public Main getGame() {
        return game;
    }

    public void clearPendingSpawn(int id) {
        pendingSpawns.remove(id);
    }
}
