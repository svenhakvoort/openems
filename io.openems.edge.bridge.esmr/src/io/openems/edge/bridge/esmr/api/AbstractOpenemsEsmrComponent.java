package io.openems.edge.bridge.esmr.api;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractOpenemsEsmrComponent extends AbstractOpenemsComponent {

	protected final List<ChannelRecord> channelDataRecordsList = new ArrayList<>();

	protected AbstractOpenemsEsmrComponent(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
                                           io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	public List<ChannelRecord> getChannelDataRecordsList() {
		return this.channelDataRecordsList;
	}

	protected boolean activate(ComponentContext context, String id, String alias, boolean enabled,
							   ConfigurationAdmin cm, String esmrId) {
		super.activate(context, id, alias, enabled);

		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "esmrBus", esmrId)) {
			return true;
		}
		this.addChannelDataRecords();
		return false;
	}

	protected abstract void addChannelDataRecords();
}
