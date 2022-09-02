package io.openems.edge.app.heat;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

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
import io.openems.edge.app.heat.HeatingElement.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.DefaultEnum;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;
import io.openems.edge.core.appmanager.dependency.DependencyUtil;
import io.openems.edge.core.appmanager.validator.CheckRelayCount;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;

/**
 * Describes a App for a RTU Heating Element.
 *
 * <pre>
  {
    "appId":"App.Heat.HeatingElement",
    "alias":"Heizstab",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"CTRL_IO_HEATING_ELEMENT_ID": "ctrlIoHeatingElement0",
    	"OUTPUT_CHANNEL_PHASE_L1": "io0/Relay1",
    	"OUTPUT_CHANNEL_PHASE_L2": "io0/Relay2",
    	"OUTPUT_CHANNEL_PHASE_L3": "io0/Relay3"
    },
    "dependencies": [
    	{
        	"key": "RELAY",
        	"instanceId": UUID
    	}
    ],
    "appDescriptor": {
    	"websiteUrl": URL
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.Heat.HeatingElement")
public class HeatingElement extends AbstractOpenemsApp<Property> implements OpenemsApp {

	public static enum Property implements DefaultEnum {
		ALIAS("Heating Element App"), //
		CTRL_IO_HEATING_ELEMENT_ID("ctrlIoHeatingElement0"), //
		OUTPUT_CHANNEL_PHASE_L1("io0/Relay1"), //
		OUTPUT_CHANNEL_PHASE_L2("io0/Relay2"), //
		OUTPUT_CHANNEL_PHASE_L3("io0/Relay3"), //
		;

		private final String defaultValue;

		private Property(String defaultValue) {
			this.defaultValue = defaultValue;
		}

		@Override
		public String getDefaultValue() {
			return this.defaultValue;
		}

	}

	@Activate
	public HeatingElement(@Reference ComponentManager componentManager, ComponentContext componentContext,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {

			final var heatingElementId = this.getId(t, p, Property.CTRL_IO_HEATING_ELEMENT_ID);

			final var alias = this.getValueOrDefault(p, Property.ALIAS, this.getName(l));
			final var outputChannelPhaseL1 = this.getValueOrDefault(p, Property.OUTPUT_CHANNEL_PHASE_L1);
			final var outputChannelPhaseL2 = this.getValueOrDefault(p, Property.OUTPUT_CHANNEL_PHASE_L2);
			final var outputChannelPhaseL3 = this.getValueOrDefault(p, Property.OUTPUT_CHANNEL_PHASE_L3);

			List<Component> comp = new ArrayList<>();
			var jsonConfigBuilder = JsonUtils.buildJsonObject();

			comp.add(new EdgeConfig.Component(heatingElementId, alias, "Controller.IO.HeatingElement",
					jsonConfigBuilder.addProperty("outputChannelPhaseL1", outputChannelPhaseL1) //
							.addProperty("outputChannelPhaseL2", outputChannelPhaseL2) //
							.addProperty("outputChannelPhaseL3", outputChannelPhaseL3) //
							.build()));//

			var componentIdOfRelay = outputChannelPhaseL1.substring(0, outputChannelPhaseL1.indexOf('/'));
			var appIdOfRelay = DependencyUtil.getInstanceIdOfAppWhichHasComponent(this.componentManager,
					componentIdOfRelay);

			if (appIdOfRelay == null) {
				// relay may be created but not as a app
				return new AppConfiguration(comp);
			}

			var dependencies = Lists.newArrayList(new DependencyDeclaration("RELAY", //
					DependencyDeclaration.CreatePolicy.NEVER, //
					DependencyDeclaration.UpdatePolicy.NEVER, //
					DependencyDeclaration.DeletePolicy.NEVER, //
					DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ALL, //
					DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED, //
					DependencyDeclaration.AppDependencyConfig.create() //
							.setSpecificInstanceId(appIdOfRelay) //
							.build()));

			return new AppConfiguration(comp, null, null, dependencies);
		};
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		var relays = this.componentUtil.getPreferredRelays(Lists.newArrayList(), new int[] { 1, 2, 3 },
				new int[] { 4, 5, 6 });
		var options = this.componentUtil.getAllRelays() //
				.stream().map(r -> r.relays).flatMap(List::stream) //
				.collect(Collectors.toList());
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildSelect(Property.OUTPUT_CHANNEL_PHASE_L1) //
								.setOptions(options) //
								.onlyIf(relays != null, t -> t.setDefaultValue(relays[0])) //
								.setLabel(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".outputChannelPhaseL1.label"))
								.setDescription(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".outputChannelPhaseL1.description"))
								.build())
						.add(JsonFormlyUtil.buildSelect(Property.OUTPUT_CHANNEL_PHASE_L2) //
								.setOptions(options) //
								.onlyIf(relays != null, t -> t.setDefaultValue(relays[1])) //
								.setLabel(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".outputChannelPhaseL2.label"))
								.setDescription(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".outputChannelPhaseL2.description"))
								.build())
						.add(JsonFormlyUtil.buildSelect(Property.OUTPUT_CHANNEL_PHASE_L3) //
								.setOptions(options) //
								.onlyIf(relays != null, t -> t.setDefaultValue(relays[2])) //
								.setLabel(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".outputChannelPhaseL3.label"))
								.setDescription(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".outputChannelPhaseL3.description"))
								.build())
						.build())
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategorys() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.HEAT };
	}

	@Override
	public ValidatorConfig.Builder getValidateBuilder() {
		return ValidatorConfig.create() //
				.setInstallableCheckableConfigs(Lists.newArrayList(//
						new ValidatorConfig.CheckableConfig(CheckRelayCount.COMPONENT_NAME,
								new ValidatorConfig.MapBuilder<>(new TreeMap<String, Object>()) //
										.put("count", 3) //
										.build())));
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

}
