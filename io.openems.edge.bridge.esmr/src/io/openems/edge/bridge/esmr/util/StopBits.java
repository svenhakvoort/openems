package io.openems.edge.bridge.esmr.util;

public enum StopBits {
    STOPBITS_1(1),
    STOPBITS_1_5(3),
    STOPBITS_2(2);

    private final int odlValue;

    private StopBits(int oldValue) {
        this.odlValue = oldValue;
    }

    public int getOldValue() {
        return this.odlValue;
    }
}