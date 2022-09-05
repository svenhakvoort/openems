package io.openems.edge.meter.esmr;

import io.openems.edge.bridge.esmr.api.AbstractOpenemsEsmrComponent;
import io.openems.edge.bridge.esmr.api.BridgeEsmr;
import io.openems.edge.bridge.esmr.api.ChannelRecord;
import io.openems.edge.bridge.esmr.api.EsmrTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import nl.basjes.dsmr.DSMRTelegram;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;

@Designate(ocd = Config.class, factory = true)
@Component(
        name = "Meter.ESMR",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class EsmrMeter extends AbstractOpenemsEsmrComponent implements SymmetricMeter, AsymmetricMeter, OpenemsComponent {

    private MeterType meterType = MeterType.GRID;

    @Reference
    protected ConfigurationAdmin cm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected BridgeEsmr esmrBus;

    public EsmrMeter() {
        super(
                OpenemsComponent.ChannelId.values(),
                SymmetricMeter.ChannelId.values(),
                AsymmetricMeter.ChannelId.values(),
                io.openems.edge.meter.esmr.ChannelId.values()
        );
        AsymmetricMeter.initializePowerSumChannels(this);
    }

    @Activate
    void activate(ComponentContext context, Config config) {
        this.meterType = config.type();
        super.activate(context, config.id(), config.alias(), config.enabled(), cm, config.esmr_id());

        this.esmrBus.addTask(config.id(), new EsmrTask(this.esmrBus, this));
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
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1), telegram -> kiloWattToWatt(telegram.getPowerReceivedL1() - telegram.getPowerReturnedL1())));
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2), telegram -> kiloWattToWatt(telegram.getPowerReceivedL2() - telegram.getPowerReturnedL2())));
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3), telegram -> kiloWattToWatt(telegram.getPowerReceivedL3() - telegram.getPowerReturnedL3())));

        this.channelDataRecordsList.add(new ChannelRecord(this.channel(AsymmetricMeter.ChannelId.CONSUMPTION_ACTIVE_POWER_L1), telegram -> kiloWattToWatt(telegram.getPowerReceivedL1())));
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(AsymmetricMeter.ChannelId.CONSUMPTION_ACTIVE_POWER_L2), telegram -> kiloWattToWatt(telegram.getPowerReceivedL2())));
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(AsymmetricMeter.ChannelId.CONSUMPTION_ACTIVE_POWER_L3), telegram -> kiloWattToWatt(telegram.getPowerReceivedL3())));

        this.channelDataRecordsList.add(new ChannelRecord(this.channel(AsymmetricMeter.ChannelId.PRODUCTION_ACTIVE_POWER_L1), telegram -> kiloWattToWatt(telegram.getPowerReturnedL1())));
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(AsymmetricMeter.ChannelId.PRODUCTION_ACTIVE_POWER_L2), telegram -> kiloWattToWatt(telegram.getPowerReturnedL2())));
        this.channelDataRecordsList.add(new ChannelRecord(this.channel(AsymmetricMeter.ChannelId.PRODUCTION_ACTIVE_POWER_L3), telegram -> kiloWattToWatt(telegram.getPowerReturnedL3())));
    }

    private static int kiloWattToWatt(Double kiloWatt) {
        return (int) (kiloWatt * 1000);
    }

}
