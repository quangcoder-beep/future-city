package com.futurecity.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTable;

/**
 * Premium Settings Panel.
 * Features: Separate volume controls with % display, graphics quality selector.
 */
public class SettingsPanel extends BasePanel {

    public SettingsPanel(Skin skin) {
        super("SETTINGS", skin);
        setSize(420, 420);
        populateContent();
    }

    @Override
    protected void populateContent() {
        contentTable.clear();
        contentTable.top().pad(15, 10, 15, 10);

        final com.futurecity.game.core.Main gameMain = (com.futurecity.game.core.Main) Gdx.app.getApplicationListener();

        // ═══ AUDIO SECTION ═══
        addSectionHeader("AUDIO");

        addVolumeRow("MASTER", gameMain != null && gameMain.audioManager != null
                ? gameMain.audioManager.getMasterVolume() : 0.7f, val -> {
            if (gameMain != null && gameMain.audioManager != null)
                gameMain.audioManager.setMasterVolume(val);
        });

        addVolumeRow("SFX", 0.8f, val -> {});
        addVolumeRow("MUSIC", 0.5f, val -> {});

        // ═══ GRAPHICS SECTION ═══
        addSectionHeader("GRAPHICS");

        VisTable gfxRow = new VisTable();
        gfxRow.pad(6, 0, 6, 0);

        VisLabel qLabel = new VisLabel("QUALITY", "small");
        qLabel.setColor(SkinLoader.TEXT_SECONDARY);
        gfxRow.add(qLabel).left().padRight(10);

        VisTable btnGroup = new VisTable();
        String[] levels = {"LOW", "MED", "HIGH"};
        final VisLabel[] levelLabels = new VisLabel[3];
        final int[] selected = {1};

        for (int i = 0; i < levels.length; i++) {
            final int idx = i;
            VisTable card = new VisTable();
            if (skin.has("card-bg", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
                card.setBackground(skin.getDrawable("card-bg"));
            }
            card.pad(5, 10, 5, 10);

            levelLabels[i] = new VisLabel(levels[i], "small");
            levelLabels[i].setColor(i == selected[0] ? SkinLoader.NEON_CYAN : SkinLoader.TEXT_DIM);
            card.add(levelLabels[i]);

            card.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
                @Override
                public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                    selected[0] = idx;
                    for (int j = 0; j < levelLabels.length; j++) {
                        levelLabels[j].setColor(j == idx ? SkinLoader.NEON_CYAN : SkinLoader.TEXT_DIM);
                    }
                    if (gameMain != null && gameMain.audioManager != null)
                        gameMain.audioManager.playSfx("sounds/ui/click.ogg");
                }
            });
            btnGroup.add(card).padRight(5);
        }
        gfxRow.add(btnGroup).expandX().left();
        contentTable.add(gfxRow).growX().padBottom(10).row();

        // Footer
        VisLabel footer = new VisLabel("BUILD 2026.04", "metadata-label");
        contentTable.add(footer).expandY().bottom().padTop(15);
    }

    /**
     * Creates a compact volume row: LABEL [====SLIDER====] 70%
     */
    private void addVolumeRow(String name, float initialValue, java.util.function.Consumer<Float> onChange) {
        VisTable row = new VisTable();
        row.pad(5, 0, 5, 0);

        VisLabel label = new VisLabel(name, "small");
        label.setColor(SkinLoader.TEXT_SECONDARY);
        row.add(label).left().width(70);

        final VisSlider slider = new VisSlider(0, 1, 0.05f, false);
        slider.setValue(initialValue);
        slider.setColor(SkinLoader.NEON_CYAN);
        row.add(slider).expandX().fillX().padLeft(8).padRight(8);

        VisLabel pctLabel = new VisLabel(pct(initialValue), "small");
        pctLabel.setColor(SkinLoader.NEON_GOLD);
        row.add(pctLabel).right().width(40);

        slider.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                pctLabel.setText(pct(slider.getValue()));
                if (onChange != null) onChange.accept(slider.getValue());
            }
        });

        contentTable.add(row).growX().padBottom(3).row();
    }

    private void addSectionHeader(String title) {
        VisTable header = new VisTable();
        if (skin.has("separator-neon", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
            header.add(new com.badlogic.gdx.scenes.scene2d.ui.Image(skin.getDrawable("separator-neon")))
                    .height(2).width(20).padRight(6);
        }
        VisLabel titleLabel = new VisLabel(title, "small");
        titleLabel.setColor(SkinLoader.NEON_CYAN);
        header.add(titleLabel);
        if (skin.has("separator-neon", com.badlogic.gdx.scenes.scene2d.utils.Drawable.class)) {
            header.add(new com.badlogic.gdx.scenes.scene2d.ui.Image(skin.getDrawable("separator-neon")))
                    .height(2).expandX().fillX().padLeft(6);
        }
        contentTable.add(header).growX().padTop(12).padBottom(8).row();
    }

    private String pct(float val) {
        return (int) (val * 100) + "%";
    }
}
