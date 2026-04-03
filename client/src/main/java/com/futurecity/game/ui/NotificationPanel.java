package com.futurecity.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.futurecity.game.entities.NotificationEntry;
import com.futurecity.game.managers.NotificationManager;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Premium Notification History Panel.
 * Features: Type icons, slide-in rows, mark-read button, timestamp display.
 */
public class NotificationPanel extends BasePanel {
    private final com.futurecity.game.core.Main game;
    private NotificationManager manager;
    private VisTable listTable;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    public NotificationPanel(Skin skin, com.futurecity.game.core.Main game, NotificationManager manager) {
        super("NOTIFICATION LOG", skin);
        this.game = game;
        this.manager = manager;
        setSize(550, 550);
        populateContent();

        manager.addListener(new NotificationManager.NotificationListener() {
            @Override
            public void onNewNotification(NotificationEntry entry) {
                refreshList();
            }

            @Override
            public void onNotificationsRead() {
                refreshList();
            }
        });
    }

    @Override
    protected void populateContent() {
        contentTable.clear();
        contentTable.top().pad(10);

        // Header
        VisTable header = new VisTable();
        VisLabel headerTitle = new VisLabel("SIGNAL INTERCEPTS", "small");
        headerTitle.setColor(SkinLoader.NEON_CYAN);
        header.add(headerTitle).left().expandX();

        VisTextButton markReadBtn = new VisTextButton("MARK ALL READ", "neon-button");
        if (game.audioManager != null) {
            game.audioManager.attachDefaultTo(markReadBtn);
        }
        markReadBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                manager.markAllAsRead();
            }
        });
        header.add(markReadBtn).width(160).height(30).right();
        contentTable.add(header).growX().pad(5).row();

        // Separator
        if (skin.has("separator-neon", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
            contentTable.add(new Image(skin.getDrawable("separator-neon"))).height(2).growX().padBottom(5).row();
        }

        // Scroll area
        listTable = new VisTable();
        listTable.top();
        VisScrollPane scrollPane = new VisScrollPane(listTable);
        scrollPane.setFadeScrollBars(false);
        enableAutoScrollFocus(scrollPane);
        contentTable.add(scrollPane).expand().fill().pad(5).row();

        // Footer
        VisTable footer = new VisTable();
        VisTextButton clearBtn = new VisTextButton("PURGE ALL");
        clearBtn.setColor(SkinLoader.NEON_RED);
        if (game.audioManager != null) {
            game.audioManager.attachDefaultTo(clearBtn);
        }
        clearBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                manager.clearAll();
            }
        });
        footer.add(clearBtn).width(120).right();
        contentTable.add(footer).growX().pad(5);

        refreshList();
    }

    public void refreshList() {
        if (listTable == null) return;
        listTable.clear();

        if (manager.getNotifications().isEmpty()) {
            VisLabel empty = new VisLabel("No intercepted signals.", "small");
            empty.setColor(SkinLoader.TEXT_DIM);
            listTable.add(empty).pad(40);
            return;
        }

        int index = 0;
        for (NotificationEntry entry : manager.getNotifications()) {
            VisTable row = new VisTable();
            if (skin.has("card-bg", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
                row.setBackground(skin.getDrawable("card-bg"));
            }
            row.pad(10, 12, 10, 12);

            // Type icon
            String typeIcon;
            Color typeColor;
            switch (entry.getType()) {
                case SUCCESS:
                    typeIcon = "✓";
                    typeColor = SkinLoader.NEON_LIME;
                    break;
                case WARNING:
                    typeIcon = "⚠";
                    typeColor = SkinLoader.NEON_GOLD;
                    break;
                case ALERT:
                    typeIcon = "!";
                    typeColor = SkinLoader.NEON_RED;
                    break;
                default:
                    typeIcon = "●";
                    typeColor = SkinLoader.NEON_CYAN;
            }

            VisLabel iconLabel = new VisLabel(typeIcon, "small");
            iconLabel.setColor(typeColor);
            row.add(iconLabel).width(24).padRight(8);

            // Timestamp
            String timeStr = timeFormat.format(new Date(entry.getTimestamp()));
            VisLabel timeLabel = new VisLabel(timeStr, "small");
            timeLabel.setColor(SkinLoader.TEXT_DIM);
            row.add(timeLabel).width(75).padRight(10);

            // Message
            VisLabel msgLabel = new VisLabel(entry.getMessage());
            msgLabel.setWrap(true);
            msgLabel.setColor(typeColor.cpy().lerp(Color.WHITE, 0.4f));
            row.add(msgLabel).expandX().fillX().left();

            // Slide-in animation for newly visible rows
            row.setColor(1, 1, 1, 0);
            row.addAction(Actions.sequence(
                    Actions.delay(index * 0.03f),
                    Actions.parallel(
                            Actions.fadeIn(0.2f),
                            Actions.moveBy(-20, 0),
                            Actions.moveBy(20, 0, 0.2f)
                    )
            ));

            listTable.add(row).growX().padBottom(4).row();
            index++;
        }
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            manager.markAllAsRead();
            refreshList();
        }
    }
}
