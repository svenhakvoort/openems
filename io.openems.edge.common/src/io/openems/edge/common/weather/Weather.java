package io.openems.edge.common.weather;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.service.event.EventHandler;

public interface Weather extends OpenemsComponent, EventHandler {

    public static final String SINGLETON_SERVICE_PID = "Core.Weather";
    public static final String SINGLETON_COMPONENT_ID = "_weather";

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

}
