package io.openems.edge.bridge.esmr.util;

public enum DataBits {
    DATABITS_5(5),
    DATABITS_6(6),
    DATABITS_7(7),
    DATABITS_8(8);

    private final int odlValue;

    private DataBits(int oldValue) {
        this.odlValue = oldValue;
    }

    public int getOldValue() {
        return this.odlValue;
    }

}