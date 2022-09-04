package io.openems.edge.meter.esmr;

import io.openems.edge.bridge.mbus.api.AbstractOpenemsMbusComponent;
import io.openems.edge.bridge.mbus.api.BridgeMbus;
import io.openems.edge.bridge.mbus.api.ChannelRecord;
import io.openems.edge.bridge.mbus.api.MbusTask;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
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

import java.util.function.Consumer;

import static io.openems.edge.meter.esmr.ChannelId.TEST;
import static io.openems.edge.meter.esmr.ChannelId.TOTAL_CONSUMED_ENERGY;

@Designate(ocd = Config.class, factory = true)
@Component(
        name = "Meter.Sagemcom.ESMR",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class EsmrMeter extends AbstractOpenemsMbusComponent implements AsymmetricMeter, OpenemsComponent {

    private MeterType meterType = MeterType.GRID;

    @Reference
    protected ConfigurationAdmin cm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected BridgeMbus mbus;

    public EsmrMeter() {
        super(
                OpenemsComponent.ChannelId.values(),
                AsymmetricMeter.ChannelId.values(),
                io.openems.edge.meter.esmr.ChannelId.values()
        );
    }

    @Activate
    void activate(ComponentContext context, Config config) {
        this.meterType = config.type();
        super.activate(context, config.id(), config.alias(), config.enabled(), config.primaryAddress(), this.cm, "mbus", config.mbus_id());
        // register into mbus bridge task list
        this.mbus.addTask(config.id(), new MbusTask(this.mbus, this));
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
    protected void addChannelDataRecords() {
        var channel1 = new ChannelRecord(this.channel(TEST), 0);
        channel1.getChannel().onSetNextValue(value -> System.out.println("GRIDMETER VALUE: " + value));
        this.channelDataRecordsList.add(channel1);
    }

}
