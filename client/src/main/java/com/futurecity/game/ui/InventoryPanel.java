package com.futurecity.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.kotcrab.vis.ui.widget.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.futurecity.game.managers.NetworkManager;
import com.futurecity.shared.packets.resonse.InventoryResponse;

/**
 * Player's inventory: shows Coins, Items, active deliveries.
 * Open by pressing H.
 */
public class InventoryPanel extends BasePanel {
    private NetworkManager networkManager;

    private VisLabel coinLabel;
    private VisTable itemListTable;
    private VisTable deliversTable;
    private VisTable buyersTable;
    private VisLabel statusLabel;

    public InventoryPanel(Skin skin, NetworkManager networkManager) {
        super("INVENTORY", skin);
        this.networkManager = networkManager;
        this.fullScreenMode = true; // Enable full screen mode
        populateContent();
    }


    @Override
    protected void populateContent() {
        contentTable.clear();

        // --- Header with Coins Icon ---
        VisTable coinRow = new VisTable();
        coinRow.add(new com.badlogic.gdx.scenes.scene2d.ui.Image(getSkin().getDrawable("icon-shop"))).size(24).padRight(5);
        coinLabel = new VisLabel("0 Coins");
        coinLabel.setColor(com.badlogic.gdx.graphics.Color.GOLD);
        coinRow.add(coinLabel);
        contentTable.add(coinRow).left().padBottom(15).row();

        // --- Items list ---
        VisLabel itemsHeader = new VisLabel("--- OWNED ITEMS ---");
        itemsHeader.setColor(com.badlogic.gdx.graphics.Color.CYAN);
        contentTable.add(itemsHeader).padBottom(5).row();

        itemListTable = new VisTable();
        VisScrollPane scrollPane = new VisScrollPane(itemListTable);
        scrollPane.setFadeScrollBars(false);
        // Center and limit width for multi-column layout
        contentTable.add(scrollPane).expand().top().width(600).pad(10).row();

        // --- Delivering (as Shipper) ---
        VisLabel delHeader = new VisLabel("--- ACTIVE DELIVERIES ---");
        delHeader.setColor(com.badlogic.gdx.graphics.Color.CYAN);
        contentTable.add(delHeader).padTop(10).padBottom(5).row();
        deliversTable = new VisTable();
        contentTable.add(deliversTable).fillX().pad(5).row();

        // --- Buying (as Buyer) ---
        VisLabel buyHeader = new VisLabel("--- MY ORDERS ---");
        buyHeader.setColor(com.badlogic.gdx.graphics.Color.CYAN);
        contentTable.add(buyHeader).padTop(10).padBottom(5).row();
        buyersTable = new VisTable();
        contentTable.add(buyersTable).fillX().pad(5).row();

        // --- Status ---
        statusLabel = new VisLabel("");
        statusLabel.setColor(com.badlogic.gdx.graphics.Color.LIGHT_GRAY);
        contentTable.add(statusLabel).padTop(5).row();
    }

    private java.util.List<Runnable> timerUpdates = new java.util.ArrayList<>();

    @Override
    public void act(float delta) {
        super.act(delta);
        for (Runnable r : timerUpdates) {
            r.run();
        }
    }

    /**
     * Updates Coins separately without refreshing full content.
     */
    public void updateCoins(int coins) {
        if (coinLabel != null) {
            coinLabel.setText(coins + " Coins");
        }
    }

    /**
     * Updates data from InventoryResponse.
     */
    public void updateFromResponse(InventoryResponse response) {
        coinLabel.setText(response.playerCoins + " Coins");
        timerUpdates.clear();

        // Items - multi-column (3 cols)
        itemListTable.clear();
        if (response.itemNames != null && response.itemNames.length > 0) {
            int columns = 3; 
            for (int i = 0; i < response.itemNames.length; i++) {
                VisTable itemCard = new VisTable();
                itemCard.setBackground(getSkin().getDrawable("textfield-bg")); 
                itemCard.pad(8);

                itemCard.add(new com.badlogic.gdx.scenes.scene2d.ui.Image(getSkin().getDrawable("icon-chest"))).size(40).row();
                VisLabel nameLabel = new VisLabel(response.itemNames[i]);
                nameLabel.setEllipsis(true);
                itemCard.add(nameLabel).width(120).padTop(5).row();
                
                VisLabel qtyLabel = new VisLabel("Quantity: " + response.quantities[i]);
                qtyLabel.setFontScale(0.85f);
                qtyLabel.setColor(Color.LIGHT_GRAY);
                itemCard.add(qtyLabel).padTop(2);

                itemListTable.add(itemCard).pad(10).width(150);
                if ((i + 1) % columns == 0) itemListTable.row();
            }
        } else {
            itemListTable.add(new VisLabel("Your inventory is empty.")).pad(20);
        }

        // Active deliveries (Shipper)
        deliversTable.clear();
        if (response.activeDeliveries != null && !response.activeDeliveries.isEmpty()) {
            for (InventoryResponse.OrderInfo del : response.activeDeliveries) {
                VisTable row = new VisTable();

                String stateStr = "PENDING_PICKUP".equals(del.status) ? "[PENDING PICKUP]" : "[DELIVERING]";
                VisLabel stateLabel = new VisLabel(stateStr);
                stateLabel.setColor("PENDING_PICKUP".equals(del.status) ? Color.CYAN : Color.GREEN);
                row.add(stateLabel).colspan(2).left().row();

                VisTable itemRow = new VisTable();
                itemRow.add(new com.badlogic.gdx.scenes.scene2d.ui.Image(getSkin().getDrawable("icon-chest"))).size(20).padRight(5);
                itemRow.add(new VisLabel(del.itemName)).expandX().left();
                row.add(itemRow).left().expandX();

                VisTable rewardRow = new VisTable();
                rewardRow.add(new com.badlogic.gdx.scenes.scene2d.ui.Image(getSkin().getDrawable("icon-shop"))).size(18).padRight(2);
                rewardRow.add(new VisLabel("+" + del.reward));
                row.add(rewardRow).right().padLeft(10);
                row.row();

                VisTable destRow = new VisTable();
                destRow.add(new com.badlogic.gdx.scenes.scene2d.ui.Image(getSkin().getDrawable("icon-gps"))).size(18).padRight(5);
                destRow.add(new VisLabel(del.destinationName)).expandX().left();
                row.add(destRow).colspan(2).left().row();

                // Countdown Timer
                if (del.acceptedAtMs > 0 && del.timeoutMinutes > 0) {
                    VisLabel timerLabel = new VisLabel("");
                    timerLabel.setColor(Color.ORANGE);
                    row.add(timerLabel).colspan(2).left().padBottom(5).row();

                    long expireTimeMs = del.acceptedAtMs + (del.timeoutMinutes * 60000L);
                    timerUpdates.add(() -> {
                        long now = System.currentTimeMillis();
                        long remaining = expireTimeMs - now;
                        if (remaining > 0) {
                            long secs = remaining / 1000;
                            long m = secs / 60;
                            long s = secs % 60;
                            timerLabel.setText(String.format("Time left: %02d:%02d", m, s));
                            if (remaining < 60000L) timerLabel.setColor(Color.RED);
                        } else {
                            timerLabel.setText("EXPIRED!");
                            timerLabel.setColor(Color.RED);
                        }
                    });
                }

                VisTable btnTable = new VisTable();
                
                VisTextButton cancelBtn = new VisTextButton("CANCEL ORDER");
                cancelBtn.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        networkManager.sendDeliveryCancelRequest(del.orderId);
                        statusLabel.setText("Cancelling order #" + del.orderId);
                    }
                });
                btnTable.add(cancelBtn).padRight(5);
                row.add(btnTable).colspan(2).right().padTop(2);

                deliversTable.add(row).fillX().pad(5).row();
            }
        } else {
            deliversTable.add(new VisLabel("None")).pad(5);
        }

        // My Orders (Buyer)
        buyersTable.clear();
        if (response.myOrders != null && !response.myOrders.isEmpty()) {
            for (InventoryResponse.OrderInfo ord : response.myOrders) {
                VisTable row = new VisTable();
                row.add(new com.badlogic.gdx.scenes.scene2d.ui.Image(getSkin().getDrawable("icon-check"))).size(18).padRight(5);
                row.add(new VisLabel(ord.itemName)).left().expandX();
                VisLabel st = new VisLabel(ord.status);
                st.setColor(com.badlogic.gdx.graphics.Color.YELLOW);
                row.add(st).right().padLeft(10);
                buyersTable.add(row).fillX().pad(5).row();
            }
        } else {
            buyersTable.add(new VisLabel("None")).pad(5);
        }
    }

    @Override
    public void showPanel() {
        super.showPanel();
        // Fetch data when opening
        networkManager.sendInventoryRequest();
    }
}


