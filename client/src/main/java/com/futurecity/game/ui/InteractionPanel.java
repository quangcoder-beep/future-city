package com.futurecity.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.futurecity.game.managers.NetworkManager;
import com.futurecity.game.entities.MainCharactor;
import com.futurecity.shared.enums.InteractionUIType;
import com.futurecity.shared.packets.resonse.InteractionResponse;
import com.futurecity.shared.packets.resonse.DriverPendingOrdersResponse;
import com.futurecity.shared.packets.resonse.InventoryResponse;
import com.futurecity.shared.packets.resonse.NewOrderNotification;
import com.futurecity.shared.packets.resonse.ShopListResponse;
import com.futurecity.shared.packets.request.DriverPendingOrdersRequest;
import com.futurecity.shared.packets.request.OrderRequest;
import com.futurecity.shared.packets.request.ShopListRequest;
import com.kotcrab.vis.ui.widget.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Bảng Tương tác Thống nhất (v3): Bảng điều khiển tích hợp cho các tương tác F, H và P.
 */
public class InteractionPanel extends BasePanel {
    private final NetworkManager networkManager;
    
    // Lưu trữ dữ liệu thống nhất
    private InteractionResponse currentData;
    private String currentTargetId;
    private InventoryResponse inventoryData;
    private List<ShopListResponse.ShopInfo> shopListData;
    private DriverPendingOrdersResponse pendingOrdersData;
    private List<NewOrderNotification> orderNotifications = new ArrayList<>();
    
    // Các hàm phản hồi (Callbacks)
    private Consumer<Vector3> onGpsRequested;
    private Runnable onStopGpsRequested;

    // Các thẻ (Tabs)
    public enum DashboardTab { INTERACT, INVENTORY, PHONE }
    private DashboardTab currentTab = DashboardTab.INTERACT;
    
    // Cấu trúc bố cục giao diện (UI Layout)
    private VisTable headerField;
    private VisTable userField;
    private VisTable workField;
    private VisTable actionField;
    private VisTable tabSelectionField;
    
    // Trạng thái (Status)
    private VisLabel statusLabel;
    private boolean isDeliveryTab = false; // Thẻ nội bộ cho CỬA HÀNG (Bảng việc làm vs Danh mục hàng)
    private List<Runnable> timerUpdates = new ArrayList<>();

    private MainCharactor player;

    public InteractionPanel(Skin skin, NetworkManager networkManager, MainCharactor player) {
        super("CITY DASHBOARD", skin);
        this.networkManager = networkManager;
        this.player = player;
        setSize(780, 650); 
        this.fullScreenMode = true;
        populateContent();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        for (Runnable r : timerUpdates) {
            r.run();
        }
    }

    @Override
    protected void populateContent() {
        contentTable.clear();
        contentTable.top().pad(15);

        // --- 0. KHU VỰC CHỌN THẺ ---
        tabSelectionField = new VisTable();
        contentTable.add(tabSelectionField).growX().padBottom(10).row();
        
        addSeparator(Color.GRAY);

        // --- 1. KHU VỰC TIÊU ĐỀ ---
        headerField = new VisTable();
        contentTable.add(headerField).growX().pad(5).row();

        addSeparator(new Color(0.3f, 0.3f, 0.3f, 1));

        // --- 2. KHU VỰC NGƯỜI DÙNG (Chỉ số/Ví) ---
        userField = new VisTable();
        contentTable.add(userField).growX().pad(5).row();
        
        addSeparator(new Color(0.2f, 0.2f, 0.2f, 1));

        // --- 3. KHU VỰC CÔNG VIỆC (Nội dung có thể cuộn) ---
        workField = new VisTable();
        VisScrollPane scroll = new VisScrollPane(workField);
        scroll.setFadeScrollBars(false);
        contentTable.add(scroll).grow().pad(5).row();

        // --- 4. KHU VỰC HÀNH ĐỘNG (Chân bảng) ---
        actionField = new VisTable();
        contentTable.add(actionField).growX().padTop(10).row();

        // Status Label
        statusLabel = new VisLabel("");
        statusLabel.setColor(Color.LIGHT_GRAY);
        contentTable.add(statusLabel).padTop(5).row();
    }

    private void addSeparator(Color color) {
        VisTable sep = new VisTable();
        sep.setBackground(skin.getDrawable("textfield-bg"));
        sep.setColor(color);
        contentTable.add(sep).height(2).growX().padLeft(5).padRight(5).row();
    }

    public DashboardTab getCurrentTab() { return currentTab; }

    public void switchToTab(DashboardTab tab) {
        this.currentTab = tab;
        networkManager.sendInventoryRequest(); // Always helpful
        if (tab == DashboardTab.PHONE) {
            networkManager.sendPacket(new ShopListRequest());
            networkManager.sendPacket(new DriverPendingOrdersRequest());
        }
        refreshDashboard();
        showPanel();
    }

    public void updateAndShow(InteractionResponse data) {
        this.currentData = data;
        this.currentTargetId = data.targetId;
        this.currentTab = DashboardTab.INTERACT;
        this.isDeliveryTab = false;
        refreshDashboard();
        showPanel();
    }

    private void refreshDashboard() {
        tabSelectionField.clear();
        headerField.clear();
        userField.clear();
        workField.clear();
        actionField.clear();
        timerUpdates.clear();
        statusLabel.setText("");

        buildTabSelectors();

        if (currentTab == DashboardTab.INTERACT) {
            if (currentData == null) {
                workField.add(new VisLabel("No interaction focus.")).pad(50);
            } else {
                renderInteractTab();
            }
        } else if (currentTab == DashboardTab.INVENTORY) {
            renderInventoryTab();
        } else if (currentTab == DashboardTab.PHONE) {
            renderPhoneTab();
        }
    }

    private void buildTabSelectors() {
        VisTextButton b1 = new VisTextButton("INTERACT (F)");
        VisTextButton b2 = new VisTextButton("INVENTORY (H)");
        VisTextButton b3 = new VisTextButton("SMARTPHONE (P)");

        if (currentTab == DashboardTab.INTERACT) b1.setColor(Color.GOLD);
        if (currentTab == DashboardTab.INVENTORY) b2.setColor(Color.GOLD);
        if (currentTab == DashboardTab.PHONE) b3.setColor(Color.GOLD);

        b1.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { currentTab = DashboardTab.INTERACT; refreshDashboard(); } });
        b2.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { switchToTab(DashboardTab.INVENTORY); } });
        b3.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { switchToTab(DashboardTab.PHONE); } });

        tabSelectionField.add(b1).width(240).padRight(5);
        tabSelectionField.add(b2).width(240).padRight(5);
        tabSelectionField.add(b3).width(240);
    }

    private void renderInteractTab() {
        String title = (currentData.displayName != null ? currentData.displayName : "OBJECT").toUpperCase();
        this.getTitleLabel().setText("DASHBOARD: " + title);
        
        headerField.add(new VisLabel(title, "title")).expandX().left();
        VisLabel typeLabel = new VisLabel(currentData.uiType.name().replace("_", " "));
        typeLabel.setColor(Color.CYAN); typeLabel.setFontScale(0.8f);
        headerField.add(typeLabel).right();

        userField.add(new Image(skin.getDrawable("icon-shop"))).size(24).padRight(5);
        VisLabel coinVal = new VisLabel("🪙 " + currentData.playerCoins + " Coins");
        coinVal.setColor(Color.GOLD);
        userField.add(coinVal).left().expandX();
        
        if (isShopType()) {
            VisTextButton subBtn = new VisTextButton(currentData.isSubscribed ? "Registered Courier 🛵" : "Join Courier Team");
            if (currentData.isSubscribed) subBtn.setColor(Color.GREEN);
            subBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    currentData.isSubscribed = !currentData.isSubscribed;
                    networkManager.sendShopSubscribeRequest(currentTargetId, currentData.isSubscribed);
                    refreshDashboard();
                }
            });
            userField.add(subBtn).right().width(220);
        }

        buildWorkContent();
        buildActionFooter();
    }

    private void renderInventoryTab() {
        this.getTitleLabel().setText("DASHBOARD: MY INVENTORY");
        headerField.add(new VisLabel("MY INVENTORY", "title")).expandX().left();
        
        if (inventoryData == null) {
            workField.add(new VisLabel("Updating inventory system...")).pad(40);
            return;
        }

        userField.add(new Image(skin.getDrawable("icon-shop"))).size(24).padRight(5);
        VisLabel coinVal = new VisLabel("🪙 " + inventoryData.playerCoins + " Coins");
        coinVal.setColor(Color.GOLD);
        userField.add(coinVal).left().expandX();

        // 1. Owned Items
        addSectionTitle("OWNED ITEMS");
        VisTable itemTable = new VisTable();
        if (inventoryData.itemNames != null && inventoryData.itemNames.length > 0) {
            int columns = 3;
            for (int i = 0; i < inventoryData.itemNames.length; i++) {
                VisTable card = new VisTable();
                card.setBackground(skin.getDrawable("textfield-bg"));
                card.pad(10);
                card.add(new Image(skin.getDrawable("icon-chest"))).size(45).row();
                VisLabel nameLab = new VisLabel(inventoryData.itemNames[i]);
                nameLab.setEllipsis(true);
                card.add(nameLab).width(120).padTop(5).row();
                VisLabel qty = new VisLabel("Quantity: " + inventoryData.quantities[i]);
                qty.setFontScale(0.85f); qty.setColor(Color.LIGHT_GRAY);
                card.add(qty);
                itemTable.add(card).pad(10).width(160);
                if ((i + 1) % columns == 0) itemTable.row();
            }
        } else {
            itemTable.add(new VisLabel("Your inventory is empty.")).pad(20);
        }
        workField.add(itemTable).pad(10).row();

        // 2. Active Deliveries (Shipper)
        addSectionTitle("ACTIVE DELIVERIES (SHIPPER)");
        if (inventoryData.activeDeliveries != null && !inventoryData.activeDeliveries.isEmpty()) {
            for (InventoryResponse.OrderInfo del : inventoryData.activeDeliveries) {
                renderOrderRow(workField, del, true);
            }
        } else {
            workField.add(new VisLabel("None")).pad(10).row();
        }

        // 3. My Orders (Buyer)
        addSectionTitle("MY ORDERS (BUYER)");
        if (inventoryData.myOrders != null && !inventoryData.myOrders.isEmpty()) {
            for (InventoryResponse.OrderInfo ord : inventoryData.myOrders) {
                renderOrderRow(workField, ord, false);
            }
        } else {
            workField.add(new VisLabel("None")).pad(10).row();
        }

        VisTextButton closeBtn = new VisTextButton("DISMISS");
        closeBtn.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { hidePanel(); } });
        actionField.add(closeBtn).width(160).height(45);
    }

    private void renderPhoneTab() {
        this.getTitleLabel().setText("DASHBOARD: VIRTUAL MOBILE");
        headerField.add(new VisLabel("VIRTUAL MOBILE 📱", "title")).expandX().left();
        
        // --- ADDED COIN DISPLAY TO PHONE ---
        userField.add(new Image(skin.getDrawable("icon-shop"))).size(24).padRight(5);
        int currentCoins = (inventoryData != null) ? inventoryData.playerCoins : (currentData != null ? currentData.playerCoins : 0);
        VisLabel coinVal = new VisLabel("🪙 " + currentCoins + " Coins");
        coinVal.setColor(Color.GOLD);
        userField.add(coinVal).left().expandX();
        // ------------------------------------

        statusLabel.setText("System: Online | Data Sync: OK");
        statusLabel.setColor(Color.GREEN);

        // 1. Notifications
        addSectionTitle("LIVE NOTIFICATIONS");
        VisTable notifTable = new VisTable();
        if (orderNotifications.isEmpty()) {
            notifTable.add(new VisLabel("No recent alerts.")).pad(10);
        } else {
            for (NewOrderNotification n : orderNotifications) {
                VisTable r = new VisTable();
                r.add(new VisLabel("🔔 " + n.itemName + " @ " + n.shopName)).left().expandX();
                VisTextButton v = new VisTextButton("LOG");
                v.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { statusLabel.setText("Logged order from " + n.shopName); } });
                r.add(v).width(60);
                notifTable.add(r).growX().padBottom(5).row();
            }
        }
        workField.add(notifTable).growX().pad(10).row();

        // 2. Shopping Apps
        addSectionTitle("SHOPPING APPLICATIONS");
        VisTable shopTable = new VisTable();
        if (shopListData != null && !shopListData.isEmpty()) {
            for (ShopListResponse.ShopInfo s : shopListData) {
                VisLabel sn = new VisLabel(s.shopName); sn.setColor(Color.CYAN);
                shopTable.add(sn).left().padTop(10).row();
                for (ShopListResponse.ShopItemInfo item : s.items) {
                    VisTable row = new VisTable();
                    VisTable infoTable = new VisTable();
                    infoTable.add(new VisLabel(item.itemName)).left().row();
                    int estShip = 20 + (int)(item.price * 0.1f);
                    VisLabel priceLabel = new VisLabel("Price: " + item.price + " + Ship: ~" + estShip);
                    priceLabel.setFontScale(0.85f); priceLabel.setColor(Color.LIGHT_GRAY);
                    infoTable.add(priceLabel).left();
                    row.add(infoTable).left().expandX();
                    VisTable btnTable = new VisTable();
                    btnTable.add(new Image(skin.getDrawable("icon-shop"))).size(24).padRight(5);
                    VisTextButton orderBtn = new VisTextButton("ORDER");
                    orderBtn.addListener(new ClickListener() { @Override public void clicked(InputEvent event, float x, float y) { networkManager.sendOrderRequest(s.shopId, item.itemId); switchToTab(DashboardTab.INVENTORY); } });
                    btnTable.add(orderBtn).width(80);
                    row.add(btnTable).width(120).padLeft(10);
                    shopTable.add(row).growX().padBottom(8).row();
                }
            }
        } else {
            shopTable.add(new VisLabel("No shopping network available.")).pad(20);
        }
        workField.add(shopTable).growX().pad(10).row();

        // 3. Pending Orders (Job Board)
        addSectionTitle("PENDING ORDERS (DRIVER)");
        if (pendingOrdersData != null && pendingOrdersData.orders != null && !pendingOrdersData.orders.isEmpty()) {
            for (DriverPendingOrdersResponse.OrderInfo p : pendingOrdersData.orders) {
                VisTable row = new VisTable();
                row.setBackground(skin.getDrawable("textfield-bg"));
                row.pad(10);
                
                VisLabel desc = new VisLabel(p.itemName + " @ " + p.shopName);
                desc.setEllipsis(true);
                row.add(desc).left().expandX();
                
                VisLabel reward = new VisLabel("+$" + p.reward);
                reward.setColor(Color.GREEN);
                row.add(reward).padRight(5);
                
                // SHOP GPS
                VisImageButton gpsS = new VisImageButton(skin.getDrawable("icon-gps"));
                gpsS.addListener(new ClickListener() { 
                    @Override public void clicked(InputEvent e, float x, float y) { 
                        if(onGpsRequested!=null) onGpsRequested.accept(new Vector3(p.shopX, p.shopY, p.shopZ)); 
                        statusLabel.setText("GPS -> Shop " + p.shopName);
                    } 
                });
                row.add(gpsS).size(36).padRight(2);

                // DEST GPS
                VisImageButton gpsD = new VisImageButton(skin.getDrawable("icon-gps"));
                gpsD.setColor(Color.GREEN);
                gpsD.addListener(new ClickListener() { 
                    @Override public void clicked(InputEvent e, float x, float y) { 
                        if(onGpsRequested!=null) onGpsRequested.accept(new Vector3(p.destX, p.destY, p.destZ)); 
                        statusLabel.setText("GPS -> Customer " + p.destinationName);
                    } 
                });
                row.add(gpsD).size(36).padRight(5);

                VisTextButton acc = new VisTextButton("OK");
                acc.addListener(new ClickListener() { 
                    @Override public void clicked(InputEvent e, float x, float y) { 
                        networkManager.sendDeliveryAcceptRequest(p.orderId); 
                        switchToTab(DashboardTab.INVENTORY); 
                    } 
                });
                row.add(acc).width(80);
                workField.add(row).growX().padBottom(5).row();
            }
        } else {
            workField.add(new VisLabel("No contracts found on the network.")).pad(10).row();
        }

        VisTextButton closeBtn = new VisTextButton("POWER OFF");
        closeBtn.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { hidePanel(); } });
        actionField.add(closeBtn).width(200).height(50);
    }

    private void addSectionTitle(String title) {
        VisLabel l = new VisLabel("--- " + title.toUpperCase() + " ---");
        l.setColor(Color.CYAN);
        workField.add(l).center().expandX().padTop(20).padBottom(10).row();
    }

    private void renderOrderRow(VisTable target, InventoryResponse.OrderInfo order, boolean isShipper) {
        VisTable row = new VisTable();
        row.setBackground(skin.getDrawable("textfield-bg"));
        row.pad(10);
        
        String stStr = order.status;
        if (isShipper) {
            stStr = "PENDING_PICKUP".equals(order.status) ? "[PENDING PICKUP]" : "[DELIVERING]";
        }
        VisLabel st = new VisLabel(stStr);
        st.setColor(isShipper ? ("PENDING_PICKUP".equals(order.status) ? Color.CYAN : Color.GREEN) : Color.YELLOW);
        row.add(st).colspan(isShipper ? 5 : 2).left().row();

        row.add(new Image(skin.getDrawable("icon-chest"))).size(28).padRight(5);
        VisLabel nameLab = new VisLabel(order.itemName);
        nameLab.setEllipsis(true);
        row.add(nameLab).left().expandX();
        
        if (isShipper) {
            VisTable payT = new VisTable();
            payT.add(new Image(skin.getDrawable("icon-shop"))).size(18).padRight(3);
            VisLabel pL = new VisLabel("+" + order.reward); pL.setColor(Color.GOLD);
            payT.add(pL);
            row.add(payT).width(100).right().padRight(15);
            
            VisImageButton gpsS = new VisImageButton(skin.getDrawable("icon-gps"));
            gpsS.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { if(onGpsRequested!=null) onGpsRequested.accept(new Vector3(order.shopX, order.shopY, order.shopZ)); statusLabel.setText("GPS: Shop"); } });
            row.add(gpsS).size(36).padRight(2);

            VisImageButton gpsD = new VisImageButton(skin.getDrawable("icon-gps"));
            gpsD.setColor(Color.GREEN);
            gpsD.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { if(onGpsRequested!=null) onGpsRequested.accept(new Vector3(order.destX, order.destY, order.destZ)); statusLabel.setText("GPS: " + order.destinationName); } });
            row.add(gpsD).size(36).padRight(10);

            VisTextButton stop = new VisTextButton("STOP");
            stop.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { if(onStopGpsRequested!=null) onStopGpsRequested.run(); statusLabel.setText("Navigation Off."); } });
            row.add(stop).width(65).padRight(5);

            VisTextButton cancel = new VisTextButton("CANCEL");
            cancel.setColor(Color.RED);
            cancel.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { networkManager.sendDeliveryCancelRequest(order.orderId); statusLabel.setText("Requesting Cancel..."); } });
            row.add(cancel).width(80);

            // SMART PICKUP/HANDOVER BUTTON
            if ("PENDING_PICKUP".equals(order.status) || "DELIVERING".equals(order.status)) {
                float dist = 999f;
                if (player != null) {
                    float tx = order.destX;
                    float ty = order.destY;
                    float tz = order.destZ;

                    if ("PENDING_PICKUP".equals(order.status)) {
                        tx = order.shopX;
                        ty = order.shopY;
                        tz = order.shopZ;
                    } else if ("DELIVERING".equals(order.status) && "PLAYER".equals(order.buyerType)) {
                        // Dynamic P2P distance check
                        try {
                            int buyerDbId = Integer.parseInt(order.buyerRefId);
                            com.futurecity.game.entities.RemotePlayer buyer = null;
                            for (com.futurecity.game.entities.RemotePlayer rp : networkManager.getRemotePlayers().values()) {
                                if (rp.getDbUserId() == buyerDbId) {
                                    buyer = rp;
                                    break;
                                }
                            }
                            if (buyer != null) {
                                tx = buyer.getPosition().x;
                                ty = buyer.getPosition().y;
                                tz = buyer.getPosition().z;
                            }
                        } catch (Exception ex) {}
                    }
                    dist = com.badlogic.gdx.math.Vector3.dst(player.getPosition().x, player.getPosition().y, player.getPosition().z, tx, ty, tz);
                }
                
                if (dist < 200.0f) {
                    String btnTxt = "PENDING_PICKUP".equals(order.status) ? "PICK UP" : ("PLAYER".equals(order.buyerType) ? "SIGN" : "DELIVER");
                    VisTextButton smartAction = new VisTextButton(btnTxt);
                    smartAction.setColor(Color.CYAN);
                    smartAction.addListener(new ClickListener() {
                        @Override public void clicked(InputEvent e, float x, float y) {
                            if ("PENDING_PICKUP".equals(order.status)) {
                                networkManager.sendDeliveryPickupRequest(order.orderId);
                                statusLabel.setText("Picking up...");
                            } else {
                                networkManager.sendDeliveryHandoverRequest(order.orderId);
                                statusLabel.setText("Handing over...");
                            }
                        }
                    });
                    row.add(smartAction).width(100).padLeft(5);
                }
            }

            target.add(row).growX().pad(5).row();

            if (order.acceptedAtMs > 0 && order.timeoutMinutes > 0) {
                VisLabel timer = new VisLabel(""); timer.setColor(Color.ORANGE);
                target.add(timer).left().padLeft(35).padBottom(12).row();
                long expiry = order.acceptedAtMs + (order.timeoutMinutes * 60000L);
                timerUpdates.add(() -> {
                    long rem = expiry - System.currentTimeMillis();
                    if (rem > 0) {
                        long m = (rem/1000)/60; long s = (rem/1000)%60;
                        timer.setText(String.format("Time left: %02d:%02d", m, s));
                        if (rem < 60000) timer.setColor(Color.RED);
                    } else {
                        timer.setText("EXPIRED!"); timer.setColor(Color.RED);
                    }
                });
            }
        } else {
            row.add(st).right().padRight(15);
            if ("PENDING".equals(order.status)) {
                VisTextButton skip = new VisTextButton("CANCEL");
                skip.setColor(Color.RED);
                skip.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { networkManager.sendOrderCancelRequest(order.orderId); } });
                row.add(skip).width(85);
            }
            target.add(row).growX().pad(3).row();
        }
    }

    private void buildWorkContent() {
        InteractionUIType type = currentData.uiType;
        if (isShopType()) {
            VisTable tabs = new VisTable();
            VisTextButton catB = new VisTextButton("PHYSICAL CATALOG");
            VisTextButton jobB = new VisTextButton("LOGISTICS BOARD");
            if (!isDeliveryTab) catB.setColor(Color.SKY); else jobB.setColor(Color.SKY);
            catB.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { isDeliveryTab = false; refreshDashboard(); } });
            jobB.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { isDeliveryTab = true; refreshDashboard(); } });
            tabs.add(catB).width(260).padRight(12);
            tabs.add(jobB).width(260);
            workField.add(tabs).padBottom(20).row();
            if (isDeliveryTab) buildDeliveryTable(); else buildItemTable();
        } else if (type == InteractionUIType.PLAYER_INTERACT) {
            VisTable stats = new VisTable();
            stats.add(new VisLabel("Citizen Name:")).left().padRight(15);
            VisLabel n = new VisLabel(currentData.displayName); n.setColor(Color.YELLOW);
            stats.add(n).left().row();
            stats.add(new VisLabel("Citizen ID:")).left().padRight(15);
            stats.add(new VisLabel(currentData.targetId)).left().row();
            workField.add(stats).pad(25).center().row();
            VisLabel msg = new VisLabel(currentData.message != null ? currentData.message : "Standard citizen interaction protocols active.");
            msg.setWrap(true); msg.setAlignment(Align.center);
            workField.add(msg).width(550).pad(10);
        } else if (type == InteractionUIType.NPC_DIALOGUE) {
            if (currentData.dialogueLines != null) {
                for (String line : currentData.dialogueLines) {
                    VisLabel l = new VisLabel(line); l.setWrap(true); l.setAlignment(Align.center);
                    workField.add(l).width(550).pad(10).row();
                }
            }
        } else {
            VisLabel l = new VisLabel(currentData.message); l.setWrap(true); l.setAlignment(Align.center);
            workField.add(l).width(550).pad(25);
        }
    }

    private void buildItemTable() {
        VisTable t = new VisTable();
        if (currentData.itemNames == null || currentData.itemNames.length == 0) {
            t.add(new VisLabel("No products available here.")).pad(40);
        } else {
            // Headers
            t.add(new VisLabel("Name")).width(200).pad(3);
            t.add(new VisLabel("Price")).width(100).pad(3);
            t.add(new VisLabel("Stock")).width(100).pad(3);
            t.add(new VisLabel("")).width(90).pad(3);
            t.row();

            for (int i=0; i<currentData.itemNames.length; i++) {
                final int id = currentData.itemIds[i]; final String nm = currentData.itemNames[i];
                final int price = currentData.itemPrices[i]; final int stock = currentData.itemStocks[i];

                t.add(new VisLabel(nm)).width(200).left();
                VisLabel pr = new VisLabel("🪙 " + price); pr.setColor(Color.GOLD);
                t.add(pr).width(100).center();
                VisLabel stk = new VisLabel(stock + "");
                stk.setColor(stock > 0 ? Color.GREEN : Color.RED);
                t.add(stk).width(100).center();

                String bTxt = "BUY";
                boolean canBuy = true;
                if (stock <= 0) { bTxt = "EMPTY"; canBuy = false; }
                else if (currentData.playerCoins < price) { bTxt = "LOW $"; canBuy = false; }

                VisTextButton b = new VisTextButton(bTxt);
                if (!canBuy) b.setDisabled(true);
                b.addListener(new ClickListener(){ @Override public void clicked(InputEvent e, float x, float y) { if(!b.isDisabled()) networkManager.sendBuyRequest(currentTargetId, id); } });
                t.add(b).width(90).padLeft(12).row();
            }
        }
        workField.add(t).growX();
    }

    private void buildDeliveryTable() {
        if (!currentData.isSubscribed) {
            workField.add(new VisLabel("Courier license required for this board.")).pad(45);
            return;
        }
        VisTable t = new VisTable();
        if (currentData.orderIds == null || currentData.orderIds.length == 0) {
            t.add(new VisLabel("No logistical tasks available.")).pad(40);
        } else {
            // Headers
            t.add(new VisLabel("Order")).width(200).pad(3);
            t.add(new VisLabel("Reward")).width(100).pad(3);
            t.add(new VisLabel("To")).width(160).pad(3);
            t.add(new VisLabel("")).width(90).pad(3);
            t.row();

            for (int i=0; i<currentData.orderIds.length; i++) {
                final int oid = currentData.orderIds[i];
                t.add(new VisLabel(currentData.orderItemNames[i])).width(200).left();
                VisLabel pay = new VisLabel("+$" + currentData.orderRewards[i]); pay.setColor(Color.GREEN);
                t.add(pay).width(100).center();
                t.add(new VisLabel(currentData.orderDestinations[i])).width(160).center();
                VisTextButton a = new VisTextButton("ACCEPT");
                a.addListener(new ClickListener(){ @Override public void clicked(InputEvent e, float x, float y) { networkManager.sendDeliveryAcceptRequest(oid); hidePanel(); } });
                t.add(a).width(90).padLeft(12).row();
            }
        }
        workField.add(t).growX();
    }

    private void buildActionFooter() {
        InteractionUIType type = currentData.uiType;
        if (type == InteractionUIType.PICKUP_ACTION) {
             VisTextButton pickupBtn = new VisTextButton("PICK UP");
             pickupBtn.addListener(new ClickListener() {
                 @Override
                 public void clicked(InputEvent event, float x, float y) {
                     networkManager.sendDeliveryPickupRequest(currentData.activeOrderId);
                     hidePanel();
                 }
             });
             actionField.add(pickupBtn).width(260).height(55).padRight(25);
        }
        else if (type == InteractionUIType.HANDOVER_ACTION || type == InteractionUIType.PLAYER_INTERACT) {
             String txt = (type == InteractionUIType.HANDOVER_ACTION && currentData.isPlayerTarget) ? "REQUEST SIGNATURE" : "COMPLETE DELIVERY";
             if (type == InteractionUIType.PLAYER_INTERACT) txt = "GIVE ITEM";
             VisTextButton main = new VisTextButton(txt);
             main.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { networkManager.sendDeliveryHandoverRequest(currentData.activeOrderId); hidePanel(); } });
             actionField.add(main).width(260).height(55).padRight(25);
        }
        VisTextButton close = new VisTextButton("EXIT");
        close.addListener(new ClickListener() { @Override public void clicked(InputEvent e, float x, float y) { hidePanel(); } });
        actionField.add(close).width(160).height(55);
    }

    private boolean isShopType() {
        InteractionUIType t = currentData.uiType;
        return t == InteractionUIType.SHOP_GENERAL || t == InteractionUIType.SHOP_FOOD || t == InteractionUIType.SHOP_CLOTHES || t == InteractionUIType.PICKUP_ACTION;
    }

    public void setOnGpsRequested(Consumer<Vector3> cb) { this.onGpsRequested = cb; }
    public void setOnStopGpsRequested(Runnable cb) { this.onStopGpsRequested = cb; }

    public void updateInventory(InventoryResponse res) {
        this.inventoryData = res;
        if (currentData != null) currentData.playerCoins = res.playerCoins;
        if (currentTab != DashboardTab.INTERACT) refreshDashboard();
    }

    public void updateShops(List<ShopListResponse.ShopInfo> shops) {
        this.shopListData = shops;
        if (currentTab == DashboardTab.PHONE) refreshDashboard();
    }

    public void updatePendingOrders(DriverPendingOrdersResponse response) {
        this.pendingOrdersData = response;
        if (currentTab == DashboardTab.PHONE) refreshDashboard();
    }

    public void onBuySuccess(int coins, int itemId) {
        // Sync balance across all data structures
        if (currentData != null) {
            currentData.playerCoins = coins;
            if (currentData.itemIds != null) {
                for (int i=0; i<currentData.itemIds.length; i++) {
                    if (currentData.itemIds[i] == itemId) { currentData.itemStocks[i]--; break; }
                }
            }
        }
        if (inventoryData != null) {
            inventoryData.playerCoins = coins;
        }

        statusLabel.setText("✓ Transaction Success. New Balance: " + coins);
        statusLabel.setColor(Color.GREEN);
        refreshDashboard();
    }

    public void addNewOrder(NewOrderNotification notif) {
        this.orderNotifications.add(0, notif);
        if (orderNotifications.size() > 10) orderNotifications.remove(10);
        if (currentTab == DashboardTab.PHONE || (currentTab == DashboardTab.INTERACT && isDeliveryTab)) refreshDashboard();
    }
}
