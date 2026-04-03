package com.futurecity.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.futurecity.game.core.Main;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.futurecity.game.managers.NetworkManager;
import com.futurecity.game.managers.NotificationManager;
import com.futurecity.game.entities.MainCharactor;
import com.futurecity.shared.enums.InteractionUIType;
import com.futurecity.shared.packets.resonse.InteractionResponse;
import com.futurecity.shared.packets.resonse.DriverPendingOrdersResponse;
import com.futurecity.shared.packets.resonse.InventoryResponse;
import com.futurecity.shared.packets.resonse.NewOrderNotification;
import com.futurecity.shared.packets.resonse.ShopListResponse;
import com.futurecity.shared.packets.request.DriverPendingOrdersRequest;
import com.futurecity.shared.packets.request.ShopListRequest;
import com.kotcrab.vis.ui.widget.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Premium Unified Dashboard (v4): Merges InteractionPanel + PhonePanel.
 * Features: Underline tab indicator, card-based layout, neon section dividers.
 */
public class InteractionPanel extends BasePanel {
    private final NetworkManager networkManager;
    private final Main game;
    private NotificationManager notificationManager;

    // Data storage
    private InteractionResponse currentData;
    private String currentTargetId;
    private InventoryResponse inventoryData;
    private List<ShopListResponse.ShopInfo> shopListData;
    private DriverPendingOrdersResponse pendingOrdersData;
    private List<NewOrderNotification> orderNotifications = new ArrayList<>();

    // Callbacks
    private Consumer<Vector3> onGpsRequested;
    private Runnable onStopGpsRequested;

    // Tabs
    public enum DashboardTab {
        INTERACT, INVENTORY, PHONE
    }

    private DashboardTab currentTab = DashboardTab.INTERACT;

    // Layout structure
    private VisTable headerField;
    private VisTable userField;
    private VisTable workField;
    private VisTable actionField;
    private VisTable tabSelectionField;

    // Status
    private VisLabel statusLabel;
    private boolean isDeliveryTab = false;
    private List<Runnable> timerUpdates = new ArrayList<>();

    private MainCharactor player;

    public InteractionPanel(Skin skin, NetworkManager networkManager, Main game, MainCharactor player,
            NotificationManager notificationManager) {
        super("CITY DASHBOARD", skin);
        this.networkManager = networkManager;
        this.game = game;
        this.player = player;
        this.notificationManager = notificationManager;
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

        // Tab Selection
        tabSelectionField = new VisTable();
        contentTable.add(tabSelectionField).growX().padBottom(5).row();

        // Header
        headerField = new VisTable();
        contentTable.add(headerField).growX().pad(5).row();

        addNeonSeparator();

        // User section (coins/wallet)
        userField = new VisTable();
        contentTable.add(userField).growX().pad(5).row();

        addNeonSeparator();

        // Work section (scrollable content)
        workField = new VisTable();
        workField.top();
        VisScrollPane scroll = new VisScrollPane(workField);
        scroll.setFadeScrollBars(false);
        enableAutoScrollFocus(scroll);
        contentTable.add(scroll).grow().pad(5).row();

        // Action footer
        actionField = new VisTable();
        contentTable.add(actionField).growX().padTop(10).row();

        // Status
        statusLabel = new VisLabel("", "small");
        statusLabel.setColor(SkinLoader.TEXT_DIM);
        contentTable.add(statusLabel).padTop(5).row();
    }

    private void addNeonSeparator() {
        if (skin.has("separator-neon", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
            contentTable.add(new Image(skin.getDrawable("separator-neon"))).height(2).growX().padLeft(5).padRight(5).row();
        } else {
            VisTable sep = new VisTable();
            sep.setBackground(skin.getDrawable("textfield-bg"));
            sep.setColor(SkinLoader.SEPARATOR);
            contentTable.add(sep).height(2).growX().padLeft(5).padRight(5).row();
        }
    }

    public DashboardTab getCurrentTab() {
        return currentTab;
    }

    public void switchToTab(DashboardTab tab) {
        this.currentTab = tab;
        networkManager.sendInventoryRequest();
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
                workField.add(new VisLabel("No interaction focus.", "small")).pad(50);
            } else {
                renderInteractTab();
            }
        } else if (currentTab == DashboardTab.INVENTORY) {
            renderInventoryTab();
        } else if (currentTab == DashboardTab.PHONE) {
            renderPhoneTab();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // TAB SELECTOR with underline indicator
    // ═══════════════════════════════════════════════════════════

    private void buildTabSelectors() {
        tabSelectionField.pad(5);
        String[] tabNames = {"INTERACT (F)", "INVENTORY (H)", "SMARTPHONE (P)"};
        DashboardTab[] tabs = DashboardTab.values();

        for (int i = 0; i < tabs.length; i++) {
            final DashboardTab tab = tabs[i];
            boolean isActive = (currentTab == tab);

            VisTable tabCell = new VisTable();
            tabCell.pad(8, 15, 5, 15);

            // Tab label
            VisLabel tabLabel = new VisLabel(tabNames[i], isActive ? "tab-active" : "tab-inactive");
            tabCell.add(tabLabel).row();

            // Underline indicator
            if (isActive && skin.has("tab-indicator", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
                Image indicator = new Image(skin.getDrawable("tab-indicator"));
                tabCell.add(indicator).height(3).growX().padTop(4);
            } else {
                tabCell.add().height(3).growX().padTop(4); // Spacer for alignment
            }

            tabCell.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent e, float x, float y) {
                    if (game.audioManager != null) game.audioManager.playSfx("sounds/ui/click.ogg");
                    if (tab == DashboardTab.INTERACT) {
                        currentTab = DashboardTab.INTERACT;
                        refreshDashboard();
                    } else {
                        switchToTab(tab);
                    }
                }
            });

            tabSelectionField.add(tabCell).expandX();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // INTERACT TAB
    // ═══════════════════════════════════════════════════════════

    private void renderInteractTab() {
        String title = (currentData.displayName != null ? currentData.displayName : "OBJECT").toUpperCase();
        this.getTitleLabel().setText("DASHBOARD: " + title);

        headerField.add(new VisLabel(title, "title")).expandX().left();
        VisLabel typeLabel = new VisLabel(currentData.uiType.name().replace("_", " "), "small");
        typeLabel.setColor(SkinLoader.NEON_CYAN);
        headerField.add(typeLabel).right();

        // Coins
        userField.add(new Image(skin.getDrawable("icon-shop"))).size(24).padRight(5);
        VisLabel coinVal = new VisLabel(currentData.playerCoins + " Coins", "small");
        coinVal.setColor(SkinLoader.NEON_GOLD);
        userField.add(coinVal).left().expandX();

        if (isShopType()) {
            VisTextButton subBtn = new VisTextButton(
                    currentData.isSubscribed ? "Registered Courier" : "Join Courier Team", "neon-button");
            if (currentData.isSubscribed)
                subBtn.setColor(SkinLoader.NEON_LIME);
            if (game.audioManager != null) game.audioManager.attachDefaultTo(subBtn);
            subBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    currentData.isSubscribed = !currentData.isSubscribed;
                    networkManager.sendShopSubscribeRequest(currentTargetId, currentData.isSubscribed);
                    refreshDashboard();
                }
            });
            userField.add(subBtn).right().width(220).height(35);
        }

        buildWorkContent();
        buildActionFooter();
    }

    // ═══════════════════════════════════════════════════════════
    // INVENTORY TAB — Card-based layout
    // ═══════════════════════════════════════════════════════════

    private void renderInventoryTab() {
        this.getTitleLabel().setText("DASHBOARD: MY INVENTORY");
        headerField.add(new VisLabel("MY INVENTORY", "title")).expandX().left();

        if (inventoryData == null) {
            workField.add(new VisLabel("Syncing inventory...", "small")).pad(40);
            return;
        }

        userField.add(new Image(skin.getDrawable("icon-shop"))).size(24).padRight(5);
        VisLabel coinVal = new VisLabel(inventoryData.playerCoins + " Coins", "small");
        coinVal.setColor(SkinLoader.NEON_GOLD);
        userField.add(coinVal).left().expandX();

        // 1. Owned Items — Card Grid
        addSectionTitle("OWNED ITEMS");
        VisTable itemGrid = new VisTable();
        if (inventoryData.itemNames != null && inventoryData.itemNames.length > 0) {
            int columns = 3;
            for (int i = 0; i < inventoryData.itemNames.length; i++) {
                VisTable card = new VisTable();
                if (skin.has("card-bg", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
                    card.setBackground(skin.getDrawable("card-bg"));
                }
                card.pad(12);
                card.add(new Image(skin.getDrawable("icon-chest"))).size(40).row();
                VisLabel nameLab = new VisLabel(inventoryData.itemNames[i], "small");
                nameLab.setEllipsis(true);
                nameLab.setColor(Color.WHITE);
                card.add(nameLab).width(110).padTop(6).row();
                VisLabel qty = new VisLabel("x" + inventoryData.quantities[i], "small");
                qty.setColor(SkinLoader.TEXT_DIM);
                card.add(qty).padTop(3);
                itemGrid.add(card).pad(6).width(140).height(110);
                if ((i + 1) % columns == 0)
                    itemGrid.row();
            }
        } else {
            itemGrid.add(new VisLabel("Your inventory is empty.", "small")).pad(20);
        }
        workField.add(itemGrid).pad(10).row();

        // 2. Active Deliveries
        addSectionTitle("ACTIVE DELIVERIES");
        if (inventoryData.activeDeliveries != null && !inventoryData.activeDeliveries.isEmpty()) {
            for (InventoryResponse.OrderInfo del : inventoryData.activeDeliveries) {
                renderOrderRow(workField, del, true);
            }
        } else {
            workField.add(new VisLabel("None", "small")).pad(10).row();
        }

        // 3. My Orders
        addSectionTitle("MY ORDERS");
        if (inventoryData.myOrders != null && !inventoryData.myOrders.isEmpty()) {
            for (InventoryResponse.OrderInfo ord : inventoryData.myOrders) {
                renderOrderRow(workField, ord, false);
            }
        } else {
            workField.add(new VisLabel("None", "small")).pad(10).row();
        }

        VisTextButton closeBtn = new VisTextButton("DISMISS", "neon-button");
        if (game.audioManager != null) game.audioManager.attachDefaultTo(closeBtn);
        closeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                hidePanel();
            }
        });
        actionField.add(closeBtn).width(160).height(42);
    }

    // ═══════════════════════════════════════════════════════════
    // PHONE TAB (Merged from PhonePanel)
    // ═══════════════════════════════════════════════════════════

    private void renderPhoneTab() {
        this.getTitleLabel().setText("DASHBOARD: VIRTUAL MOBILE");
        headerField.add(new VisLabel("VIRTUAL MOBILE", "title")).expandX().left();

        // Coins
        userField.add(new Image(skin.getDrawable("icon-shop"))).size(24).padRight(5);
        int currentCoins = (inventoryData != null) ? inventoryData.playerCoins
                : (currentData != null ? currentData.playerCoins : 0);
        VisLabel coinVal = new VisLabel(currentCoins + " Coins", "small");
        coinVal.setColor(SkinLoader.NEON_GOLD);
        userField.add(coinVal).left().expandX();

        statusLabel.setText("System: Online | Data Sync: OK");
        statusLabel.setColor(SkinLoader.NEON_LIME);

        // 1. Notifications
        addSectionTitle("LIVE NOTIFICATIONS");
        VisTable notifTable = new VisTable();
        if (orderNotifications.isEmpty()) {
            notifTable.add(new VisLabel("No recent alerts.", "small")).pad(10);
        } else {
            for (NewOrderNotification n : orderNotifications) {
                VisTable r = new VisTable();
                if (skin.has("card-bg", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
                    r.setBackground(skin.getDrawable("card-bg"));
                }
                r.pad(8);
                VisLabel alertLabel = new VisLabel(n.itemName + " @ " + n.shopName, "small");
                alertLabel.setColor(SkinLoader.NEON_GOLD);
                r.add(alertLabel).left().expandX();
                VisTextButton v = new VisTextButton("LOG");
                if (game.audioManager != null) game.audioManager.attachDefaultTo(v);
                v.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent e, float x, float y) {
                        statusLabel.setText("Logged order from " + n.shopName);
                    }
                });
                r.add(v).width(60);
                notifTable.add(r).growX().padBottom(4).row();
            }
        }
        workField.add(notifTable).growX().pad(10).row();

        // 2. Shopping Apps
        addSectionTitle("SHOPPING APPLICATIONS");
        VisTable shopTable = new VisTable();
        if (shopListData != null && !shopListData.isEmpty()) {
            for (ShopListResponse.ShopInfo s : shopListData) {
                VisLabel sn = new VisLabel(s.shopName, "small");
                sn.setColor(SkinLoader.NEON_CYAN);
                shopTable.add(sn).left().padTop(10).row();
                for (ShopListResponse.ShopItemInfo item : s.items) {
                    VisTable row = new VisTable();
                    if (skin.has("card-bg", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
                        row.setBackground(skin.getDrawable("card-bg"));
                    }
                    row.pad(8);
                    VisTable infoTable = new VisTable();
                    infoTable.add(new VisLabel(item.itemName, "small")).left().row();
                    int estShip = 20 + (int) (item.price * 0.1f);
                    VisLabel priceLabel = new VisLabel("Price: " + item.price + " + Ship: ~" + estShip, "small");
                    priceLabel.setColor(SkinLoader.TEXT_DIM);
                    infoTable.add(priceLabel).left();
                    row.add(infoTable).left().expandX();
                    VisTable btnTable = new VisTable();
                    btnTable.add(new Image(skin.getDrawable("icon-shop"))).size(20).padRight(5);
                    VisTextButton orderBtn = new VisTextButton("ORDER");
                    if (game.audioManager != null) game.audioManager.attachDefaultTo(orderBtn);
                    orderBtn.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            networkManager.sendOrderRequest(s.shopId, item.itemId);
                            switchToTab(DashboardTab.INVENTORY);
                        }
                    });
                    btnTable.add(orderBtn).width(80);
                    row.add(btnTable).width(120).padLeft(10);
                    shopTable.add(row).growX().padBottom(6).row();
                }
            }
        } else {
            shopTable.add(new VisLabel("No shopping network available.", "small")).pad(20);
        }
        workField.add(shopTable).growX().pad(10).row();

        // 3. Pending Orders
        addSectionTitle("PENDING ORDERS (DRIVER)");
        if (pendingOrdersData != null && pendingOrdersData.orders != null && !pendingOrdersData.orders.isEmpty()) {
            for (DriverPendingOrdersResponse.OrderInfo p : pendingOrdersData.orders) {
                VisTable row = new VisTable();
                if (skin.has("card-bg", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
                    row.setBackground(skin.getDrawable("card-bg"));
                }
                row.pad(10);

                VisLabel desc = new VisLabel(p.itemName + " @ " + p.shopName, "small");
                desc.setEllipsis(true);
                row.add(desc).left().expandX();

                VisLabel reward = new VisLabel("+$" + p.reward, "small");
                reward.setColor(SkinLoader.NEON_LIME);
                row.add(reward).padRight(8);

                // Shop GPS
                VisImageButton gpsS = new VisImageButton(skin.getDrawable("icon-gps"));
                if (game.audioManager != null) game.audioManager.attachDefaultTo(gpsS);
                gpsS.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent e, float x, float y) {
                        if (onGpsRequested != null)
                            onGpsRequested.accept(new Vector3(p.shopX, p.shopY, p.shopZ));
                        statusLabel.setText("GPS -> Shop " + p.shopName);
                    }
                });
                row.add(gpsS).size(32).padRight(2);

                // Dest GPS
                VisImageButton gpsD = new VisImageButton(skin.getDrawable("icon-gps"));
                if (game.audioManager != null) game.audioManager.attachDefaultTo(gpsD);
                gpsD.setColor(SkinLoader.NEON_LIME);
                gpsD.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent e, float x, float y) {
                        if (onGpsRequested != null)
                            onGpsRequested.accept(new Vector3(p.destX, p.destY, p.destZ));
                        statusLabel.setText("GPS -> Customer " + p.destinationName);
                    }
                });
                row.add(gpsD).size(32).padRight(5);

                VisTextButton acc = new VisTextButton("OK");
                if (game.audioManager != null) game.audioManager.attachDefaultTo(acc);
                acc.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent e, float x, float y) {
                        networkManager.sendDeliveryAcceptRequest(p.orderId);
                        switchToTab(DashboardTab.INVENTORY);
                    }
                });
                row.add(acc).width(70);
                workField.add(row).growX().padBottom(5).row();
            }
        } else {
            workField.add(new VisLabel("No contracts found.", "small")).pad(10).row();
        }

        VisTextButton closeBtn = new VisTextButton("POWER OFF", "neon-button");
        if (game.audioManager != null) game.audioManager.attachDefaultTo(closeBtn);
        closeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                hidePanel();
            }
        });
        actionField.add(closeBtn).width(200).height(45);
    }

    // ═══════════════════════════════════════════════════════════
    // SHARED UI HELPERS
    // ═══════════════════════════════════════════════════════════

    private void addSectionTitle(String title) {
        VisTable header = new VisTable();
        if (skin.has("separator-neon", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
            header.add(new Image(skin.getDrawable("separator-neon"))).height(2).width(15).padRight(6);
        }
        VisLabel l = new VisLabel(title.toUpperCase(), "small");
        l.setColor(SkinLoader.NEON_CYAN);
        header.add(l);
        if (skin.has("separator-neon", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
            header.add(new Image(skin.getDrawable("separator-neon"))).height(2).expandX().fillX().padLeft(6);
        }
        workField.add(header).growX().padTop(18).padBottom(8).row();
    }

    private void renderOrderRow(VisTable target, InventoryResponse.OrderInfo order, boolean isShipper) {
        VisTable row = new VisTable();
        if (skin.has("card-bg", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
            row.setBackground(skin.getDrawable("card-bg"));
        }
        row.pad(10);

        // Status indicator
        String stStr = order.status;
        Color stColor;
        if (isShipper) {
            if ("PENDING_PICKUP".equals(order.status)) {
                stStr = "PENDING PICKUP";
                stColor = SkinLoader.NEON_CYAN;
            } else {
                stStr = "DELIVERING";
                stColor = SkinLoader.NEON_LIME;
            }
        } else {
            stColor = SkinLoader.NEON_GOLD;
        }
        VisLabel st = new VisLabel(stStr, "small");
        st.setColor(stColor);
        row.add(st).colspan(isShipper ? 5 : 2).left().row();

        row.add(new Image(skin.getDrawable("icon-chest"))).size(24).padRight(5);
        VisLabel nameLab = new VisLabel(order.itemName, "small");
        nameLab.setEllipsis(true);
        row.add(nameLab).left().expandX();

        if (isShipper) {
            VisTable payT = new VisTable();
            payT.add(new Image(skin.getDrawable("icon-shop"))).size(16).padRight(3);
            VisLabel pL = new VisLabel("+" + order.reward, "small");
            pL.setColor(SkinLoader.NEON_GOLD);
            payT.add(pL);
            row.add(payT).width(80).right().padRight(10);

            VisImageButton gpsS = new VisImageButton(skin.getDrawable("icon-gps"));
            if (game.audioManager != null) game.audioManager.attachDefaultTo(gpsS);
            gpsS.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent e, float x, float y) {
                    if (onGpsRequested != null)
                        onGpsRequested.accept(new Vector3(order.shopX, order.shopY, order.shopZ));
                    statusLabel.setText("GPS: Shop");
                }
            });
            row.add(gpsS).size(32).padRight(2);

            VisImageButton gpsD = new VisImageButton(skin.getDrawable("icon-gps"));
            if (game.audioManager != null) game.audioManager.attachDefaultTo(gpsD);
            gpsD.setColor(SkinLoader.NEON_LIME);
            gpsD.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent e, float x, float y) {
                    if (onGpsRequested != null)
                        onGpsRequested.accept(new Vector3(order.destX, order.destY, order.destZ));
                    statusLabel.setText("GPS: " + order.destinationName);
                }
            });
            row.add(gpsD).size(32).padRight(8);

            VisTextButton stop = new VisTextButton("STOP");
            if (game.audioManager != null) game.audioManager.attachDefaultTo(stop);
            stop.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent e, float x, float y) {
                    if (onStopGpsRequested != null) onStopGpsRequested.run();
                    statusLabel.setText("Navigation Off.");
                }
            });
            row.add(stop).width(55).padRight(4);

            VisTextButton cancel = new VisTextButton("CANCEL");
            if (game.audioManager != null) game.audioManager.attachDefaultTo(cancel);
            cancel.setColor(SkinLoader.NEON_RED);
            cancel.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent e, float x, float y) {
                    networkManager.sendDeliveryCancelRequest(order.orderId);
                    statusLabel.setText("Cancelling...");
                }
            });
            row.add(cancel).width(70);

            // Smart Pickup/Handover
            if ("PENDING_PICKUP".equals(order.status) || "DELIVERING".equals(order.status)) {
                float dist = 999f;
                if (player != null) {
                    float tx = order.destX, ty = order.destY, tz = order.destZ;
                    if ("PENDING_PICKUP".equals(order.status)) {
                        tx = order.shopX; ty = order.shopY; tz = order.shopZ;
                    } else if ("DELIVERING".equals(order.status) && "PLAYER".equals(order.buyerType)) {
                        try {
                            int buyerDbId = Integer.parseInt(order.buyerRefId);
                            for (com.futurecity.game.entities.RemotePlayer rp : networkManager.getRemotePlayers().values()) {
                                if (rp.getDbUserId() == buyerDbId) {
                                    tx = rp.getPosition().x; ty = rp.getPosition().y; tz = rp.getPosition().z;
                                    break;
                                }
                            }
                        } catch (Exception ex) {}
                    }
                    dist = Vector3.dst(player.getPosition().x, player.getPosition().y, player.getPosition().z, tx, ty, tz);
                }

                if (dist < 200.0f) {
                    String btnTxt = "PENDING_PICKUP".equals(order.status) ? "PICK UP"
                            : ("PLAYER".equals(order.buyerType) ? "SIGN" : "DELIVER");
                    VisTextButton smartAction = new VisTextButton(btnTxt, "neon-button");
                    if (game.audioManager != null) game.audioManager.attachDefaultTo(smartAction);
                    smartAction.setColor(SkinLoader.NEON_CYAN);
                    smartAction.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent e, float x, float y) {
                            if ("PENDING_PICKUP".equals(order.status)) {
                                networkManager.sendDeliveryPickupRequest(order.orderId);
                            } else {
                                networkManager.sendDeliveryHandoverRequest(order.orderId);
                            }
                            statusLabel.setText("Processing...");
                        }
                    });
                    row.add(smartAction).width(90).padLeft(5);
                }
            }

            target.add(row).growX().pad(4).row();

            // Timer
            if (order.acceptedAtMs > 0 && order.timeoutMinutes > 0) {
                VisLabel timer = new VisLabel("", "small");
                timer.setColor(SkinLoader.NEON_GOLD);
                target.add(timer).left().padLeft(35).padBottom(8).row();
                long expiry = order.acceptedAtMs + (order.timeoutMinutes * 60000L);
                timerUpdates.add(() -> {
                    long rem = expiry - System.currentTimeMillis();
                    if (rem > 0) {
                        long m = (rem / 1000) / 60;
                        long s = (rem / 1000) % 60;
                        timer.setText(String.format("%02d:%02d", m, s));
                        if (rem < 60000) timer.setColor(SkinLoader.NEON_RED);
                        else if (rem < 180000) timer.setColor(SkinLoader.NEON_GOLD);
                        else timer.setColor(SkinLoader.NEON_LIME);
                    } else {
                        timer.setText("EXPIRED!");
                        timer.setColor(SkinLoader.NEON_RED);
                    }
                });
            }
        } else {
            row.add(st).right().padRight(15);
            if ("PENDING".equals(order.status)) {
                VisTextButton skip = new VisTextButton("CANCEL");
                if (game.audioManager != null) game.audioManager.attachDefaultTo(skip);
                skip.setColor(SkinLoader.NEON_RED);
                skip.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent e, float x, float y) {
                        networkManager.sendOrderCancelRequest(order.orderId);
                    }
                });
                row.add(skip).width(75);
            }
            target.add(row).growX().pad(3).row();
        }
    }

    // ═══════════════════════════════════════════════════════════
    // INTERACT TAB — Work Content Builders
    // ═══════════════════════════════════════════════════════════

    private void buildWorkContent() {
        InteractionUIType type = currentData.uiType;
        if (isShopType()) {
            VisTable tabs = new VisTable();
            VisTextButton catB = new VisTextButton("CATALOG", "neon-button");
            VisTextButton jobB = new VisTextButton("LOGISTICS", "neon-button");
            if (game.audioManager != null) {
                game.audioManager.attachDefaultTo(catB);
                game.audioManager.attachDefaultTo(jobB);
            }
            if (!isDeliveryTab) catB.setColor(SkinLoader.NEON_CYAN);
            else jobB.setColor(SkinLoader.NEON_CYAN);
            catB.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent e, float x, float y) {
                    isDeliveryTab = false;
                    refreshDashboard();
                }
            });
            jobB.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent e, float x, float y) {
                    isDeliveryTab = true;
                    refreshDashboard();
                }
            });
            tabs.add(catB).width(240).height(38).padRight(12);
            tabs.add(jobB).width(240).height(38);
            workField.add(tabs).padBottom(15).row();
            if (isDeliveryTab) buildDeliveryTable();
            else buildItemTable();
        } else if (type == InteractionUIType.PLAYER_INTERACT) {
            VisTable stats = new VisTable();
            if (skin.has("card-bg", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
                stats.setBackground(skin.getDrawable("card-bg"));
            }
            stats.pad(20);
            stats.add(new VisLabel("Citizen Name:", "small")).left().padRight(15);
            VisLabel n = new VisLabel(currentData.displayName, "small");
            n.setColor(SkinLoader.NEON_GOLD);
            stats.add(n).left().row();
            stats.add(new VisLabel("Citizen ID:", "small")).left().padRight(15);
            stats.add(new VisLabel(currentData.targetId, "small")).left().row();
            workField.add(stats).pad(20).center().row();
            VisLabel msg = new VisLabel(currentData.message != null ? currentData.message : "Standard interaction protocols active.");
            msg.setWrap(true);
            msg.setAlignment(Align.center);
            workField.add(msg).width(550).pad(10);
        } else if (type == InteractionUIType.NPC_DIALOGUE) {
            if (currentData.dialogueLines != null) {
                for (String line : currentData.dialogueLines) {
                    VisLabel l = new VisLabel(line);
                    l.setWrap(true);
                    l.setAlignment(Align.center);
                    workField.add(l).width(550).pad(10).row();
                }
            }
        } else {
            VisLabel l = new VisLabel(currentData.message);
            l.setWrap(true);
            l.setAlignment(Align.center);
            workField.add(l).width(550).pad(25);
        }
    }

    private void buildItemTable() {
        VisTable t = new VisTable();
        if (currentData.itemNames == null || currentData.itemNames.length == 0) {
            t.add(new VisLabel("No products available.", "small")).pad(40);
        } else {
            // Card grid for items
            int columns = 2;
            for (int i = 0; i < currentData.itemNames.length; i++) {
                final int id = currentData.itemIds[i];
                final String nm = currentData.itemNames[i];
                final int price = currentData.itemPrices[i];
                final int stock = currentData.itemStocks[i];

                VisTable card = new VisTable();
                if (skin.has("card-bg", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
                    card.setBackground(skin.getDrawable("card-bg"));
                }
                card.pad(12);

                VisLabel nameLabel = new VisLabel(nm, "small");
                nameLabel.setEllipsis(true);
                card.add(nameLabel).left().expandX().colspan(2).row();

                VisLabel prLabel = new VisLabel(price + " coins", "small");
                prLabel.setColor(SkinLoader.NEON_GOLD);
                card.add(prLabel).left().padTop(4);

                VisLabel stkLabel = new VisLabel("Stock: " + stock, "small");
                stkLabel.setColor(stock > 0 ? SkinLoader.NEON_LIME : SkinLoader.NEON_RED);
                card.add(stkLabel).right().padTop(4).row();

                String bTxt = "BUY";
                boolean canBuy = true;
                if (stock <= 0) { bTxt = "EMPTY"; canBuy = false; }
                else if (currentData.playerCoins < price) { bTxt = "LOW $"; canBuy = false; }

                VisTextButton b = new VisTextButton(bTxt);
                if (game.audioManager != null) game.audioManager.attachDefaultTo(b);
                if (!canBuy) b.setDisabled(true);
                b.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent e, float x, float y) {
                        if (!b.isDisabled())
                            networkManager.sendBuyRequest(currentTargetId, id);
                    }
                });
                card.add(b).colspan(2).growX().height(32).padTop(8);

                t.add(card).width(320).pad(6);
                if ((i + 1) % columns == 0) t.row();
            }
        }
        workField.add(t).growX();
    }

    private void buildDeliveryTable() {
        if (!currentData.isSubscribed) {
            workField.add(new VisLabel("Courier license required for this board.", "small")).pad(45);
            return;
        }
        VisTable t = new VisTable();
        if (currentData.orderIds == null || currentData.orderIds.length == 0) {
            t.add(new VisLabel("No logistical tasks available.", "small")).pad(40);
        } else {
            for (int i = 0; i < currentData.orderIds.length; i++) {
                final int oid = currentData.orderIds[i];
                VisTable row = new VisTable();
                if (skin.has("card-bg", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
                    row.setBackground(skin.getDrawable("card-bg"));
                }
                row.pad(10);

                VisLabel itemLabel = new VisLabel(currentData.orderItemNames[i], "small");
                row.add(itemLabel).left().expandX();

                VisLabel pay = new VisLabel("+$" + currentData.orderRewards[i], "small");
                pay.setColor(SkinLoader.NEON_LIME);
                row.add(pay).padRight(8);

                VisLabel dest = new VisLabel(currentData.orderDestinations[i], "small");
                dest.setColor(SkinLoader.TEXT_DIM);
                row.add(dest).padRight(8);

                VisTextButton a = new VisTextButton("ACCEPT", "neon-button");
                if (game.audioManager != null) game.audioManager.attachDefaultTo(a);
                a.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent e, float x, float y) {
                        networkManager.sendDeliveryAcceptRequest(oid);
                        hidePanel();
                    }
                });
                row.add(a).width(90).height(30);
                t.add(row).growX().padBottom(5).row();
            }
        }
        workField.add(t).growX();
    }

    private void buildActionFooter() {
        InteractionUIType type = currentData.uiType;
        if (type == InteractionUIType.PICKUP_ACTION) {
            VisTextButton pickupBtn = new VisTextButton("PICK UP", "neon-button");
            if (game.audioManager != null) game.audioManager.attachDefaultTo(pickupBtn);
            pickupBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    networkManager.sendDeliveryPickupRequest(currentData.activeOrderId);
                    hidePanel();
                }
            });
            actionField.add(pickupBtn).width(260).height(48).padRight(25);
        } else if (type == InteractionUIType.HANDOVER_ACTION || type == InteractionUIType.PLAYER_INTERACT) {
            String txt = (type == InteractionUIType.HANDOVER_ACTION && currentData.isPlayerTarget) ? "REQUEST SIGNATURE"
                    : "COMPLETE DELIVERY";
            if (type == InteractionUIType.PLAYER_INTERACT) txt = "GIVE ITEM";
            VisTextButton main = new VisTextButton(txt, "neon-button");
            if (game.audioManager != null) game.audioManager.attachDefaultTo(main);
            main.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent e, float x, float y) {
                    networkManager.sendDeliveryHandoverRequest(currentData.activeOrderId);
                    hidePanel();
                }
            });
            actionField.add(main).width(260).height(48).padRight(25);
        }
        VisTextButton close = new VisTextButton("EXIT", "neon-button");
        if (game.audioManager != null) game.audioManager.attachDefaultTo(close);
        close.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                hidePanel();
            }
        });
        actionField.add(close).width(160).height(48);
    }

    // ═══════════════════════════════════════════════════════════
    // UTILITY & DATA METHODS
    // ═══════════════════════════════════════════════════════════

    private boolean isShopType() {
        InteractionUIType t = currentData.uiType;
        return t == InteractionUIType.SHOP_GENERAL || t == InteractionUIType.SHOP_FOOD
                || t == InteractionUIType.SHOP_CLOTHES || t == InteractionUIType.PICKUP_ACTION;
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
        if (response.orders != null && notificationManager != null) {
            java.util.Set<Integer> validIds = new java.util.HashSet<>();
            for (DriverPendingOrdersResponse.OrderInfo info : response.orders) {
                validIds.add(info.orderId);
            }
            orderNotifications.removeIf(notif -> !validIds.contains(notif.orderId));
        }
        if (currentTab == DashboardTab.PHONE) refreshDashboard();
    }

    public void onBuySuccess(int coins, int itemId) {
        if (currentData != null) {
            currentData.playerCoins = coins;
            if (currentData.itemIds != null) {
                for (int i = 0; i < currentData.itemIds.length; i++) {
                    if (currentData.itemIds[i] == itemId) {
                        currentData.itemStocks[i]--;
                        break;
                    }
                }
            }
        }
        if (inventoryData != null) inventoryData.playerCoins = coins;
        statusLabel.setText("Transaction Success. Balance: " + coins);
        statusLabel.setColor(SkinLoader.NEON_LIME);
        refreshDashboard();
    }

    public void addNewOrder(NewOrderNotification notif) {
        this.orderNotifications.add(0, notif);
        if (orderNotifications.size() > 10) orderNotifications.remove(10);
        if (currentTab == DashboardTab.PHONE || (currentTab == DashboardTab.INTERACT && isDeliveryTab))
            refreshDashboard();
    }
}
