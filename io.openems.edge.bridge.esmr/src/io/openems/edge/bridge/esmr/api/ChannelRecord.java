package io.openems.edge.bridge.esmr.api;

import io.openems.edge.common.channel.Channel;
import nl.basjes.dsmr.DSMRTelegram;

import java.util.function.Consumer;
import java.util.function.Function;

public class ChannelRecord {

	private final Function<DSMRTelegram, Object> dataCollector;
	private Channel<?> channel;

	/**
	 * In this case you will request secondary address values. eg. manufacturer,
	 * device id or meter type.
	 *
	 * @param channel  the Channel
	 * @param dataCollector the dataCollector
	 */
	public ChannelRecord(Channel<?> channel, Function<DSMRTelegram, Object> dataCollector) {
		this.channel = channel;
		this.dataCollector = dataCollector;
	}

	public Channel<?> getChannel() {
		return this.channel;
	}

	public void setChannelId(Channel<?> channel) {
		this.channel = channel;
	}

	public Function<DSMRTelegram, Object> getDataCollector() {
		return dataCollector;
	}

}
