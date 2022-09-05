package io.openems.edge.bridge.esmr.api;

import nl.basjes.dsmr.DSMRTelegram;

public class EsmrTask {

    private final AbstractOpenemsEsmrComponent openemsEsmrComponent; // creator of this task instance

    private final BridgeEsmr bridgeEsmr;

	public EsmrTask(BridgeEsmr bridgeEsmr, AbstractOpenemsEsmrComponent openemsEsmrComponent) {
        this.openemsEsmrComponent = openemsEsmrComponent;
        this.bridgeEsmr = bridgeEsmr;
    }

    public void setResponse(DSMRTelegram data) {
        new ChannelDataRecordMapper(data, this.openemsEsmrComponent.getChannelDataRecordsList());
    }

}