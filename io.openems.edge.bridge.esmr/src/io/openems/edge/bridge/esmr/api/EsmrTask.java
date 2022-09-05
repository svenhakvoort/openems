package io.openems.edge.bridge.esmr.api;

import io.openems.edge.common.component.AbstractOpenemsComponent;

public class EsmrTask {

    private final AbstractOpenemsComponent openemsEsmrComponent; // creator of this task instance

    private final BridgeEsmr bridgeEsmr;

	public EsmrTask(BridgeEsmr bridgeEsmr, AbstractOpenemsComponent openemsEsmrComponent) {
        this.openemsEsmrComponent = openemsEsmrComponent;
        this.bridgeEsmr = bridgeEsmr;
    }

    public void setResponse(String data) {
		System.out.println(data);
    }

}