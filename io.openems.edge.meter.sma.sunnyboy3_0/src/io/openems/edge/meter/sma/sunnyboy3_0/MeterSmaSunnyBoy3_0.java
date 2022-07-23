package io.openems.edge.meter.sma.sunnyboy3_0;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;

public interface MeterSmaSunnyBoy3_0 {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		ACTIVE_PRODUCTION_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT))
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
