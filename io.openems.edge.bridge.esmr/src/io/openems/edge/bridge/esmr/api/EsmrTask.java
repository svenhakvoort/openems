package io.openems.edge.bridge.esmr.api;

import gnu.io.*;
import io.openems.edge.bridge.esmr.util.ReadUTF8RecordStream;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import org.openmuc.jmbus.VariableDataStructure;

import java.io.*;
import java.util.TooManyListenersException;

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