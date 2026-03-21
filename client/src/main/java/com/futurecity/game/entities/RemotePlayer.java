package com.futurecity.game.entities;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.futurecity.shared.config.GameConstants;
import com.futurecity.shared.entities.PlayerState;
import com.futurecity.shared.enums.AnimationName;
import com.futurecity.shared.enums.InteractionAction;

/**
 * Äáº¡i diá»‡n cho ngÆ°á»i chÆ¡i KHĂC trong tháº¿ giá»›i game.
 * Class nĂ y khĂ´ng xá»­ lĂ½ input, chá»‰ hiá»ƒn thá»‹ (Render) vĂ  lĂ m
 * mÆ°á»£t chuyá»ƒn Ä‘á»™ng
 * (Interpolation).
 */
public class RemotePlayer extends GameObject implements Interactable {

    // Controller Ä‘á»ƒ cháº¡y animation (Äi bá»™, Ä‘á»©ng yĂªn...)
    private final AnimationController animationController;

    // --- Interpolation Variables ---
    private final Vector3 targetPosition = new Vector3();
    private float targetAngle = 0f;

    // Offset gĂ³c xoay (do model gá»‘c bá»‹ lá»‡ch)
    private final float rotationOffset = 270f;

    // ID cá»§a ngÆ°á»i chÆ¡i
    private int playerId = -1;
    private int dbUserId = -1;
    private String username = "";

    public RemotePlayer(ModelInstance instance, PlayerState initialState) {
        super(instance);

        // Khá»Ÿi táº¡o vá»‹ trĂ­ ban Ä‘áº§u ngay láº­p tá»©c
        if (initialState != null) {
            this.playerId = initialState.id;
            this.dbUserId = initialState.dbUserId;
            this.username = initialState.username != null ? initialState.username : "Player " + playerId;
            this.position.set(initialState.position);
            this.targetPosition.set(initialState.position);
            this.targetAngle = initialState.angle;
        }

        // --- CĂ€I Ä áº¶T VA CHáº M (BOUNDING BOX) ---
        // Sá»­ dá»¥ng kĂ­ch thÆ°á»›c cá»‘ Ä‘á»‹nh nhÆ° MainCharactor Ä‘á»ƒ Ä‘áº£m báº£o
        // tÆ°Æ¡ng tĂ¡c khá»›p nhau
        Vector3 min = new Vector3(-GameConstants.PLAYER_WIDTH / 2, 0, -GameConstants.PLAYER_DEPTH / 2);
        Vector3 max = new Vector3(GameConstants.PLAYER_WIDTH / 2, GameConstants.PLAYER_HEIGHT,
                GameConstants.PLAYER_DEPTH / 2);
        this.localBounds.set(min, max);
        updateBoundsFromTransform();

        // Setup Animation
        this.animationController = new AnimationController(instance);
        if (initialState != null && initialState.currentAnimation != null && !initialState.currentAnimation.isEmpty()) {
            animationController.setAnimation(initialState.currentAnimation, -1);
        } else {
            animationController.setAnimation(AnimationName.IDLE.getValue(), -1);
        }
    }

    /**
     * Nháº­n gĂ³i tin tá»« Server cáº­p nháº­t vá»‹ trĂ­ Ä‘Ă­ch.
     */
    /**
     * Cáº­p nháº­t tráº¡ng thĂ¡i má»¥c tiĂªu tá»« Server.
     * DĂ¹ng Ä‘á»ƒ ná»™i suy vá»‹ trĂ­ vĂ  xoay, trĂ¡nh hiá»‡n tÆ°á»£ng giáº­t lag
     * khi máº¡ng khĂ´ng á»•n
     * Ä‘á»‹nh.
     */
    public void setTargetState(PlayerState state) {
        // Cáº­p nháº­t Ä‘Ă­ch Ä‘áº¿n má»›i
        this.targetPosition.set(state.position);
        this.targetAngle = state.angle;
        this.dbUserId = state.dbUserId;
        if (state.username != null)
            this.username = state.username;

        // Cáº­p nháº­t Animation náº¿u thay Ä‘á»•i
        String animId = state.currentAnimation;
        if (animId == null || animId.isEmpty())
            animId = AnimationName.IDLE.getValue();

        if (animationController.current == null || !animationController.current.animation.id.equals(animId)) {
            // Chuyá»ƒn animation mÆ°á»£t trong 0.2s
            animationController.animate(animId, -1, 1f, null, 0.2f);
        }
    }

    /**
     * HĂ m update gá»i má»—i frame (60 FPS)
     */
    /**
     * Cáº­p nháº­t logic cá»§a ngÆ°á»i chÆ¡i khĂ¡c má»—i khung hĂ¬nh.
     */
    public void update(float delta) {
        // 1. INTERPOLATION (LĂ m mÆ°á»£t vá»‹ trĂ­)
        // Di chuyá»ƒn vá» phĂ­a Ä‘Ă­ch vá»›i tá»‘c Ä‘á»™ phá»¥ thuá»™c vĂ o delta time
        // Alpha = 10f * delta giĂºp di chuyá»ƒn mÆ°á»£t mĂ  á»Ÿ má»i FPS
        // Náº¿u delta = 0.016 (60fps) -> alpha ~ 0.16 (nhanh hÆ¡n cÅ© chĂºt)
        float alpha = 10f * delta;
        if (alpha > 1f)
            alpha = 1f;
        this.position.lerp(targetPosition, alpha);

        // 2. Cáº­p nháº­t Model Transform (Vá»‹ trĂ­ + Xoay + Scale)
        updateTransform();

        // 3. Update Animation
        animationController.update(delta);
    }

    private void updateTransform() {
        // Reset transform vá» vá»‹ trĂ­ má»›i
        instance.transform.setToTranslation(this.position);

        // Xoay theo gĂ³c server gá»­i vá»
        instance.transform.rotate(Vector3.Y, targetAngle + rotationOffset);

        // Scale model theo tá»· lá»‡ game
        instance.transform.scale(GameConstants.WORLD_SCALE, GameConstants.WORLD_SCALE, GameConstants.WORLD_SCALE);

        // Cáº­p nháº­t bounding box
        updateBoundsFromTransform();
    }

    // --- INTERACTABLE IMPLEMENTATION ---

    @Override
    public BoundingBox getCollisionBox() {
        return this.getWorldBounds();
    }

    @Override
    public void onInteract() {
        System.out.println("Tương tác với người chơi: " + playerId);
    }

    @Override
    public String getInteractionPrompt() {
        return "Press F to interact";
    }

    @Override
    public float getRequiredDot() {
        return 0.5f;
    }

    @Override
    public String getNameId() {
        return "player_" + playerId;
    }

    public int getDbUserId() {
        return dbUserId;
    }

    @Override
    public InteractionAction getAction() {
        return InteractionAction.INTERACT;
    }

    public String getUsername() {
        return (username != null && !username.isEmpty()) ? username : "Player " + playerId;
    }
}
