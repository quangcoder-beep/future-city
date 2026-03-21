package com.futurecity.game.entities;

import com.badlogic.gdx.math.collision.BoundingBox;
import com.futurecity.shared.enums.InteractionAction;

public interface Interactable {
    // --- PHáº¦N 1: Dá»® LIá»†U Äá»‚ TĂNH TOĂN (Logic ToĂ¡n) ---
    /**
     * * Tráº£ vá» cĂ¡i há»™p va cháº¡m cá»§a váº­t thá»ƒ.
     * Há»‡ thá»‘ng cáº§n cĂ¡i nĂ y Ä‘á»ƒ láº¥y tĂ¢m (Center) mĂ  tĂ­nh khoáº£ng
     * cĂ¡ch & gĂ³c nhĂ¬n.
     */
    BoundingBox getCollisionBox();

    // --- PHáº¦N 2: HĂ€NH Äá»˜NG (Logic Game) ---
    /**
     * * HĂ m nĂ y sáº½ cháº¡y khi ngÆ°á»i chÆ¡i báº¥m nĂºt F thĂ nh cĂ´ng.
     * NPC thĂ¬ hiá»‡n há»™i thoáº¡i, Cá»­a thĂ¬ má»Ÿ, RÆ°Æ¡ng thĂ¬ rá»›t Ä‘á»“...
     */
    void onInteract();

    // --- PHáº¦N 3: HIá»‚N THá»Š (Logic UI) ---
    /**
     * * Tráº£ vá» dĂ²ng chá»¯ sáº½ hiá»‡n trĂªn Ä‘áº§u váº­t thá»ƒ.
     * VĂ­ dá»¥: "Talk", "Open", "Pick up", "Inspect"
     */
    String getInteractionPrompt();

    // --- PHáº¦N 4: TĂ™Y CHá»ˆNH (Optional - CĂ³ thá»ƒ Ä‘á»ƒ máº·c Ä‘á»‹nh) ---
    /**
     * Quy Ä‘á»‹nh váº­t nĂ y dá»… tÆ°Æ¡ng tĂ¡c hay khĂ³.
     * Máº·c Ä‘á»‹nh lĂ  0.5 (Dá»…). Náº¿u lĂ  váº­t bĂ© tĂ­ thĂ¬ Override tráº£ vá»
     * 0.8 (KhĂ³).
     */

    default float getRequiredDot() {
        return 0.5f;
    }

    public String getNameId();

    public InteractionAction getAction();
}
