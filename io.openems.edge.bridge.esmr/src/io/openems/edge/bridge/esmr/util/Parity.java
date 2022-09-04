package io.openems.edge.bridge.esmr.util;

public enum Parity {
    NONE(0),
    ODD(1),
    EVEN(2),
    MARK(3),
    SPACE(4);

    private static final Parity[] VALUES = values();
    private final int odlValue;

    private Parity(int oldValue) {
        this.odlValue = oldValue;
    }

    public static Parity forValue(int parity) {
        for (Parity p : VALUES) {
            if (p.odlValue == parity) {
                return p;
            }
        }

        throw new RuntimeException("Error.");
    }

    public int getOldValue() {
        return this.odlValue;
    }

}
