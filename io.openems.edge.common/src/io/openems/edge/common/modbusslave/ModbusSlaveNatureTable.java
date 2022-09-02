package io.openems.edge.common.modbusslave;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.openems.common.channel.AccessMode;
import io.openems.edge.common.channel.ChannelId;

public final class ModbusSlaveNatureTable {

	public static class Builder {
		private final Class<?> nature;
		private final AccessMode accessModeFilter;
		private final int length;
		private final List<ModbusRecord> maps = new ArrayList<>();

		private int nextOffset = 0;

		public Builder(Class<?> nature, AccessMode accessModeFilter, int length) {
			this.nature = nature;
			this.accessModeFilter = accessModeFilter;
			this.length = length;
		}

		/**
		 * Add a Channel to the {@link Builder}.
		 * 
		 * @param offset    the register address offset
		 * @param channelId the {@link ChannelId}
		 * @param type      the {@link ModbusType}
		 * @return myself
		 */
		public Builder channel(int offset, ChannelId channelId, ModbusType type) {
			var filter = this.accessModeFilter;
			var channel = channelId.doc().getAccessMode();
			if (
			// Filter for READ_ONLY Channels
			filter == AccessMode.READ_ONLY && (channel == AccessMode.READ_ONLY || channel == AccessMode.READ_WRITE) //
					||
					// Filter for READ_WRITE channels -> allow all Channels
					filter == AccessMode.READ_WRITE //
					||
					// Filter for WRITE_ONLY channels
					filter == AccessMode.WRITE_ONLY
							&& (channel == AccessMode.WRITE_ONLY || channel == AccessMode.READ_WRITE)) {
				this.add(new ModbusRecordChannel(offset, type, channelId, filter));

			} else {
				// Channel did not pass filter -> show as Reserved
				switch (type) {
				case FLOAT32:
					this.float32Reserved(offset);
					break;
				case FLOAT64:
					this.float64Reserved(offset);
					break;
				case STRING16:
					this.string16Reserved(offset);
					break;
				case ENUM16:
				case UINT16:
					this.uint16Reserved(offset);
					break;
				case UINT32:
					this.uint32Reserved(offset);
					break;
				}
			}
			return this;
		}

		/**
		 * Add a Unsigned Int 16 value to the {@link ModbusSlaveNatureTable}
		 * {@link Builder}.
		 * 
		 * @param offset the address offset
		 * @param name   the name of the register
		 * @param value  the value
		 * @return myself
		 */
		public Builder uint16(int offset, String name, short value) {
			this.add(new ModbusRecordUint16(offset, name, value));
			return this;
		}

		/**
		 * Add a Unsigned Int 16 Hash value to the {@link ModbusSlaveNatureTable}
		 * {@link Builder}.
		 * 
		 * @param offset the address offset
		 * @param text   the value
		 * @return myself
		 */
		public Builder uint16Hash(int offset, String text) {
			this.add(new ModbusRecordUint16Hash(offset, text));
			return this;
		}

		/**
		 * Add a Unsigned Int 16 Reserved value to the {@link ModbusSlaveNatureTable}
		 * {@link Builder}.
		 * 
		 * @param offset the address offset
		 * @return myself
		 */
		public Builder uint16Reserved(int offset) {
			this.add(new ModbusRecordUint16Reserved(offset));
			return this;
		}

		/**
		 * Add a Unsigned Int 32 Reserved value to the {@link ModbusSlaveNatureTable}
		 * {@link Builder}.
		 * 
		 * @param offset the address offset
		 * @return myself
		 */
		public Builder uint32Reserved(int offset) {
			this.add(new ModbusRecordUint32Reserved(offset));
			return this;
		}

		/**
		 * Add a Float 32 value to the {@link ModbusSlaveNatureTable} {@link Builder}.
		 * 
		 * @param offset the address offset
		 * @param name   the name of the register
		 * @param value  the value
		 * @return myself
		 */
		public Builder float32(int offset, String name, float value) {
			this.add(new ModbusRecordFloat32(offset, name, value));
			return this;
		}

		/**
		 * Add a Float 32 Reserved value to the {@link ModbusSlaveNatureTable}
		 * {@link Builder}.
		 * 
		 * @param offset the address offset
		 * @return myself
		 */
		public Builder float32Reserved(int offset) {
			this.add(new ModbusRecordFloat32Reserved(offset));
			return this;
		}

		/**
		 * Add a Float 64 value to the {@link ModbusSlaveNatureTable} {@link Builder}.
		 * 
		 * @param offset the address offset
		 * @param name   the name of the register
		 * @param value  the value
		 * @return myself
		 */
		public Builder float64(int offset, String name, double value) {
			this.add(new ModbusRecordFloat64(offset, name, value));
			return this;
		}

		/**
		 * Add a Float 64 Reserved value to the {@link ModbusSlaveNatureTable}
		 * {@link Builder}.
		 * 
		 * @param offset the address offset
		 * @return myself
		 */
		public Builder float64Reserved(int offset) {
			this.add(new ModbusRecordFloat64Reserved(offset));
			return this;
		}

		/**
		 * Add a String 16 value to the {@link ModbusSlaveNatureTable} {@link Builder}.
		 * 
		 * @param offset the address offset
		 * @param name   the name of the register
		 * @param value  the value
		 * @return myself
		 */
		public Builder string16(int offset, String name, String value) {
			this.add(new ModbusRecordString16(offset, name, value));
			return this;
		}

		/**
		 * Add a String 16 Reserved value to the {@link ModbusSlaveNatureTable}
		 * {@link Builder}.
		 * 
		 * @param offset the address offset
		 * @return myself
		 */
		public Builder string16Reserved(int offset) {
			this.add(new ModbusRecordString16Reserved(offset));
			return this;
		}

		private void add(ModbusRecord record) throws IllegalArgumentException {
			if (record.getOffset() != this.nextOffset) {
				throw new IllegalArgumentException("Expected offset [" + this.nextOffset + "] but got ["
						+ record.getOffset() + "] for Record [" + record + "]");
			}
			this.nextOffset += record.getType().getWords();
			// TODO validate that this 'offset' is not already used (e.g. by float32)
			this.maps.add(record);
		}

		public ModbusSlaveNatureTable build() {
			Collections.sort(this.maps, Comparator.comparing(ModbusRecord::getOffset));
			return new ModbusSlaveNatureTable(this.nature, this.length,
					this.maps.toArray(new ModbusRecord[this.maps.size()]));
		}

	}

	/**
	 * Builds a {@link ModbusSlaveNatureTable} {@link Builder}.
	 * 
	 * @param nature     the OpenEMS Nature {@link Class}
	 * @param accessMode the {@link AccessMode}
	 * @param length     the reserved total length
	 * @return the {@link Builder}
	 */
	public static Builder of(Class<?> nature, AccessMode accessMode, int length) {
		return new Builder(nature, accessMode, length);
	}

	private final Class<?> nature;
	private final int length;
	private final ModbusRecord[] modbusRecords;

	private ModbusSlaveNatureTable(Class<?> nature, int length, ModbusRecord[] modbusRecords) {
		this.nature = nature;
		this.length = length;
		this.modbusRecords = modbusRecords;
	}

	public Class<?> getNature() {
		return this.nature;
	}

	public int getLength() {
		return this.length;
	}

	public ModbusRecord[] getModbusRecords() {
		return this.modbusRecords;
	}
}
