package io.openems.edge.bridge.esmr.api;

import io.openems.edge.common.channel.Channel;
import nl.basjes.dsmr.DSMRTelegram;

import java.util.List;
import java.util.function.Function;

public class ChannelDataRecordMapper {
	protected DSMRTelegram data;

	protected List<ChannelRecord> channelDataRecordsList;

	public ChannelDataRecordMapper(DSMRTelegram data, List<ChannelRecord> channelDataRecordsList) {
		this.data = data;
		this.channelDataRecordsList = channelDataRecordsList;

		for (ChannelRecord channelRecord : channelDataRecordsList) {
			this.mapDataToChannel(data, channelRecord.getDataCollector(), channelRecord.getChannel());
		}
	}

	private void mapDataToChannel(DSMRTelegram data, Function<DSMRTelegram, Object> dataCollector, Channel<?> channel) {
		if (data != null) {
			var collectedData = dataCollector.apply(data);
//			System.out.printf("Collected value %s with type %s and channel %s%n", collectedData, channel.getType(), channel.channelId().name());
			channel.setNextValue(collectedData);
		}
	}

	public DSMRTelegram getData() {
		return data;
	}

	public void setData(DSMRTelegram data) {
		this.data = data;
	}

	public List<ChannelRecord> getChannelDataRecordsList() {
		return this.channelDataRecordsList;
	}

	public void setChannelDataRecordsList(List<ChannelRecord> channelDataRecordsList) {
		this.channelDataRecordsList = channelDataRecordsList;
	}

}
