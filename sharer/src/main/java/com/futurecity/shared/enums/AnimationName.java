package com.futurecity.shared.enums;

/**
 * Danh sĂ¡ch tĂªn cĂ¡c Ä‘á»™ng tĂ¡c (Animation) cá»§a nhĂ¢n váº­t.
 * DĂ¹ng Ä‘á»ƒ Ä‘á»“ng bá»™ hĂ³a tĂªn Animation giá»¯a Client vĂ  Server.
 */
public enum AnimationName {
    IDLE("nhandoi_idle"),
    WALK("dibo_walking"),
    RUN("chay_running");

    private final String value;

    AnimationName(String value) {
        this.value = value;
    }

    /**
     * Láº¥y giĂ¡ trá»‹ chuá»—i thá»±c táº¿ cá»§a animation (khá»›p vá»›i file 3D)
     */
    public String getValue() {
        return value;
    }
}
