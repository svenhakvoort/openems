package io.openems.edge.meter.esmr;

import io.openems.edge.bridge.esmr.api.AbstractOpenemsEsmrComponent;
import io.openems.edge.bridge.esmr.api.BridgeEsmr;
import io.openems.edge.bridge.esmr.api.ChannelRecord;
import io.openems.edge.bridge.esmr.api.EsmrTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

@Designate(ocd = Config.class, factory = true)
@Component(
        name = "Meter.ESMR",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
@EventTopics({
        EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE,
        EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE
})
public class EsmrMeter extends AbstractOpenemsEsmrComponent implements SymmetricMeter, AsymmetricMeter, OpenemsComponent, TimedataProvider, EventHandler {

    private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this, SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
    private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this, SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);
    @Reference
    protected ConfigurationAdmin cm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected BridgeEsmr esmrBus;
    private MeterType meterType = MeterType.GRID;
    @Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
    private volatile Timedata timedata = null;

    public EsmrMeter() {
        super(
                OpenemsComponent.ChannelId.values(),
                SymmetricMeter.ChannelId.values(),
                AsymmetricMeter.ChannelId.values(),
                io.openems.edge.meter.esmr.ChannelId.values()
        );
        AsymmetricMeter.initializePowerSumChannels(this);
    }

    private static int kiloWattToWatt(Double kiloWatt) {
        return (int) (kiloWatt * 1000);
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

    @Override
    public void handleEvent(Event event) {
        if (!this.isEnabled()) {
            return;
        }
        switch (event.getTopic()) {
            case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
                break;
            case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
                this.calculateEnergy();
                break;
        }
    }

    private void calculateEnergy() {
        try {
            // Calculate Energy
            int activePowerL1 = this.getActivePowerL1().get();
            int activePowerL2 = this.getActivePowerL2().get();
            int activePowerL3 = this.getActivePowerL3().get();

            int activePower = activePowerL1 + activePowerL2 + activePowerL3;
            if (activePower > 0) {
                // Buy-From-Grid
                this.calculateProductionEnergy.update(activePower);
                this.calculateConsumptionEnergy.update(0);
            } else {
                // Sell-To-Grid
                this.calculateProductionEnergy.update(0);
                this.calculateConsumptionEnergy.update(activePower * -1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Timedata getTimedata() {
        return this.timedata;
    }

}
