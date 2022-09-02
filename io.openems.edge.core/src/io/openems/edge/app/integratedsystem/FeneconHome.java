package io.openems.edge.app.integratedsystem;

import static io.openems.edge.core.appmanager.ConfigurationTarget.VALIDATE;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.EnumUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.integratedsystem.FeneconHome.Property;
import io.openems.edge.app.meter.SocomecMeter;
import io.openems.edge.app.pvselfconsumption.GridOptimizedCharge;
import io.openems.edge.app.pvselfconsumption.SelfConsumptionOptimization;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.JsonFormlyUtil.InputBuilder.Type;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;

/**
 * Describes a FENECON Home energy storage system.
 *
 * <pre>
  {
    "appId":"App.FENECON.Home",
    "alias":"FENECON Home",
    "instanceId": UUID,
    "image": base64,
    "properties":{
      "SAFETY_COUNTRY":"AUSTRIA",
      "RIPPLE_CONTROL_RECEIVER_ACTIV":false,
      "MAX_FEED_IN_POWER":5000,
      "FEED_IN_SETTING":"PU_ENABLE_CURVE",
      "HAS_AC_METER":true,
      "HAS_DC_PV1":true,
      "DC_PV1_ALIAS":"PV 1",
      "HAS_DC_PV2":true,
      "DC_PV2_ALIAS":"PV 2",
      "HAS_EMERGENCY_RESERVE":true,
      "EMERGENCY_RESERVE_ENABLED":true,
      "EMERGENCY_RESERVE_SOC":20
    },
    "dependencies": [
    	{
        	"key": "GRID_OPTIMIZED_CHARGE",
        	"instanceId": UUID
    	},
    	{
        	"key": "AC_METER",
        	"instanceId": UUID
    	},
    	{
        	"key": "SELF_CONSUMTION_OPTIMIZATION",
        	"instanceId": UUID
    	}
    ],
    "appDescriptor": {
    	"websiteUrl": URL
    }
  }
 * </pre>
 */
@Component(name = "App.FENECON.Home")
public class FeneconHome extends AbstractOpenemsApp<Property> implements OpenemsApp {

	public static enum Property {
		// Battery Inverter
		SAFETY_COUNTRY, //
		MAX_FEED_IN_POWER, //
		FEED_IN_SETTING, //

		// (ger. Rundsteuerempfänger)
		RIPPLE_CONTROL_RECEIVER_ACTIV, //

		// External AC PV
		HAS_AC_METER, //

		// DC PV Charger 1
		HAS_DC_PV1, //
		DC_PV1_ALIAS, //

		// DC PV Charger 2
		HAS_DC_PV2, //
		DC_PV2_ALIAS, //

		// Emergency Reserve SoC
		HAS_EMERGENCY_RESERVE, //
		EMERGENCY_RESERVE_ENABLED, //
		EMERGENCY_RESERVE_SOC, //

		// Shadow management
		SHADOW_MANAGEMENT_DISABLED, //
		;
	}

	@Activate
	public FeneconHome(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.build();
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, //
			AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {
			var essId = "ess0";
			var modbusIdInternal = "modbus0";
			var modbusIdExternal = "modbus1";

			var hasEmergencyReserve = EnumUtils.getAsOptionalBoolean(p, Property.HAS_EMERGENCY_RESERVE).orElse(false);
			var emergencyReserveEnabled = EnumUtils.getAsOptionalBoolean(p, Property.EMERGENCY_RESERVE_ENABLED)
					.orElse(false);
			var rippleControlReceiverActive = EnumUtils.getAsOptionalBoolean(p, Property.RIPPLE_CONTROL_RECEIVER_ACTIV)
					.orElse(false);
			var shadowManagmentDisabled = EnumUtils.getAsOptionalBoolean(p, Property.SHADOW_MANAGEMENT_DISABLED)
					.orElse(false);

			// Battery-Inverter Settings
			var safetyCountry = EnumUtils.getAsString(p, Property.SAFETY_COUNTRY);
			int maxFeedInPower;
			String feedInSetting;
			if (!rippleControlReceiverActive) {
				maxFeedInPower = EnumUtils.getAsInt(p, Property.MAX_FEED_IN_POWER);
				feedInSetting = EnumUtils.getAsString(p, Property.FEED_IN_SETTING);
			} else {
				maxFeedInPower = 0;
				feedInSetting = "UNDEFINED";
			}

			var bundle = AbstractOpenemsApp.getTranslationBundle(l);
			var components = Lists.newArrayList(//
					new EdgeConfig.Component(modbusIdInternal,
							TranslationUtil.getTranslation(bundle, this.getAppId() + "." + modbusIdInternal + ".alias"),
							"Bridge.Modbus.Serial", JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("portName", "/dev/busUSB1") //
									.addProperty("baudRate", 19200) //
									.addProperty("databits", 8) //
									.addProperty("stopbits", "ONE") //
									.addProperty("parity", "NONE") //
									.addProperty("logVerbosity", "NONE") //
									.onlyIf(t == ConfigurationTarget.ADD, //
											j -> j.addProperty("invalidateElementsAfterReadErrors", 1) //
			).build()),
					new EdgeConfig.Component(modbusIdExternal,
							TranslationUtil.getTranslation(bundle, this.getAppId() + "." + modbusIdExternal + ".alias"),
							"Bridge.Modbus.Serial", JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("portName", "/dev/busUSB2") //
									.addProperty("baudRate", 9600) //
									.addProperty("databits", 8) //
									.addProperty("stopbits", "ONE") //
									.addProperty("parity", "NONE") //
									.addProperty("logVerbosity", "NONE") //
									.addProperty("invalidateElementsAfterReadErrors", 1) //
									.build()),
					new EdgeConfig.Component("meter0",
							TranslationUtil.getTranslation(bundle, this.getAppId() + ".meter0.alias"),
							"GoodWe.Grid-Meter", //
							JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("modbus.id", modbusIdExternal) //
									.addProperty("modbusUnitId", 247) //
									.build()),
					new EdgeConfig.Component("io0",
							TranslationUtil.getTranslation(bundle, this.getAppId() + ".io0.alias"), "IO.KMtronic.4Port", //
							JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("modbus.id", modbusIdInternal) //
									.addProperty("modbusUnitId", 2) //
									.build()),
					new EdgeConfig.Component("battery0",
							TranslationUtil.getTranslation(bundle, this.getAppId() + ".battery0.alias"),
							"Battery.Fenecon.Home", //
							JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("startStop", "AUTO") //
									.addProperty("modbus.id", modbusIdInternal) //
									.addProperty("modbusUnitId", 1) //
									.addProperty("batteryStartUpRelay", "io0/Relay4") //
									.build()),
					new EdgeConfig.Component("batteryInverter0",
							TranslationUtil.getTranslation(bundle, this.getAppId() + ".batteryInverter0.alias"),
							"GoodWe.BatteryInverter", JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("modbus.id", modbusIdExternal) //
									.addProperty("modbusUnitId", 247) //
									.addProperty("safetyCountry", safetyCountry) //
									.addProperty("backupEnable", //
											hasEmergencyReserve ? "ENABLE" : "DISABLE") //
									.addProperty("feedPowerEnable", rippleControlReceiverActive ? "DISABLE" : "ENABLE") //
									.addProperty("feedPowerPara", maxFeedInPower) //
									.addProperty("setfeedInPowerSettings", feedInSetting) //
									.addProperty("mpptForShadowEnable", shadowManagmentDisabled ? "DISABLE" : "ENABLE") //
									.build()),
					new EdgeConfig.Component(essId,
							TranslationUtil.getTranslation(bundle, this.getAppId() + "." + essId + ".alias"),
							"Ess.Generic.ManagedSymmetric", JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("startStop", "START") //
									.addProperty("batteryInverter.id", "batteryInverter0") //
									.addProperty("battery.id", "battery0") //
									.build()),
					new EdgeConfig.Component("predictor0",
							TranslationUtil.getTranslation(bundle, this.getAppId() + ".predictor0.alias"),
							"Predictor.PersistenceModel", JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.add("channelAddresses", JsonUtils.buildJsonArray() //
											.add("_sum/ProductionActivePower") //
											.add("_sum/ConsumptionActivePower") //
											.build()) //
									.build()),
					new EdgeConfig.Component("ctrlEssSurplusFeedToGrid0",
							TranslationUtil.getTranslation(bundle,
									this.getAppId() + ".ctrlEssSurplusFeedToGrid0.alias"),
							"Controller.Ess.Hybrid.Surplus-Feed-To-Grid", JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.addProperty("ess.id", essId) //
									.build()) //
			);

			if (EnumUtils.getAsOptionalBoolean(p, Property.HAS_DC_PV1).orElse(false)) {
				var alias = EnumUtils.getAsOptionalString(p, Property.DC_PV1_ALIAS).orElse("DC-PV 1");
				components.add(new EdgeConfig.Component("charger0", alias, "GoodWe.Charger-PV1", //
						JsonUtils.buildJsonObject() //
								.addProperty("enabled", true) //
								.addProperty("essOrBatteryInverter.id", "batteryInverter0") //
								.addProperty("modbus.id", modbusIdExternal) //
								.addProperty("modbusUnitId", 247) //
								.build()));
			}

			if (EnumUtils.getAsOptionalBoolean(p, Property.HAS_DC_PV2).orElse(false)) {
				var alias = EnumUtils.getAsOptionalString(p, Property.DC_PV2_ALIAS).orElse("DC-PV 2");
				components.add(new EdgeConfig.Component("charger1", alias, "GoodWe.Charger-PV2", //
						JsonUtils.buildJsonObject() //
								.addProperty("enabled", true) //
								.addProperty("essOrBatteryInverter.id", "batteryInverter0") //
								.addProperty("modbus.id", modbusIdExternal) //
								.addProperty("modbusUnitId", 247) //
								.build()));
			}

			if (hasEmergencyReserve) {
				components.add(new EdgeConfig.Component("meter2",
						TranslationUtil.getTranslation(bundle, this.getAppId() + ".meter2.alias"),
						"GoodWe.EmergencyPowerMeter", //
						JsonUtils.buildJsonObject() //
								.addProperty("enabled", true) //
								.addProperty("modbus.id", modbusIdExternal) //
								.addProperty("modbusUnitId", 247) //
								.build()));

				// use 5(minimum value) as reserveSoc if emergencyReserveEnabled is not enabled
				var emergencyReserveSoc = EnumUtils.getAsOptionalInt(p, Property.EMERGENCY_RESERVE_SOC).orElse(5);
				components.add(new EdgeConfig.Component("ctrlEmergencyCapacityReserve0",
						TranslationUtil.getTranslation(bundle,
								this.getAppId() + ".ctrlEmergencyCapacityReserve0.alias"),
						"Controller.Ess.EmergencyCapacityReserve", //
						JsonUtils.buildJsonObject() //
								.addProperty("enabled", true) //
								.addProperty("ess.id", essId) //
								.onlyIf(t != VALIDATE,
										b -> b.addProperty("isReserveSocEnabled", emergencyReserveEnabled)) //
								.onlyIf(t != VALIDATE, b -> b.addProperty("reserveSoc", emergencyReserveSoc)) //
								.build()));
			}

			var hasAcMeter = EnumUtils.getAsOptionalBoolean(p, Property.HAS_AC_METER).orElse(false);

			// remove components that were in the old configuration but now are a dependency
			if (t == ConfigurationTarget.DELETE) {
				components.add(new EdgeConfig.Component("ctrlGridOptimizedCharge0", "",
						"Controller.Ess.GridOptimizedCharge", JsonUtils.buildJsonObject().build()));
				components.add(new EdgeConfig.Component("ctrlBalancing0", "", "Controller.Symmetric.Balancing",
						JsonUtils.buildJsonObject().build()));
				if (hasAcMeter) {
					components.add(new EdgeConfig.Component("meter1", "", "Meter.Socomec.Threephase",
							JsonUtils.buildJsonObject().build()));
				}
			}

			/*
			 * Set Execution Order for Scheduler.
			 */
			List<String> schedulerExecutionOrder = new ArrayList<>();
			if (hasEmergencyReserve) {
				schedulerExecutionOrder.add("ctrlEmergencyCapacityReserve0");
			}
			schedulerExecutionOrder.add("ctrlGridOptimizedCharge0");
			schedulerExecutionOrder.add("ctrlEssSurplusFeedToGrid0");
			schedulerExecutionOrder.add("ctrlBalancing0");

			var dependencies = Lists.newArrayList(new DependencyDeclaration("GRID_OPTIMIZED_CHARGE", //
					DependencyDeclaration.CreatePolicy.IF_NOT_EXISTING, //
					DependencyDeclaration.UpdatePolicy.ALWAYS, //
					DependencyDeclaration.DeletePolicy.IF_MINE, //
					DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ONLY_UNCONFIGURED_PROPERTIES, //
					DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED, //
					DependencyDeclaration.AppDependencyConfig.create() //
							.setAppId("App.PvSelfConsumption.GridOptimizedCharge") //
							.setProperties(JsonUtils.buildJsonObject() //
									.addProperty(GridOptimizedCharge.Property.SELL_TO_GRID_LIMIT_ENABLED.name(),
											!rippleControlReceiverActive) //
									.addProperty(GridOptimizedCharge.Property.MAXIMUM_SELL_TO_GRID_POWER.name(),
											maxFeedInPower) //
									.build())
							.build()),
					new DependencyDeclaration("SELF_CONSUMPTION_OPTIMIZATION", //
							DependencyDeclaration.CreatePolicy.IF_NOT_EXISTING, //
							DependencyDeclaration.UpdatePolicy.NEVER, //
							DependencyDeclaration.DeletePolicy.IF_MINE, //
							DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ONLY_UNCONFIGURED_PROPERTIES, //
							DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED, //
							DependencyDeclaration.AppDependencyConfig.create() //
									.setAppId("App.PvSelfConsumption.SelfConsumptionOptimization") //
									.setProperties(JsonUtils.buildJsonObject() //
											.addProperty(SelfConsumptionOptimization.Property.ESS_ID.name(), essId) //
											.addProperty(SelfConsumptionOptimization.Property.METER_ID.name(), "meter0") //
											.build())
									.build()) //
			);

			if (hasAcMeter) {
				dependencies.add(new DependencyDeclaration("AC_METER", //
						DependencyDeclaration.CreatePolicy.ALWAYS, //
						DependencyDeclaration.UpdatePolicy.ALWAYS, //
						DependencyDeclaration.DeletePolicy.IF_MINE, //
						DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ONLY_UNCONFIGURED_PROPERTIES, //
						DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED, //
						DependencyDeclaration.AppDependencyConfig.create() //
								.setAppId("App.Meter.Socomec") //
								.setInitialProperties(JsonUtils.buildJsonObject() //
										.addProperty(SocomecMeter.Property.TYPE.name(), "PRODUCTION") //
										.addProperty(SocomecMeter.Property.MODBUS_ID.name(), modbusIdExternal) //
										.addProperty(SocomecMeter.Property.MODBUS_UNIT_ID.name(), 6) //
										.build())
								.setProperties(JsonUtils.buildJsonObject() //
										.addProperty(SocomecMeter.Property.MODBUS_ID.name(), modbusIdExternal) //
										.build())
								.build()));
			}

			return new AppConfiguration(components, schedulerExecutionOrder, null, dependencies);
		};
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		final var batteryInverter = this.getBatteryInverter();
		final var emergencyController = this.componentUtil.getComponent("ctrlEmergencyCapacityReserve0", //
				"Controller.Ess.EmergencyCapacityReserve");
		final var emergencyReserveEnabled = emergencyController.map(EdgeConfig.Component::getProperties)
				.map(t -> t.get("isReserveSocEnabled")).map(JsonElement::getAsBoolean).orElse(false);
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildSelect(Property.SAFETY_COUNTRY) //
								.setLabel(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".safetyCountry.label")) //
								.isRequired(true) //
								.setOptions(JsonUtils.buildJsonArray() //
										.add(JsonUtils.buildJsonObject() //
												.addProperty("label", //
														TranslationUtil.getTranslation(bundle, "germany")) //
												.addProperty("value", "GERMANY") //
												.build()) //
										.add(JsonUtils.buildJsonObject() //
												.addProperty("label", //
														TranslationUtil.getTranslation(bundle, "austria")) //
												.addProperty("value", "AUSTRIA") //
												.build()) //
										.add(JsonUtils.buildJsonObject() //
												.addProperty("label", //
														TranslationUtil.getTranslation(bundle, "switzerland")) //
												.addProperty("value", "SWITZERLAND") //
												.build()) //
										.build()) //
								.onlyIf(batteryInverter.isPresent(), f -> {
									f.setDefaultValue(batteryInverter.get() //
											.getProperty("safetyCountry").get().getAsString());
								}).build())
						.add(JsonFormlyUtil.buildCheckbox(Property.RIPPLE_CONTROL_RECEIVER_ACTIV) //
								.setLabel(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".rippleControlReceiver.label"))
								.setDescription(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".rippleControlReceiver.description"))
								.setDefaultValue(false) //
								.onlyIf(batteryInverter.isPresent(), t -> {
									var defaultValue = batteryInverter.get().getProperty("feedPowerEnable")
											.map(j -> JsonUtils.getAsOptionalString(j).get()).orElse("ENABLE")
											.equals("DISABLE");
									t.setDefaultValue(defaultValue);
								}).build())
						.add(JsonFormlyUtil.buildInput(Property.MAX_FEED_IN_POWER) //
								.setLabel(
										TranslationUtil.getTranslation(bundle, this.getAppId() + ".feedInLimit.label")) //
								.isRequired(true) //
								.onlyShowIfNotChecked(Property.RIPPLE_CONTROL_RECEIVER_ACTIV) //
								.setInputType(Type.NUMBER) //
								.setDefaultValue(0) //
								.onlyIf(batteryInverter.isPresent(), f -> {
									f.setDefaultValue(batteryInverter.get() //
											.getProperty("feedPowerPara").get());
								}).build())
						.add(JsonFormlyUtil.buildSelect(Property.FEED_IN_SETTING) //
								.setLabel(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".feedInSettings.label")) //
								.isRequired(true) //
								.setDefaultValue("UNDEFINED") //
								.onlyShowIfNotChecked(Property.RIPPLE_CONTROL_RECEIVER_ACTIV) //
								.setOptions(this.getFeedInSettingsOptions(), t -> t, t -> t) //
								.onlyIf(batteryInverter.isPresent(), f -> {
									f.setDefaultValue(batteryInverter.get() //
											.getProperty("setfeedInPowerSettings") //
											.get().getAsString());
								}).build())
						.add(JsonFormlyUtil.buildCheckbox(Property.HAS_AC_METER) //
								.setLabel(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".hasAcMeterSocomec.label")) //
								.isRequired(true) //
								.setDefaultValue(this.componentUtil //
										.getComponent("meter1", "Meter.Socomec.Threephase") //
										.isPresent()) //
								.build())
						.add(JsonFormlyUtil.buildCheckbox(Property.HAS_DC_PV1) //
								.setLabel(TranslationUtil.getTranslation(bundle, this.getAppId() + ".hasDcPV1.label")) //
								.isRequired(true) //
								.setDefaultValue(this.componentUtil //
										.getComponent("charger0", "GoodWe.Charger-PV1").isPresent())
								.build())
						.add(JsonFormlyUtil.buildInput(Property.DC_PV1_ALIAS) //
								.setLabel("DC-PV 1 Alias") //
								.setDefaultValue("DC-PV1") //
								.onlyShowIfChecked(Property.HAS_DC_PV1) //
								.onlyIf(this.componentUtil.getComponent("charger0", "GoodWe.Charger-PV1").isPresent(),
										j -> j.setDefaultValueWithStringSupplier(() -> {
											var charger = this.componentUtil //
													.getComponent("charger0", "GoodWe.Charger-PV1");
											if (charger.isEmpty()) {
												return null;
											}
											return charger.get().getAlias();
										}))
								.build())
						.add(JsonFormlyUtil.buildCheckbox(Property.HAS_DC_PV2) //
								.setLabel(TranslationUtil.getTranslation(bundle, this.getAppId() + ".hasDcPV2.label")) //
								.isRequired(true) //
								.setDefaultValue(this.componentUtil //
										.getComponent("charger1", "GoodWe.Charger-PV2").isPresent())
								.build())
						.add(JsonFormlyUtil.buildInput(Property.DC_PV2_ALIAS) //
								.setLabel("DC-PV 2 Alias") //
								.setDefaultValue("DC-PV2") //
								.onlyShowIfChecked(Property.HAS_DC_PV2) //
								.onlyIf(this.componentUtil.getComponent("charger1", "GoodWe.Charger-PV2").isPresent(),
										j -> j.setDefaultValueWithStringSupplier(() -> {
											var charger = this.componentUtil //
													.getComponent("charger1", "GoodWe.Charger-PV2");
											if (charger.isEmpty()) {
												return null;
											}
											return charger.get().getAlias();
										}))
								.build())
						.add(JsonFormlyUtil.buildCheckbox(Property.HAS_EMERGENCY_RESERVE) //
								.setLabel(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".emergencyPowerSupply.label")) //
								.isRequired(true) //
								.setDefaultValue(emergencyController.isPresent()) //
								.build())
						.add(JsonFormlyUtil.buildCheckbox(Property.EMERGENCY_RESERVE_ENABLED) //
								.setLabel(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".emergencyPowerEnergy.label")) //
								.setDefaultValue(emergencyReserveEnabled) //
								.onlyShowIfChecked(Property.HAS_EMERGENCY_RESERVE) //
								.build())
						.add(JsonFormlyUtil.buildInput(Property.EMERGENCY_RESERVE_SOC) //
								.setLabel(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".reserveEnergy.label")) //
								.setInputType(Type.NUMBER) //
								.setMin(0) //
								.setMax(100) //
								.setDefaultValue(5) //
								.onlyShowIfChecked(Property.EMERGENCY_RESERVE_ENABLED) //
								// TODO only gets shown when toggling the checkbox 'EMERGENCY_RESERVE_ENABLED'
								// twice
								.onlyIf(emergencyReserveEnabled, f -> { //
									f.setDefaultValue(
											emergencyController.get().getProperty("reserveSoc").get().getAsNumber());
								}) //
								.build())
						.add(JsonFormlyUtil.buildCheckbox(Property.SHADOW_MANAGEMENT_DISABLED) //
								.setLabel(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".shadowManagementDisabled.label")) //
								.setDescription(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".shadowManagementDisabled.description")) //
								.build())
						.build()) //
				.build();
	}

	private final Optional<EdgeConfig.Component> getBatteryInverter() {
		var batteryInverter = this.componentManager.getEdgeConfig().getComponent("batteryInverter0");
		if (batteryInverter.isPresent() //
				&& !batteryInverter.get().getFactoryId().equals("GoodWe.BatteryInverter")) {
			batteryInverter = Optional.empty();
		}
		return batteryInverter;
	}

	@Override
	public OpenemsAppCategory[] getCategorys() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.INTEGRATED_SYSTEM };
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE_IN_CATEGORY;
	}

	private List<String> getFeedInSettingsOptions() {
		var options = new ArrayList<String>(45);
		options.add("UNDEFINED");
		options.add("QU_ENABLE_CURVE");
		options.add("PU_ENABLE_CURVE");
		// LAGGING_0_99 - LAGGING_0_80
		for (var i = 99; i >= 80; i--) {
			options.add("LAGGING_0_" + Integer.toString(i));
		}
		// LEADING_0_80 - LEADING_0_99
		for (var i = 80; i < 100; i++) {
			options.add("LEADING_0_" + Integer.toString(i));
		}
		options.add("LEADING_1");
		return options;
	}

}
