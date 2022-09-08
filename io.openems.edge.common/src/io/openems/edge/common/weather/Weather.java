package io.openems.edge.common.weather;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface Weather extends OpenemsComponent {

    public static final String SINGLETON_SERVICE_PID = "Core.Weather";
    public static final String SINGLETON_COMPONENT_ID = "_weather";

    void updateChannelsBeforeProcessImage();

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        SUN_INTENSITY(
                Doc.of(OpenemsType.FLOAT) //
                        .unit(Unit.WATT_HOURS_BY_SQUARE_METER)
                        .accessMode(AccessMode.READ_WRITE)
                        .persistencePriority(PersistencePriority.HIGH)
        );

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
