package com.futurecity.game.ui;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.widget.*;
import com.futurecity.shared.packets.request.ShopListRequest;
import com.futurecity.shared.packets.resonse.ShopListResponse;
import com.futurecity.shared.packets.resonse.InventoryResponse;
import com.futurecity.shared.packets.resonse.DriverPendingOrdersResponse;
import com.futurecity.game.managers.NetworkManager;

import java.util.List;

public class PhonePanel extends BasePanel {

    private VisTable mainScrollTable;
    private VisTable notificationTable;
    private VisTable shopListTable;
    private VisTable pendingOrdersTable;
    private VisTable courierOrdersTable;
    private VisTable myOrdersTable;
    private VisLabel statusLabel;
    private NetworkManager networkManager;
    private java.util.function.Consumer<com.badlogic.gdx.math.Vector3> onGpsRequested;
    private Runnable onStopGpsRequested;
    private java.util.List<com.futurecity.shared.packets.resonse.NewOrderNotification> orderNotifications = new java.util.ArrayList<>();

    public PhonePanel(Skin skin, NetworkManager networkManager) {
        super("Smartphone", skin);
        this.networkManager = networkManager;
        this.fullScreenMode = true; // Enable full screen mode
        populateContent();
    }

    @Override
    protected void populateContent() {
        contentTable.clear();

        // Header Section
        statusLabel = new VisLabel("Ready!");
        statusLabel.setColor(com.badlogic.gdx.graphics.Color.YELLOW);
        contentTable.add(statusLabel).pad(5).center().row();

        // Main Scroll Table
        mainScrollTable = new VisTable();
        mainScrollTable.top().left();
        
        VisScrollPane mainScroll = new VisScrollPane(mainScrollTable);
        mainScroll.setFadeScrollBars(false);
        mainScroll.setScrollingDisabled(false, false); // Enable both horizontal and vertical scrolling
        
        // Center content in full screen: Limit width to look like a phone
        contentTable.add(mainScroll).expand().top().width(450).pad(20).row();

        // Footer Section
        VisTextButton closeBtn = new VisTextButton("Power Off");
        closeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hidePanel();
            }
        });
        contentTable.add(closeBtn).pad(10).fillX();

        rebuildScrollContent();
    }

    private void rebuildScrollContent() {
        mainScrollTable.clear();
        
        // 1. New Order Notifications
        addSectionHeader("NEW ORDER NOTIFICATIONS");
        notificationTable = new VisTable();
        mainScrollTable.add(notificationTable).expandX().fillX().pad(5).row();

        // 2. Shopping Apps
        addSectionHeader("SHOPPING APPS");
        shopListTable = new VisTable();
        mainScrollTable.add(shopListTable).expandX().fillX().pad(5).row();
        
        // 3. Pending Orders (Driver)
        addSectionHeader("PENDING ORDERS (DRIVER)");
        pendingOrdersTable = new VisTable();
        mainScrollTable.add(pendingOrdersTable).expandX().fillX().pad(5).row();
        
        // 4. Active Deliveries (Shipper)
        addSectionHeader("ACTIVE DELIVERIES (SHIPPER)");
        courierOrdersTable = new VisTable();
        mainScrollTable.add(courierOrdersTable).expandX().fillX().pad(5).row();
        
        // 5. Order History (Buyer)
        addSectionHeader("ORDER HISTORY (BUYER)");
        myOrdersTable = new VisTable();
        mainScrollTable.add(myOrdersTable).expandX().fillX().pad(5).row();
        
        refreshNotifications();
    }

    private void addSectionHeader(String title) {
        VisTable headerTable = new VisTable();
        VisLabel label = new VisLabel("--- " + title.toUpperCase() + " ---");
        label.setColor(com.badlogic.gdx.graphics.Color.CYAN); // Striking neon color
        headerTable.add(label).center().expandX();
        mainScrollTable.add(headerTable).expandX().fillX().padTop(15).row();
    }

    public void setOnGpsRequested(java.util.function.Consumer<com.badlogic.gdx.math.Vector3> callback) {
        this.onGpsRequested = callback;
    }

    public void setOnStopGpsRequested(Runnable callback) {
        this.onStopGpsRequested = callback;
    }

    public void displayShops(List<ShopListResponse.ShopInfo> shops) {
        System.out.println("[CLIENT-UI] displayShops called with " + (shops == null ? "null" : shops.size()) + " shops.");
        if (shopListTable == null) {
            System.err.println("[CLIENT-UI] ERROR: shopListTable is NULL");
            return;
        }
        shopListTable.clearChildren();
        if (shops == null || shops.isEmpty()) {
            System.out.println("[CLIENT-UI] Shops list is empty.");
            shopListTable.add(new VisLabel("No shops available.")).pad(10).left();
            return;
        }
        for (ShopListResponse.ShopInfo shop : shops) {
            System.out.println("[CLIENT-UI] Processing shop: " + shop.shopName + " (" + shop.items.size() + " items)");
            VisLabel shopNameLabel = new VisLabel(shop.shopName);
            shopNameLabel.setColor(com.badlogic.gdx.graphics.Color.CYAN);
            shopListTable.add(shopNameLabel).left().padTop(10).row();
            
            for (ShopListResponse.ShopItemInfo item : shop.items) {
                VisTable row = new VisTable();
                VisTable infoTable = new VisTable();
                infoTable.add(new VisLabel(item.itemName)).left().row();
                
                int estShip = 20 + (int)(item.price * 0.1f);
                VisLabel priceLabel = new VisLabel("Price: " + item.price + " + Ship: ~" + estShip);
                priceLabel.setFontScale(0.85f);
                priceLabel.setColor(com.badlogic.gdx.graphics.Color.LIGHT_GRAY);
                infoTable.add(priceLabel).left();
                
                row.add(infoTable).left().expandX();
                
                VisTable btnTable = new VisTable();
                btnTable.add(new com.badlogic.gdx.scenes.scene2d.ui.Image(getSkin().getDrawable("icon-shop"))).size(24).padRight(5);
                
                VisTextButton orderBtn = new VisTextButton("ORDER");
                orderBtn.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        sendOrder(shop.shopId, item.itemId);
                        hidePanel();
                    }
                });
                btnTable.add(orderBtn).width(60);
                row.add(btnTable).width(100).padLeft(5);
                shopListTable.add(row).fillX().padBottom(8).row();
            }
        }
        shopListTable.invalidateHierarchy();
        mainScrollTable.invalidateHierarchy();
        mainScrollTable.pack();
        System.out.println("[CLIENT-UI] displayShops finished and layout invalidated.");
    }

    // Pending orders (Shipper) from DriverPendingOrdersResponse
    public void updatePendingOrders(List<DriverPendingOrdersResponse.OrderInfo> orders) {
        System.out.println("[CLIENT-UI] updatePendingOrders called with " + (orders == null ? "null" : orders.size()) + " orders.");
        if (pendingOrdersTable == null) return;
        pendingOrdersTable.clear();
        if (orders == null || orders.isEmpty()) {
            pendingOrdersTable.add(new VisLabel("No pending orders.")).left().pad(10);
            return;
        }
        for (DriverPendingOrdersResponse.OrderInfo order : orders) {
            VisTable row = new VisTable();
            VisLabel desc = new VisLabel(order.itemName + " @ " + order.shopName);
            desc.setEllipsis(true);
            row.add(desc).left().expandX();
            row.add(new VisLabel("+$" + order.reward)).padRight(5);
            
            VisImageButton gpsShopBtn = new VisImageButton(getSkin().getDrawable("icon-gps"));
            VisImageButton gpsDestBtn = new VisImageButton(getSkin().getDrawable("icon-gps"));
            gpsDestBtn.setColor(com.badlogic.gdx.graphics.Color.GREEN); // Destination is green
            
            VisTextButton acceptBtn = new VisTextButton("OK");
            
            gpsShopBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (onGpsRequested != null) {
                        onGpsRequested.accept(new com.badlogic.gdx.math.Vector3(order.shopX, order.shopY, order.shopZ));
                        statusLabel.setText("GPS: Shop " + order.shopName);
                    }
                }
            });
            gpsDestBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (onGpsRequested != null) {
                        onGpsRequested.accept(new com.badlogic.gdx.math.Vector3(order.destX, order.destY, order.destZ));
                        statusLabel.setText("GPS: Dest " + order.destinationName);
                    }
                }
            });
            acceptBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    System.out.println("[CLIENT-UI] Shipper accepts order: " + order.orderId);
                    networkManager.sendDeliveryAcceptRequest(order.orderId);
                    hidePanel();
                }
            });
            row.add(gpsShopBtn).size(32).padRight(2);
            row.add(gpsDestBtn).size(32).padRight(5);
            row.add(acceptBtn).width(60);
            pendingOrdersTable.add(row).fillX().padBottom(4).row();
        }
    }

    // Active deliveries + My orders from InventoryResponse
    public void updateOrdersFromInventory(InventoryResponse response) {
        if (courierOrdersTable == null || myOrdersTable == null) return;
        
        // courier active deliveries
        courierOrdersTable.clear();
        if (response.activeDeliveries != null && !response.activeDeliveries.isEmpty()) {
            for (InventoryResponse.OrderInfo del : response.activeDeliveries) {
                VisTable row = new VisTable();
                VisLabel nameLabel = new VisLabel(del.itemName);
                nameLabel.setEllipsis(true);
                row.add(nameLabel).left().width(180);
                
                VisLabel status = new VisLabel(del.status);
                status.setColor(com.badlogic.gdx.graphics.Color.ORANGE);
                row.add(status).width(80).padLeft(5);
                
                // SHOP GPS button
                VisImageButton shopGpsBtn = new VisImageButton(getSkin().getDrawable("icon-gps"));
                shopGpsBtn.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        if (onGpsRequested != null) {
                            onGpsRequested.accept(new com.badlogic.gdx.math.Vector3(del.shopX, del.shopY, del.shopZ));
                            statusLabel.setText("GPS -> Shop");
                        }
                    }
                });
                row.add(shopGpsBtn).size(32).padLeft(5);

                // DEST GPS button
                VisImageButton destGpsBtn = new VisImageButton(getSkin().getDrawable("icon-gps"));
                destGpsBtn.setColor(com.badlogic.gdx.graphics.Color.GREEN);
                destGpsBtn.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        if (onGpsRequested != null) {
                            onGpsRequested.accept(new com.badlogic.gdx.math.Vector3(del.destX, del.destY, del.destZ));
                            statusLabel.setText("GPS -> " + del.destinationName);
                        }
                    }
                });
                row.add(destGpsBtn).size(32).padLeft(2);

                // STOP GPS button
                VisTextButton stopGpsBtn = new VisTextButton("STOP");
                stopGpsBtn.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        if (onStopGpsRequested != null) {
                            onStopGpsRequested.run();
                            statusLabel.setText("Navigation stopped.");
                        }
                    }
                });
                row.add(stopGpsBtn).width(45).padLeft(5);

                // CANCEL DELIVERY button
                VisTextButton cancelBtn = new VisTextButton("CANCEL");
                cancelBtn.setColor(com.badlogic.gdx.graphics.Color.RED);
                cancelBtn.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        System.out.println("********** [CLIENT-UI] CLICK CANCEL DELIVERY (SHIPPER): " + del.orderId + " **********");
                        networkManager.sendDeliveryCancelRequest(del.orderId);
                        statusLabel.setText("Cancelling order #" + del.orderId);
                    }
                });
                row.add(cancelBtn).width(45).padLeft(5);
                courierOrdersTable.add(row).expandX().fillX().padBottom(4).row();
            }
        } else {
            courierOrdersTable.add(new VisLabel("No active deliveries.")).left().pad(10);
        }

        // buyer orders
        myOrdersTable.clear();
        if (response.myOrders != null && !response.myOrders.isEmpty()) {
            for (InventoryResponse.OrderInfo ord : response.myOrders) {
                VisTable row = new VisTable();
                VisLabel nameLabel = new VisLabel(ord.itemName);
                nameLabel.setEllipsis(true);
                row.add(nameLabel).left().width(200);
                
                VisLabel status = new VisLabel(ord.status);
                status.setColor("PENDING".equals(ord.status) ? com.badlogic.gdx.graphics.Color.YELLOW : com.badlogic.gdx.graphics.Color.GREEN);
                row.add(status).width(100).padLeft(5);

                if ("PENDING".equals(ord.status)) {
                    VisTextButton cancelBtn = new VisTextButton("CANCEL");
                    cancelBtn.setColor(com.badlogic.gdx.graphics.Color.RED);
                    cancelBtn.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            System.out.println("********** [CLIENT-UI] CLICK CANCEL ORDER: " + ord.orderId + " **********");
                            networkManager.sendOrderCancelRequest(ord.orderId);
                            statusLabel.setText("Requesting cancellation for #" + ord.orderId);
                        }
                    });
                    row.add(cancelBtn).width(50).padLeft(5);
                } else {
                    // Placeholder to maintain layout
                    row.add(new VisLabel("")).width(50);
                }

                myOrdersTable.add(row).expandX().fillX().padBottom(4).row();
            }
        } else {
            myOrdersTable.add(new VisLabel("No orders placed.")).left().pad(10);
        }

        // Force layout recalculation
        courierOrdersTable.invalidateHierarchy();
        myOrdersTable.invalidateHierarchy();
        mainScrollTable.invalidateHierarchy();
        mainScrollTable.pack(); // Force actual size
        
        statusLabel.setText("Smartphone - Ready");
    }

    public void addNewOrderNotification(com.futurecity.shared.packets.resonse.NewOrderNotification notif) {
        if (notif == null) return;
        // avoid duplicates
        for (com.futurecity.shared.packets.resonse.NewOrderNotification n : orderNotifications) {
            if (n.orderId == notif.orderId) return;
        }
        orderNotifications.add(0, notif); // Add to the beginning of the list
        if (orderNotifications.size() > 10) orderNotifications.remove(orderNotifications.size() - 1);
        
        com.badlogic.gdx.Gdx.app.postRunnable(this::refreshNotifications);
    }

    private void refreshNotifications() {
        if (notificationTable == null) return;
        notificationTable.clear();
        if (orderNotifications.isEmpty()) {
            notificationTable.add(new VisLabel("No new notifications.")).left().pad(10);
            return;
        }

        for (com.futurecity.shared.packets.resonse.NewOrderNotification notif : orderNotifications) {
            VisTable row = new VisTable();
            VisLabel label = new VisLabel(notif.itemName + " @ " + notif.shopName);
            label.setColor(com.badlogic.gdx.graphics.Color.GOLD);
            row.add(label).left().expandX();
            
            VisTextButton viewBtn = new VisTextButton("VIEW");
            viewBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    // When clicking view, navigate to the order reception tab or do something else
                    statusLabel.setText("New order from " + notif.shopName);
                }
            });
            row.add(viewBtn).width(60);
            notificationTable.add(row).fillX().padBottom(5).row();
        }
    }

    private void sendOrder(String shopId, int itemId) {
        networkManager.sendOrderRequest(shopId, itemId);
    }

    @Override
    public void showPanel() {
        super.showPanel();
        networkManager.sendPacket(new ShopListRequest());
        networkManager.sendInventoryRequest();
        networkManager.sendPacket(new com.futurecity.shared.packets.request.DriverPendingOrdersRequest());
    }
}


