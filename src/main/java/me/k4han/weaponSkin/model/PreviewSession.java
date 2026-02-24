package me.k4han.weaponSkin.model;

public class PreviewSession {

    private final int slot;
    private final int sendTaskId;
    private final int revertTaskId;

    public PreviewSession(int slot, int sendTaskId, int revertTaskId) {
        this.slot = slot;
        this.sendTaskId = sendTaskId;
        this.revertTaskId = revertTaskId;
    }

    public int getSlot() { return slot; }
    public int getSendTaskId() { return sendTaskId; }
    public int getRevertTaskId() { return revertTaskId; }
}
