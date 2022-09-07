package io.openems.edge.controller.weathercloud;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;

public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
    SUN_INTENSITY(Doc.of(OpenemsType.FLOAT) //
            .unit(Unit.WATT_HOURS_BY_SQUARE_METER))
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