package io.openems.edge.bridge.modbus.api.element;

import java.nio.ByteBuffer;

import io.openems.common.types.OpenemsType;

/**
 * A SignedDoublewordElement represents a Long value in an
 * {@link AbstractDoubleWordElement}.
 */
public class SignedDoublewordElement extends AbstractDoubleWordElement<SignedDoublewordElement, Long> {

	public SignedDoublewordElement(int address) {
		super(OpenemsType.LONG, address);
	}

	@Override
	protected SignedDoublewordElement self() {
		return this;
	}

	@Override
	protected Long fromByteBuffer(ByteBuffer buff) {
		int intValue = buff.getInt();
		if (intValue == Integer.MAX_VALUE || intValue == Integer.MIN_VALUE) {
			return 0L;
		}
		return (long) intValue;
	}

	@Override
	protected ByteBuffer toByteBuffer(ByteBuffer buff, Long value) {
		return buff.putInt(value.intValue());
	}

}
