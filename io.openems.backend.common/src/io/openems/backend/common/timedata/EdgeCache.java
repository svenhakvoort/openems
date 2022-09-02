package io.openems.backend.common.timedata;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SortedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.types.ChannelAddress;

public class EdgeCache {

	private final Logger log = LoggerFactory.getLogger(EdgeCache.class);

	/**
	 * The Timestamp of the data in the Cache.
	 */
	private long cacheTimestamp = 0L;

	/**
	 * The Timestamp when the Cache was last applied to the incoming data.
	 */
	private long lastAppliedTimestamp = 0L;

	private final HashMap<ChannelAddress, JsonElement> cacheData = new HashMap<>();

	/**
	 * Gets the channel value from cache.
	 *
	 * @param address the {@link ChannelAddress} of the channel
	 * @return the value; empty if it is not in cache
	 */
	public final Optional<JsonElement> getChannelValue(ChannelAddress address) {
		synchronized (this) {
			return Optional.ofNullable(this.cacheData.get(address));
		}
	}

	/**
	 * Updates the 'incoming data' with the data from the cache.
	 *
	 * @param edgeId        the Edge-ID
	 * @param incomingDatas the incoming data
	 */
	public void complementDataFromCache(String edgeId,
			SortedMap<Long, Map<ChannelAddress, JsonElement>> incomingDatas) {
		for (Entry<Long, Map<ChannelAddress, JsonElement>> entry : incomingDatas.entrySet()) {
			var incomingTimestamp = entry.getKey();
			var incomingData = entry.getValue();

			// Check if cache should be applied
			if (incomingTimestamp < this.cacheTimestamp) {
				// Incoming data is older than cache -> do not apply cache

			} else {
				// Incoming data is more recent than cache

				if (incomingTimestamp > this.cacheTimestamp + 5 * 60 * 1000) {
					// Cache is not anymore valid (elder than 5 minutes)
					if (this.cacheTimestamp != 0L) {
						this.log.info("Edge [" + edgeId + "]: invalidate cache. Incoming ["
								+ Instant.ofEpochMilli(incomingTimestamp) + "]. Cache ["
								+ Instant.ofEpochMilli(this.cacheTimestamp) + "]");
					}
					synchronized (this) {
						// Clear Cache
						this.cacheData.clear();
					}

				} else if (incomingTimestamp < this.lastAppliedTimestamp + 2 * 60 * 1000) {
					// Apply Cache only once every two minutes to throttle writes

				} else {
					// Apply Cache

					// cache is valid (not elder than 5 minutes)
					this.lastAppliedTimestamp = incomingTimestamp;
					synchronized (this) {
						this.cacheData.entrySet().stream() //
								.forEach(e -> {
									// check if there is a current value for this timestamp + channel
									// if not -> add cache data to write data
									incomingData.putIfAbsent(e.getKey(), e.getValue());
								});
					}
				}

				// update cache
				this.cacheTimestamp = incomingTimestamp;
				synchronized (this) {
					this.cacheData.putAll(incomingData);
				}
			}
		}
	}

}
