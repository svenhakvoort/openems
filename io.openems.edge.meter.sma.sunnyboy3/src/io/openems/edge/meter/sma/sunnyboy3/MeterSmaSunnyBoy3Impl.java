package io.openems.edge.meter.sma.sunnyboy3;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;

import java.util.function.Consumer;

@Designate(ocd = Config.class, factory = true)
@Component(
		name = "Meter.SMA.sunnyboy3",
		immediate = true,
		configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class MeterSmaSunnyBoy3Impl extends AbstractOpenemsModbusComponent implements SymmetricMeter, ModbusComponent, OpenemsComponent, MeterSmaSunnyBoy3 {

	private MeterType meterType = MeterType.PRODUCTION;

	@Reference
	protected ConfigurationAdmin cm;

	public MeterSmaSunnyBoy3Impl() {
		super(
				OpenemsComponent.ChannelId.values(),
				ModbusComponent.ChannelId.values(),
				SymmetricMeter.ChannelId.values(),
				MeterSmaSunnyBoy3.ChannelId.values()
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
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id());
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
				new FC3ReadRegistersTask(30775, Priority.HIGH, m(SymmetricMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(30775))),
				new FC3ReadRegistersTask(30803, Priority.HIGH, m(MeterSmaSunnyBoy3.ChannelId.FREQUENCY_IN_HERTZ, new UnsignedDoublewordElement(30803))),
				new FC3ReadRegistersTask(30805, Priority.HIGH, m(SymmetricMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(30805)))
		);

		this.addCalculateChannelListeners();

		return modbusProtocol;
	}

	private void addCalculateChannelListeners() {
		Channel<Long> frequencyChannelInHertz = this.channel(MeterSmaSunnyBoy3.ChannelId.FREQUENCY_IN_HERTZ);
		final Consumer<Value<Long>> convertHertzToMilliHertz = value -> {
			this.getFrequencyChannel().setNextValue(value.get().intValue() / 1000);
		};

		frequencyChannelInHertz.onSetNextValue(convertHertzToMilliHertz);
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

}
