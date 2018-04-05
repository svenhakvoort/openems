package io.openems.edge.bridge.modbus.protocol;

import com.ghgande.j2mod.modbus.procimg.InputRegister;

import io.openems.common.exceptions.OpenemsException;

/**
 * A RegisterElement represents one or more registers in a {@link Range}.
 * 
 * @author stefan.feilmeier
 */
public abstract class RegisterElement<T> {

	private final int startAddress;
	private final boolean isIgnored;

	protected Range range = null;

	/*
	 * The onUpdateCallback is called on reception of a new value
	 */
	private OnUpdate<T> onUpdateCallback;

	public RegisterElement(int startAddress) {
		this(startAddress, false);
	}

	public RegisterElement(int startAddress, boolean isIgnored) {
		this.startAddress = startAddress;
		this.onUpdateCallback = null;
		this.isIgnored = isIgnored;
	}

	public RegisterElement<T> onUpdateCallback(OnUpdate<T> onUpdateCallback) {
		this.onUpdateCallback = onUpdateCallback;
		return this;
	}

	public int getStartAddress() {
		return startAddress;
	}

	/**
	 * Number of registers
	 * 
	 * @return
	 */
	public abstract int getLength();

	/**
	 * Whether this Element should be ignored (= DummyElement)
	 * 
	 * @return
	 */
	public boolean isIgnored() {
		return isIgnored;
	}

	/**
	 * Set the {@link Range}, where this Element belongs to. This is called during
	 * {@link Range}.add()
	 *
	 * @param range
	 */
	protected void setModbusRange(Range range) {
		this.range = range;
	}

	public Range getModbusRange() {
		return range;
	}

	protected void setValue(T value) {
		if (this.onUpdateCallback != null) {
			this.onUpdateCallback.call(value);
		}
	}

	public void setInputRegisters(InputRegister... registers) throws OpenemsException {
		if (registers.length != this.getLength()) {
			throw new OpenemsException("Modbus address [" + startAddress + "]: registers length [" + registers.length
					+ "] does not match required size of [" + this.getLength() + "]");
		}
		this._setInputRegisters(registers);
	}

	protected abstract void _setInputRegisters(InputRegister... registers);

	/**
	 * BuilderPattern. The received value is adjusted to the power of the
	 * scaleFactor (y = x * 10^scaleFactor).
	 */
	public abstract RegisterElement<T> scaleFactor(int scaleFactor);

	// protected void setValue(T value) {
	// if (channel == null) {
	// return;
	// } else if (channel instanceof ModbusReadChannel) {
	// ((ModbusReadChannel<T>) channel).updateValue(value);
	// } else if (channel instanceof ModbusWriteChannel) {
	// ((ModbusWriteChannel<T>) channel).updateValue(value);
	// } else {
	// log.error("Unable to set value [" + value + "]. Channel [" +
	// channel.address()
	// + "] is no ModbusChannel or WritableModbusChannel.");
	// new Throwable().printStackTrace();
	// }
	// }
	//
	// @Override
	// public String toString() {
	// return "ModbusElement: Implementation[" + this.getClass().getSimpleName() +
	// "], ModbusAddress[" + address + "]"
	// + (channel != null ? ", ChannelAddress[" + channel.address() + "]" : "");
	// }
}
