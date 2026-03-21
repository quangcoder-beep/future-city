package com.futurecity.shared.packets.resonse;

import com.futurecity.shared.enums.InteractionAction;

public class PlayerInteractionNotification {
    public int fromPlayerId;
    public String fromName;
    public InteractionAction action;
    public String message;
}
