package com.futurecity.game.ui;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.kotcrab.vis.ui.widget.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.graphics.Color;
import com.futurecity.game.managers.NetworkManager;
import com.futurecity.shared.packets.resonse.InteractionResponse;
import com.futurecity.shared.packets.resonse.NewOrderNotification;
import java.util.Arrays;

/**
 * Panel cửa hàng: hiện danh sách sản phẩm + stock + nút Mua.
 * Mở bằng bấm F tại shop, dữ liệu nhận từ InteractionResponse.
 */
public class ShopPanel extends BasePanel {
    private NetworkManager networkManager;
    private String currentShopId;
    private int playerCoins;

    // Dữ liệu items hiện tại
    private int[] itemIds;
    private String[] itemNames;
    private int[] itemPrices;
    private int[] itemStocks;

    // Dữ liệu delivery orders
    private int[] orderIds;
    private String[] orderItemNames;
    private int[] orderRewards;
    private String[] orderDestinations;

    private VisLabel coinLabel;
    private VisTable itemListTable;
    private VisTable deliveryListTable;
    private VisLabel statusLabel;

    private boolean isSubscribed;
    private VisTextButton subscribeBtn;

    public ShopPanel(Skin skin, NetworkManager networkManager) {
        super("Shop", skin);
        this.networkManager = networkManager;
        setSize(500, 400);
        populateContent();
    }

    @Override
    protected void populateContent() {
        contentTable.clear();

        // --- Header: Coin display + Subscribe Btn ---
        VisTable headerTable = new VisTable();
        coinLabel = new VisLabel("🪙 0 Coins");
        coinLabel.setFontScale(1.1f);
        coinLabel.setColor(Color.GOLD);

        subscribeBtn = new VisTextButton("Register Courier");
        subscribeBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                networkManager.sendShopSubscribeRequest(currentShopId, !isSubscribed);
                // Optimistic UI update
                isSubscribed = !isSubscribed;
                updateSubscribeButton();
            }
        });

        headerTable.add(coinLabel).left().expandX();
        headerTable.add(subscribeBtn).right();
        contentTable.add(headerTable).growX().padBottom(10).row();

        // --- Tab buttons ---
        VisTable tabBar = new VisTable();
        VisTextButton buyTab = new VisTextButton("Buy Items");
        VisTextButton deliveryTab = new VisTextButton("Join Delivery");

        buyTab.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showBuyTab();
            }
        });
        deliveryTab.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showDeliveryTab();
            }
        });

        tabBar.add(buyTab).pad(5).width(150);
        tabBar.add(deliveryTab).pad(5).width(150);
        contentTable.add(tabBar).row();

        // --- Scroll area for items ---
        itemListTable = new VisTable();
        VisScrollPane scrollPane = new VisScrollPane(itemListTable);
        scrollPane.setFadeScrollBars(false);
        contentTable.add(scrollPane).expand().fill().pad(5).row();

        // --- Delivery list (hidden by default) ---
        deliveryListTable = new VisTable();

        // --- Status label ---
        statusLabel = new VisLabel("");
        statusLabel.setColor(Color.LIGHT_GRAY);
        contentTable.add(statusLabel).padTop(5).row();
    }

    /**
     * Updates data from InteractionResponse sent by server.
     */
    public void updateFromResponse(InteractionResponse response) {
        this.currentShopId = response.targetId;
        this.playerCoins = response.playerCoins;
        this.isSubscribed = response.isSubscribed;
        this.itemIds = response.itemIds;
        this.itemNames = response.itemNames;
        this.itemPrices = response.itemPrices;
        this.itemStocks = response.itemStocks;
        this.orderIds = response.orderIds;
        this.orderItemNames = response.orderItemNames;
        this.orderRewards = response.orderRewards;
        this.orderDestinations = response.orderDestinations;

        coinLabel.setText("🪙 " + playerCoins + " Coins");
        updateSubscribeButton();
        showBuyTab();
    }

    /**
     * Buy Tab: shows name, price, stock + [Buy] button.
     */
    private void showBuyTab() {
        itemListTable.clear();
        statusLabel.setText("");

        // Header
        itemListTable.add(new VisLabel("Name")).width(150).pad(3);
        itemListTable.add(new VisLabel("Price")).width(70).pad(3);
        itemListTable.add(new VisLabel("Stock")).width(50).pad(3);
        itemListTable.add(new VisLabel("")).width(80).pad(3);
        itemListTable.row();

        if (itemNames == null || itemNames.length == 0) {
            itemListTable.add(new VisLabel("No products available")).colspan(4).pad(20);
            return;
        }

        for (int i = 0; i < itemNames.length; i++) {
            final int index = i;
            final int itemId = itemIds[i];

            VisLabel nameLabel = new VisLabel(itemNames[i]);
            VisLabel priceLabel = new VisLabel(itemPrices[i] + "");
            priceLabel.setColor(Color.GOLD);
            VisLabel stockLabel = new VisLabel(itemStocks[i] + "");
            stockLabel.setColor(itemStocks[i] > 0 ? Color.GREEN : Color.RED);

            VisTextButton buyBtn = new VisTextButton("Buy");
            if (itemStocks[i] <= 0) {
                buyBtn.setDisabled(true);
                buyBtn.setText("Empty");
            } else if (playerCoins < itemPrices[i]) {
                buyBtn.setDisabled(true);
                buyBtn.setText("Low $");
            }

            buyBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (!buyBtn.isDisabled()) {
                        networkManager.sendBuyRequest(currentShopId, itemId);
                        statusLabel.setText("Buying " + itemNames[index] + "...");
                    }
                }
            });

            itemListTable.add(nameLabel).width(150).pad(3);
            itemListTable.add(priceLabel).width(70).pad(3);
            itemListTable.add(stockLabel).width(50).pad(3);
            itemListTable.add(buyBtn).width(80).pad(3);
            itemListTable.row();
        }
    }

    /**
     * Delivery Tab: shows PENDING orders + [Accept] button.
     */
    private void showDeliveryTab() {
        itemListTable.clear();
        statusLabel.setText("");

        // Check subscription
        if (!isSubscribed) {
            itemListTable.add(new VisLabel("🚫 You are not registered as a Courier!")).pad(20).row();
            itemListTable.add(new VisLabel("Please click 'Register Courier'")).pad(10).row();
            itemListTable.add(new VisLabel("at the top to receive delivery orders.")).pad(10).row();

            VisTextButton subNowBtn = new VisTextButton("Register Now");
            subNowBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    networkManager.sendShopSubscribeRequest(currentShopId, true);
                    isSubscribed = true;
                    updateSubscribeButton();
                    showDeliveryTab(); // Refresh to show orders
                }
            });
            itemListTable.add(subNowBtn).padTop(20).width(150);
            return;
        }

        // Header
        itemListTable.add(new VisLabel("Order")).width(150).pad(3);
        itemListTable.add(new VisLabel("Reward")).width(70).pad(3);
        itemListTable.add(new VisLabel("To")).width(100).pad(3);
        itemListTable.add(new VisLabel("")).width(80).pad(3);
        itemListTable.row();

        if (orderIds == null || orderIds.length == 0) {
            itemListTable.add(new VisLabel("No orders pending")).colspan(4).pad(20);
            return;
        }

        for (int i = 0; i < orderIds.length; i++) {
            final int orderId = orderIds[i];
            final int index = i;

            VisLabel nameLabel = new VisLabel(orderItemNames[i]);
            VisLabel rewardLabel = new VisLabel("+" + orderRewards[i]);
            rewardLabel.setColor(Color.GREEN);
            VisLabel destLabel = new VisLabel(orderDestinations[i]);

            VisTextButton acceptBtn = new VisTextButton("Accept");
            acceptBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    networkManager.sendDeliveryAcceptRequest(orderId);
                    statusLabel.setText("Accepting order...");
                }
            });

            itemListTable.add(nameLabel).width(150).pad(3);
            itemListTable.add(rewardLabel).width(70).pad(3);
            itemListTable.add(destLabel).width(100).pad(3);
            itemListTable.add(acceptBtn).width(80).pad(3);
            itemListTable.row();
        }
    }

    private void updateSubscribeButton() {
        if (isSubscribed) {
            subscribeBtn.setText("Already a Courier 🛵");
            subscribeBtn.setColor(Color.GREEN);
        } else {
            subscribeBtn.setText("Register Courier");
            subscribeBtn.setColor(Color.WHITE);
        }
    }

    /**
     * Cập nhật sau khi mua thành công.
     */
    public void onBuySuccess(int remainingCoins, int itemId) {
        this.playerCoins = remainingCoins;
        coinLabel.setText("💰 " + remainingCoins + " Coins");
        statusLabel.setText("✅ Purchase successful!");
        statusLabel.setColor(Color.GREEN);
        // Giảm stock local
        if (itemIds != null) {
            for (int i = 0; i < itemIds.length; i++) {
                if (itemIds[i] == itemId) {
                    itemStocks[i]--;
                    break;
                }
            }
        }
        showBuyTab(); // Refresh display
    }

    public void onBuyFailed(String message) {
        statusLabel.setText("❌ " + message);
        statusLabel.setColor(Color.RED);
    }

    public String getCurrentShopId() {
        return currentShopId;
    }

    public void addNewOrderToSubscriptionList(NewOrderNotification notif) {
        if (notif == null || currentShopId == null || !currentShopId.equals(notif.shopId)) {
            return;
        }

        // Thêm vào mảng (append)
        if (orderIds == null) {
            orderIds = new int[] { notif.orderId };
            orderItemNames = new String[] { notif.itemName };
            orderRewards = new int[] { notif.reward };
            orderDestinations = new String[] { notif.destination };
        } else {
            // Kiểm tra xem đã tồn tại chưa
            for (int id : orderIds) {
                if (id == notif.orderId)
                    return;
            }

            int n = orderIds.length;
            orderIds = Arrays.copyOf(orderIds, n + 1);
            orderIds[n] = notif.orderId;

            orderItemNames = Arrays.copyOf(orderItemNames, n + 1);
            orderItemNames[n] = notif.itemName;

            orderRewards = Arrays.copyOf(orderRewards, n + 1);
            orderRewards[n] = notif.reward;

            orderDestinations = Arrays.copyOf(orderDestinations, n + 1);
            orderDestinations[n] = notif.destination;
        }

        // Nếu đang mở tab delivery thì refresh UI
        if (isVisible() && statusLabel != null) {
            // Đơn giản là gọi lại showDeliveryTab nếu ta biết cách xác
            // định đang ở tab nào.
            // Ở đây populateContent() xóa hết và tạo lại, hoặc ta có thể
            // check title của
            // tab.
            // Tạm thời cứ populate lại nếu đang hiện.
            com.badlogic.gdx.Gdx.app.postRunnable(this::showDeliveryTab);
        }

        // Nếu đang mở tab delivery thì refresh UI
        if (isVisible() && statusLabel != null) {
            // Đơn giản là gọi lại showDeliveryTab nếu ta biết cách xác
            // định đang ở tab nào.
            // Ở đây populateContent() xóa hết và tạo lại, hoặc ta có thể
            // check title của
            // tab.
            // Tạm thời cứ populate lại nếu đang hiện.
            com.badlogic.gdx.Gdx.app.postRunnable(this::showDeliveryTab);
        }
    }
}
