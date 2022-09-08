package io.openems.edge.common.weather;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.service.event.Event;

public class DummyWeather extends AbstractOpenemsComponent implements Weather, OpenemsComponent {

	public DummyWeather() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Weather.ChannelId.values() //
		);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, Weather.SINGLETON_COMPONENT_ID, Weather.SINGLETON_SERVICE_PID, true);
	}

	@Override
	public void handleEvent(Event event) {
		// nothing here
	}

}