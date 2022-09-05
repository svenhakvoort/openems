package io.openems.edge.meter.esmr;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;

public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
    TOTAL_CONSUMED_ENERGY(Doc.of(OpenemsType.INTEGER) //
            .unit(Unit.KILOWATT_HOURS))
    ;

    private final Doc doc;

    private ChannelId(Doc doc) {
        this.doc = doc;
    }

    @Override
    public Doc doc() {
        return this.doc;
    }

}