package io.openems.edge.bridge.esmr.api;

import io.openems.edge.bridge.esmr.util.SerialPortReader;
import org.openmuc.jmbus.MBusConnection;

import io.openems.common.channel.Debounce;
import io.openems.common.channel.Level;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;

public interface BridgeEsmr {

	/**
	 * Add a Task.
	 * 
	 * @param sourceId the Source-ID
	 * @param task     the {@link EsmrTask}
	 */
	public void addTask(String sourceId, EsmrTask task);

	/**
	 * Get the {@link MBusConnection}.
	 *
	 * @return the {@link MBusConnection}
	 */
	public SerialPortReader getSerialPortReader();

	/**
	 * Remove the task with the given Source-ID.
	 * 
	 * @param sourceId the Source-ID
	 */
	public void removeTask(String sourceId);
}
