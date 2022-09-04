package io.openems.edge.bridge.esmr;

import io.openems.edge.bridge.esmr.api.BridgeEsmr;
import io.openems.edge.bridge.esmr.api.EsmrTask;
import io.openems.edge.bridge.esmr.util.Notifiable;
import io.openems.edge.bridge.esmr.util.SerialPortReader;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Bridge.Mbus", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)
public class BridgeEsmrImpl extends AbstractOpenemsComponent implements BridgeEsmr, EventHandler, OpenemsComponent, Notifiable {

	private final Logger log = LoggerFactory.getLogger(BridgeEsmrImpl.class);

	public BridgeEsmrImpl() {
		super(//
				OpenemsComponent.ChannelId.values()
		);
	}

	private final Map<String, EsmrTask> tasks = new HashMap<>();

	private SerialPortReader serialPortReader;
	private String portName;

	@Activate
	protected void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.portName = config.portName();

		this.serialPortReader = new SerialPortReader(this.portName, config.baudrate(), config.dataBits(), config.stopBits(), config.parity());
		this.serialPortReader.initConnection();
		this.serialPortReader.addNotifiable(this);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.serialPortReader.close();
	}

	@Override
	public void handleEvent(Event event) {
	}

	@Override
	public void addTask(String sourceId, EsmrTask task) {
		this.tasks.put(sourceId, task);
	}

	@Override
	public SerialPortReader getSerialPortReader() {
		return serialPortReader;
	}

	@Override
	public void removeTask(String sourceId) {
		this.tasks.remove(sourceId);
	}

	@Override
	public void onEvent(String data) {
		for (EsmrTask task : this.tasks.values()) {
			task.setResponse(data);
		}
	}

}
