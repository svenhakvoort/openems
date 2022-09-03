package io.openems.edge.meter.sma.sunnyboy3;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;

public interface MeterSmaSunnyBoy3 {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		FREQUENCY(Doc.of(OpenemsType.INTEGER)
				.unit(Unit.HERTZ))
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
