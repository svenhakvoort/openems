package io.openems.edge.app.pvinverter;

import java.util.EnumMap;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.utils.EnumUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.pvinverter.SolarEdgePvInverter.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.JsonFormlyUtil.InputBuilder.Type;
import io.openems.edge.core.appmanager.JsonFormlyUtil.InputBuilder.Validation;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.TranslationUtil;

/**
 * Describes a App for SolarEdge PV-Inverter.
 *
 * <pre>
  {
    "appId":"App.PvInverter.SolarEdge",
    "alias":"SolarEdge PV-Wechselrichter",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"PV_INVERTER_ID": "pvInverter0",
    	"MODBUS_ID": "modbus0",
    	"IP": "192.168.178.85",
    	"PORT": "502"
    },
    "appDescriptor": {
    	"websiteUrl": URL
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.PvInverter.SolarEdge")
public class SolarEdgePvInverter extends AbstractPvInverter<Property> implements OpenemsApp {

	public static enum Property {
		// Components
		PV_INVERTER_ID, //
		MODBUS_ID, //
		// User-Values
		ALIAS, //
		IP, // the ip for the modbus
		PORT;

	}

	@Activate
	public SolarEdgePvInverter(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {

			var alias = this.getValueOrDefault(p, Property.ALIAS, this.getName(l));
			var ip = this.getValueOrDefault(p, Property.IP, "192.168.178.85");
			var port = EnumUtils.getAsInt(p, Property.PORT);

			var modbusId = this.getId(t, p, Property.MODBUS_ID, "modbus0");
			var pvInverterId = this.getId(t, p, Property.PV_INVERTER_ID, "pvInverter0");

			var factoryIdInverter = "SolarEdge.PV-Inverter";
			var components = this.getComponents(factoryIdInverter, pvInverterId, modbusId, alias, ip, port);

			return new AppConfiguration(components);
		};
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildInput(Property.IP) //
								.setLabel(TranslationUtil.getTranslation(bundle, "ipAddress")) //
								.setDescription(TranslationUtil.getTranslation(bundle, "App.PvInverter.ip.description")) //
								.setDefaultValue("192.168.178.85") //
								.isRequired(true) //
								.setValidation(Validation.IP) //
								.build()) //
						.add(JsonFormlyUtil.buildInput(Property.PORT) //
								.setLabel(TranslationUtil.getTranslation(bundle, "port")) //
								.setDescription(
										TranslationUtil.getTranslation(bundle, "App.PvInverter.port.description")) //
								.setInputType(Type.NUMBER) //
								.setDefaultValue(502) //
								.setMin(0) //
								.isRequired(true) //
								.build()) //
						.build())
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.build();
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

}
