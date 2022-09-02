package io.openems.edge.app.timeofusetariff;

import java.util.EnumMap;
import java.util.List;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.timeofusetariff.AwattarHourly.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;

/**
 * Describes a App for AwattarHourly.
 *
 * <pre>
  {
    "appId":"App.TimeOfUseTariff.Awattar",
    "alias":"Awattar HOURLY",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"CTRL_ESS_TIME_OF_USE_TARIF_DISCHARGE_ID": "ctrlEssTimeOfUseTariffDischarge0",
    	"TIME_OF_USE_TARIF_ID": "timeOfUseTariff0"
    },
    "appDescriptor": {
    	"websiteUrl": URL
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.TimeOfUseTariff.Awattar")
public class AwattarHourly extends AbstractOpenemsApp<Property> implements OpenemsApp {

	public static enum Property {
		CTRL_ESS_TIME_OF_USE_TARIF_DISCHARGE_ID, //
		TIME_OF_USE_TARIF_ID, //
		;
	}

	@Activate
	public AwattarHourly(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {

			var ctrlEssTimeOfUseTariffDischargeId = this.getId(t, p, Property.CTRL_ESS_TIME_OF_USE_TARIF_DISCHARGE_ID,
					"ctrlEssTimeOfUseTariffDischarge0");

			var timeOfUseTariffId = this.getId(t, p, Property.TIME_OF_USE_TARIF_ID, "timeOfUseTariff0");

			// TODO ess id may be changed
			List<Component> comp = Lists.newArrayList(//
					new EdgeConfig.Component(ctrlEssTimeOfUseTariffDischargeId, this.getName(l),
							"Controller.Ess.Time-Of-Use-Tariff.Discharge", JsonUtils.buildJsonObject() //
									.addProperty("ess.id", "ess0") //
									.build()), //
					new EdgeConfig.Component(timeOfUseTariffId, this.getName(l), "TimeOfUseTariff.Awattar",
							JsonUtils.buildJsonObject() //
									.build())//
			);
			return new AppConfiguration(comp, Lists.newArrayList("ctrlEssTimeOfUseTariffDischarge0", "ctrlBalancing0"));
		};
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		return AppAssistant.create(this.getName(language)) //
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategorys() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.TIME_OF_USE_TARIFF };
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE_IN_CATEGORY;
	}

}
