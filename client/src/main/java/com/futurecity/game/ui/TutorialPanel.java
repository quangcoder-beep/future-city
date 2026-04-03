package com.futurecity.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;

/**
 * Premium Tutorial Panel.
 * Features: Keycap styled key badges, illustrated control guide.
 */
public class TutorialPanel extends BasePanel {

    public TutorialPanel(Skin skin) {
        super("OPERATOR MANUAL", skin);
        populateContent();
    }

    @Override
    public void showPanel() {
        if (getStage() != null) {
            float stageW = getStage().getWidth();
            float stageH = getStage().getHeight();
            float width = Math.min(680, stageW - 50);
            float height = Math.min(520, stageH - 80);
            setSize(width, height);
        }
        super.showPanel();
    }

    @Override
    protected void populateContent() {
        contentTable.clear();
        contentTable.top().padTop(10);

        VisTable innerTable = new VisTable();
        innerTable.top().left().pad(15);

        // ═══ 1. CONTROLS SECTION ═══
        addSectionHeader(innerTable, "COMMAND MATRIX");

        VisTable grid = new VisTable();
        grid.defaults().pad(5).left();

        // Column Headers
        VisLabel actionHeader = new VisLabel("FUNCTION", "small");
        actionHeader.setColor(SkinLoader.NEON_GOLD);
        VisLabel keyHeader = new VisLabel("KEYMAP", "small");
        keyHeader.setColor(SkinLoader.NEON_GOLD);

        grid.add(actionHeader).width(250);
        grid.add(keyHeader).expandX().left().row();

        // Separator
        if (skin.has("separator-neon", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
            grid.add(new Image(skin.getDrawable("separator-neon"))).height(1).growX().colspan(2).padBottom(5).row();
        }

        addControlRow(grid, "Move Character", "W", "A", "S", "D");
        addControlRow(grid, "Sprint", "E");
        addControlRow(grid, "Jump", "SPACE");
        addControlRow(grid, "Interact / Close UI", "F");
        addControlRow(grid, "Inventory / Dashboard", "H");
        addControlRow(grid, "Smartphone / Job Board", "P");
        addControlRow(grid, "System Menu", "ESC");
        addControlRow(grid, "Toggle Minimap Mode", "R");
        addControlRow(grid, "Switch Camera View", "C");
        addControlRow(grid, "Toggle Mouse Lock", "B");
        addControlRow(grid, "Rotate Camera", "MOUSE");
        addControlRow(grid, "Set GPS Target", "CLICK MAP");

        innerTable.add(grid).growX().padLeft(5).row();

        // ═══ 2. GAMEPLAY GUIDE ═══
        addSectionHeader(innerTable, "OPERATION PROTOCOL");

        String[] steps = {
            "1. Open Smartphone [P] or talk to NPCs to find delivery contracts.",
            "2. Follow the GPS trail on the minimap to the pickup location.",
            "3. Press [F] near the shop to pick up the delivery item.",
            "4. Follow GPS to the customer and press [F] to complete delivery.",
            "5. Earn coins and use them to purchase items from city shops."
        };

        for (String step : steps) {
            VisTable stepRow = new VisTable();
            if (skin.has("card-bg", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
                stepRow.setBackground(skin.getDrawable("card-bg"));
            }
            stepRow.pad(8, 12, 8, 12);

            VisLabel stepLabel = new VisLabel(step, "small");
            stepLabel.setWrap(true);
            stepLabel.setColor(SkinLoader.TEXT_SECONDARY);
            stepRow.add(stepLabel).growX();

            innerTable.add(stepRow).growX().padBottom(4).padLeft(5).padRight(5).row();
        }

        // Scroll wrapper
        VisScrollPane scrollPane = new VisScrollPane(innerTable);
        scrollPane.setFadeScrollBars(false);
        enableAutoScrollFocus(scrollPane);
        contentTable.add(scrollPane).expand().fill();

        // Footer
        VisLabel footer = new VisLabel("Explore Future City and build your career!", "small");
        footer.setColor(SkinLoader.NEON_LIME);
        contentTable.row();
        contentTable.add(footer).padTop(8).padBottom(8);
    }

    private void addSectionHeader(VisTable table, String title) {
        VisTable header = new VisTable();
        if (skin.has("separator-neon", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
            header.add(new Image(skin.getDrawable("separator-neon"))).height(2).width(20).padRight(8);
        }
        VisLabel label = new VisLabel(title, "small");
        label.setColor(SkinLoader.NEON_CYAN);
        header.add(label);
        if (skin.has("separator-neon", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
            header.add(new Image(skin.getDrawable("separator-neon"))).height(2).expandX().fillX().padLeft(8);
        }
        table.add(header).growX().padTop(18).padBottom(10).row();
    }

    /**
     * Adds a control row with keycap-styled badges.
     */
    private void addControlRow(VisTable table, String action, String... keys) {
        VisLabel actionLabel = new VisLabel(action, "small");
        actionLabel.setColor(Color.WHITE);
        table.add(actionLabel).width(250);

        VisTable keyGroup = new VisTable();
        for (int i = 0; i < keys.length; i++) {
            VisTable keycap = new VisTable();
            if (skin.has("keycap", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
                keycap.setBackground(skin.getDrawable("keycap"));
            }
            keycap.pad(3, 8, 3, 8);

            VisLabel keyLabel = new VisLabel(keys[i], "small");
            keyLabel.setColor(SkinLoader.NEON_LIME);
            keycap.add(keyLabel);

            keyGroup.add(keycap).padRight(i < keys.length - 1 ? 4 : 0);
        }
        table.add(keyGroup).left().row();
    }
}
