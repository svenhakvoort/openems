package io.openems.edge.meter.sma.sunnyboy3_0;

import java.util.function.Consumer;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.*;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.channel.value.Value;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.SMA.SunnyBoy3_0", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterSmaSunnyBoy3_0Impl extends AbstractOpenemsModbusComponent
		implements MeterSmaSunnyBoy3_0, AsymmetricMeter, SymmetricMeter, ModbusComponent, OpenemsComponent {

	private MeterType meterType = MeterType.PRODUCTION;

	@Reference
	protected ConfigurationAdmin cm;

	public MeterSmaSunnyBoy3_0Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				MeterSmaSunnyBoy3_0.ChannelId.values() //
		);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		this.meterType = config.type();
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		var modbusProtocol = new ModbusProtocol(this,
				// Power Readings
				new FC3ReadRegistersTask(30775, Priority.HIGH, //
						m(MeterSmaSunnyBoy3_0.ChannelId.ACTIVE_PRODUCTION_POWER, new SignedDoublewordElement(30775))),
				// Voltage, Power and Reactive Power
				new FC3ReadRegistersTask(30789, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1, new UnsignedDoublewordElement(30789),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new UnsignedDoublewordElement(30791),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new UnsignedDoublewordElement(30793),
								ElementToChannelConverter.SCALE_FACTOR_1)
						),
				new FC3ReadRegistersTask(30805, Priority.HIGH, //
						m(SymmetricMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(30805))),
				// Current
				new FC3ReadRegistersTask(30797, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.CURRENT_L1, new SignedDoublewordElement(30797)),
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new SignedDoublewordElement(30799)),
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new SignedDoublewordElement(30801))),
				// Frequency
				new FC3ReadRegistersTask(30803, Priority.LOW, //
						m(SymmetricMeter.ChannelId.FREQUENCY, new UnsignedDoublewordElement(30803),
								ElementToChannelConverter.SCALE_FACTOR_1)));

		// Calculates required Channels from other existing Channels.
		this.addCalculateChannelListeners();

		return modbusProtocol;
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	private void addCalculateChannelListeners() {
		// Average Voltage from current L1, L2 and L3
		final Consumer<Value<Integer>> calculateAverageVoltage = ignore -> {
			this._setVoltage(TypeUtils.averageRounded(//
					this.getVoltageL1Channel().getNextValue().get(), //
					this.getVoltageL2Channel().getNextValue().get(), //
					this.getVoltageL3Channel().getNextValue().get() //
			));
		};
		this.getVoltageL1Channel().onSetNextValue(calculateAverageVoltage);
		this.getVoltageL2Channel().onSetNextValue(calculateAverageVoltage);
		this.getVoltageL3Channel().onSetNextValue(calculateAverageVoltage);

		// Sum Current from Current L1, L2 and L3
		final Consumer<Value<Integer>> calculateSumCurrent = ignore -> {
			this._setCurrent(TypeUtils.sum(//
					this.getCurrentL1Channel().getNextValue().get(), //
					this.getCurrentL2Channel().getNextValue().get(), //
					this.getCurrentL3Channel().getNextValue().get() //
			));
		};
		this.getCurrentL1Channel().onSetNextValue(calculateSumCurrent);
		this.getCurrentL2Channel().onSetNextValue(calculateSumCurrent);
		this.getCurrentL3Channel().onSetNextValue(calculateSumCurrent);
	}

}
