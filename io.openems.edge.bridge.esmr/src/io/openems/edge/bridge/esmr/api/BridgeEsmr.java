package io.openems.edge.bridge.esmr.api;

import io.openems.edge.bridge.esmr.util.SerialPortReader;

public interface BridgeEsmr {

	/**
	 * Add a Task.
	 * 
	 * @param sourceId the Source-ID
	 * @param task     the {@link EsmrTask}
	 */
	public void addTask(String sourceId, EsmrTask task);

	public SerialPortReader getSerialPortReader();

	/**
	 * Remove the task with the given Source-ID.
	 * 
	 * @param sourceId the Source-ID
	 */
	public void removeTask(String sourceId);
}
