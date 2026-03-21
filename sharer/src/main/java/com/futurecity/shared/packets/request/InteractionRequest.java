package com.futurecity.shared.packets.request;

import com.futurecity.shared.enums.InteractionAction;

public class InteractionRequest {
    public int id; // ID cá»§a ngÆ°á»i chÆ¡i
    public String targetId; // ID cá»§a váº­t thá»ƒ (Cá»­a, RÆ°Æ¡ng, NPC...)
    public InteractionAction action; // Loáº¡i hĂ nh Ä‘á»™ng: "open", "loot", "talk"
}
