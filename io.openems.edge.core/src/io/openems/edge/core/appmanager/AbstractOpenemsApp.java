package io.openems.edge.core.appmanager;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingBiFunction;
import io.openems.common.function.ThrowingFunction;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.dependency.Dependency;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;
import io.openems.edge.core.appmanager.validator.CheckCardinality;
import io.openems.edge.core.appmanager.validator.Checkable;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;
import io.openems.edge.core.host.NetworkInterface.Inet4AddressWithNetmask;

public abstract class AbstractOpenemsApp<PROPERTY extends Enum<PROPERTY>> implements OpenemsApp {

	protected final ComponentManager componentManager;
	protected final ConfigurationAdmin cm;
	protected final ComponentContext componentContext;
	protected final ComponentUtil componentUtil;

	protected AbstractOpenemsApp(ComponentManager componentManager, ComponentContext componentContext,
			ConfigurationAdmin cm, ComponentUtil componentUtil) {
		this.componentManager = componentManager;
		this.componentContext = componentContext;
		this.cm = cm;
		this.componentUtil = componentUtil;
	}

	/**
	 * Provides a factory for {@link AppConfiguration}s.
	 *
	 * @return a {@link ThrowingFunction} that creates a {@link AppConfiguration}
	 *         from a {@link EnumMap} of configuration properties for a given
	 *         {@link ConfigurationTarget}.
	 */
	protected abstract ThrowingTriFunction<//
			ConfigurationTarget, // ADD, UPDATE, VALIDATE, DELETE or TEST
			EnumMap<PROPERTY, JsonElement>, // configuration properties
			Language, // the language
			AppConfiguration, // return value of the function
			OpenemsNamedException> // Exception on error
			appConfigurationFactory();

	protected final void assertCheckables(ConfigurationTarget t, Checkable... checkables) throws OpenemsNamedException {
		if (t != ConfigurationTarget.ADD && t != ConfigurationTarget.UPDATE) {
			return;
		}
		final List<String> errors = new ArrayList<>();
		for (Checkable checkable : checkables) {
			if (!checkable.check()) {
				errors.add(checkable.getErrorMessage(Language.DEFAULT));
			}
		}
		if (!errors.isEmpty()) {
			throw new OpenemsException(errors.stream().collect(Collectors.joining(";")));
		}
	}

	/**
	 * Gets the {@link AppConfiguration} for the given properties.
	 *
	 * @param errors              a collection of validation errors
	 * @param configurationTarget the target of the configuration
	 * @param language            the language of the configuration
	 * @param properties          the configured App properties
	 * @return the {@link AppConfiguration} or null
	 */
	private AppConfiguration configuration(ArrayList<String> errors, ConfigurationTarget configurationTarget,
			Language language, EnumMap<PROPERTY, JsonElement> properties) {
		try {
			return this.appConfigurationFactory().apply(configurationTarget, properties, language);
		} catch (OpenemsNamedException e) {
			errors.add(e.getMessage());
			return null;
		}
	}

	/**
	 * Convert JsonObject with Properties to EnumMap.
	 *
	 * @param errors     a collection of validation errors
	 * @param properties the configured App properties
	 * @return a typed {@link EnumMap} of Properties
	 */
	private EnumMap<PROPERTY, JsonElement> convertToEnumMap(ArrayList<String> errors, JsonObject properties) {
		var clazz = this.getPropertyClass();
		var result = new EnumMap<PROPERTY, JsonElement>(clazz);
		var unknownProperties = new ArrayList<String>();
		for (Entry<String, JsonElement> entry : properties.entrySet()) {
			final PROPERTY key;
			try {
				key = Enum.valueOf(clazz, entry.getKey());
			} catch (IllegalArgumentException e) {
				// ignore ALIAS if passed but not used
				if (!entry.getKey().equals("ALIAS")) {
					unknownProperties.add(entry.getKey());
				}
				continue;
			}
			result.put(key, entry.getValue());
		}
		if (!unknownProperties.isEmpty()) {
			errors.add("Unknown Configuration Propert" //
					+ (unknownProperties.size() > 1 ? "ies" : "y") + ":" //
					+ unknownProperties.stream().collect(Collectors.joining(",")));
		}
		return result;
	}

	@Override
	public AppConfiguration getAppConfiguration(ConfigurationTarget target, JsonObject config, Language language)
			throws OpenemsNamedException {
		var errors = new ArrayList<String>();
		var enumMap = this.convertToEnumMap(target != ConfigurationTarget.TEST ? errors : new ArrayList<>(), config);
		var c = this.configuration(errors, target, language, enumMap);

		// TODO remove and maybe add @AttributeDefinition above enums
		// this is for removing passwords so they do not get saved
		if (config.size() != enumMap.size()) {
			// remove entries that got removed
			var toRemoveKeys = new LinkedList<String>();
			for (var configEntry : config.entrySet()) {
				var key = configEntry.getKey();
				var contains = false;
				for (var entry : enumMap.entrySet()) {
					if (entry.getKey().name().equals(key)) {
						contains = true;
						break;
					}
				}
				if (!contains) {
					toRemoveKeys.add(key);
				}
			}
			for (var key : toRemoveKeys) {
				config.remove(key);
			}
		}

		if (!errors.isEmpty()) {
			throw new OpenemsException(errors.stream().collect(Collectors.joining("|")));
		}
		return c;
	}

	@Override
	public String getAppId() {
		return this.componentContext.getProperties().get(ComponentConstants.COMPONENT_NAME).toString();
	}

	/**
	 * Gets the id of the map with the given DefaultEnum
	 *
	 * <p>
	 * e. g. defaultValue: "ess0" => the next available id with the base-name "ess"
	 * and the the next available number
	 *
	 * @param t   the configuration target
	 * @param map the configuration map
	 * @param key the key to be searched for
	 * @return the found id
	 */
	protected String getId(ConfigurationTarget t, EnumMap<PROPERTY, JsonElement> map, DefaultEnum key) {
		try {
			return this.getId(t, map, Enum.valueOf(this.getPropertyClass(), key.name()), key.getDefaultValue());
		} catch (IllegalArgumentException ex) {
			// not a enum of property
		}
		return key.getDefaultValue();
	}

	/**
	 * Gets the id of the map with the given default id
	 *
	 * <p>
	 * e. g. defaultId: "ess0" => the next available id with the base-name "ess" and
	 * the the next available number
	 *
	 * @param t         the configuration target
	 * @param map       the configuration map
	 * @param p         the Property which stores the id
	 * @param defaultId the defaultId to be used
	 * @return the found id
	 */
	protected String getId(ConfigurationTarget t, EnumMap<PROPERTY, JsonElement> map, PROPERTY p, String defaultId) {
		if (t == ConfigurationTarget.TEST) {
			if (map.containsKey(p)) {
				return map.get(p).getAsString() + p.name() + ":" + defaultId;
			}
			return p.name();
		}
		return this.getValueOrDefault(map, p, defaultId);
	}

	protected abstract Class<PROPERTY> getPropertyClass();

	/**
	 * Validate the App configuration.
	 *
	 * @param jProperties a JsonObject holding the App properties
	 * @param dependecies the dependencies of the current instance
	 * @return a list of validation errors. Empty list says 'no errors'
	 */
	private List<String> getValidationErrors(JsonObject jProperties, List<Dependency> dependecies) {
		final var errors = new ArrayList<String>();

		final var properties = this.convertToEnumMap(errors, jProperties);
		final var appConfiguration = this.configuration(errors, ConfigurationTarget.VALIDATE, null, properties);
		if (appConfiguration == null) {
			return errors;
		}

		final var edgeConfig = this.componentManager.getEdgeConfig();

		this.validateComponentConfigurations(errors, edgeConfig, appConfiguration);
		this.validateScheduler(errors, edgeConfig, appConfiguration);

		try {
			var appManager = (AppManagerImpl) this.componentManager.getComponent(AppManager.SINGLETON_COMPONENT_ID);
			this.validateDependecies(errors, dependecies, appConfiguration.dependencies, appManager);
		} catch (OpenemsNamedException e) {
			// AppManager not found
			errors.add("No AppManager reachable!");
		}

		// TODO remove 'if' if it works on windows
		// changing network settings only works on linux
		if (!System.getProperty("os.name").startsWith("Windows")) {
			this.validateIps(errors, edgeConfig, appConfiguration);
		}

		return errors;
	}

	@Override
	public final ValidatorConfig getValidatorConfig() {
		Map<String, Object> properties = new TreeMap<>();
		properties.put("openemsApp", this);

		// add check for cardinality for every app
		var validator = this.getValidateBuilder().build();

		validator.getInstallableCheckableConfigs()
				.add(new ValidatorConfig.CheckableConfig(CheckCardinality.COMPONENT_NAME, properties));

		return validator;
	}

	protected ValidatorConfig.Builder getValidateBuilder() {
		return ValidatorConfig.create();
	}

	/**
	 * Gets the value of the property name in the map or the defaulValue if the
	 * property was not found.
	 *
	 * @param map      the configuration map
	 * @param property the property to be searched for
	 * @return the String value
	 */
	protected String getValueOrDefault(EnumMap<PROPERTY, JsonElement> map, DefaultEnum property) {
		var key = Enum.valueOf(this.getPropertyClass(), property.name());
		return this.getValueOrDefault(map, key, property.getDefaultValue());
	}

	/**
	 * Gets the value of the property in the map or the defaulValue if the property
	 * was not found.
	 *
	 * @param map          the configuration map
	 * @param property     the property to be searched for
	 * @param defaultValue the default value
	 * @return the String value
	 */
	protected String getValueOrDefault(EnumMap<PROPERTY, JsonElement> map, PROPERTY property, String defaultValue) {
		var element = map.get(property);
		if (element != null) {
			return JsonUtils.getAsOptionalString(element).orElse(defaultValue);
		}
		return defaultValue;
	}

	/**
	 * Checks if the given property is in the Property class included.
	 *
	 * @param property the enum property
	 * @return true if it is included else false
	 */
	public boolean hasProperty(String property) {
		try {
			Enum.valueOf(this.getPropertyClass(), property);
			return true;
		} catch (IllegalArgumentException ex) {
			// property not an enum property
		}
		return false;
	}

	/**
	 * The returning function gets called during app add or update. The returned
	 * {@link Checkable}s are executed after setting the network configuration.
	 *
	 * <p>
	 * e. g. the function can return a {@link Checkable} for checking if a device is
	 * reachable via network.
	 * </p>
	 *
	 * @return a factory function which returns {@link Checkable}s
	 */
	protected ThrowingBiFunction<//
			ConfigurationTarget, // ADD, UPDATE, VALIDATE, DELETE or TEST
			EnumMap<PROPERTY, JsonElement>, // configuration properties
			Map<String, Map<String, ?>>, // return value of the function
			OpenemsNamedException> // Exception on error
			installationValidation() {
		return null;
	}

	@Override
	public void validate(OpenemsAppInstance instance) throws OpenemsNamedException {
		var errors = this.getValidationErrors(instance.properties, instance.dependencies);
		if (!errors.isEmpty()) {
			var error = errors.stream().collect(Collectors.joining("|"));
			throw new OpenemsException(error);
		}
	}

	/**
	 * Compare actual and expected Components.
	 *
	 * @param errors                   a collection of validation errors
	 * @param actualEdgeConfig         the currently active {@link EdgeConfig}
	 * @param expectedAppConfiguration the expected {@link AppConfiguration}
	 */
	private void validateComponentConfigurations(ArrayList<String> errors, EdgeConfig actualEdgeConfig,
			AppConfiguration expectedAppConfiguration) {
		var missingComponents = new ArrayList<String>();
		for (Component expectedComponent : expectedAppConfiguration.components) {
			var componentId = expectedComponent.getId();

			// Get Actual Component Configuration
			Component actualComponent;
			try {

				actualComponent = actualEdgeConfig.getComponentOrError(componentId);
			} catch (InvalidValueException e) {
				missingComponents.add(componentId);
				continue;
			}
			// ALIAS should not be validated because it can be different depending on the
			// language
			ComponentUtilImpl.isSameConfigurationWithoutAlias(errors, expectedComponent, actualComponent);
		}

		if (!missingComponents.isEmpty()) {
			errors.add("Missing Component" //
					+ (missingComponents.size() > 1 ? "s" : "") + ":" //
					+ missingComponents.stream().collect(Collectors.joining(",")));
		}
	}

	private void validateIps(ArrayList<String> errors, EdgeConfig actualEdgeConfig,
			AppConfiguration expectedAppConfiguration) {
		if (expectedAppConfiguration.ips.isEmpty()) {
			return;
		}

		List<Inet4AddressWithNetmask> addresses = new ArrayList<>(expectedAppConfiguration.ips.size());
		for (var address : expectedAppConfiguration.ips) {
			try {
				addresses.add(Inet4AddressWithNetmask.fromString(address));
			} catch (OpenemsException e) {
				errors.add("Could not parse ip '" + address + "'.");
			}
		}

		try {
			var interfaces = this.componentUtil.getInterfaces();
			var eth0 = interfaces.stream().filter(t -> t.getName().equals("eth0")).findFirst().get();
			var eth0Adresses = eth0.getAddresses();

			var availableAddresses = new LinkedList<Inet4AddressWithNetmask>();
			for (var address : addresses) {
				for (var eth0Address : eth0Adresses.getValue()) {
					if (eth0Address.isInSameNetwork(address)) {
						availableAddresses.add(address);
						break;
					}
				}
			}

			addresses.removeAll(availableAddresses);
			for (var address : addresses) {
				errors.add("Address '" + address + "' is not added.");
			}
		} catch (NullPointerException | IllegalStateException | OpenemsNamedException e) {
			errors.add("Can not validate host config!");
			errors.add(e.getMessage());
		}
	}

	/**
	 * Validates the execution order in the Scheduler.
	 *
	 * @param errors                   a collection of validation errors
	 * @param actualEdgeConfig         the currently active {@link EdgeConfig}
	 * @param expectedAppConfiguration the expected {@link AppConfiguration}
	 */
	private void validateScheduler(ArrayList<String> errors, EdgeConfig actualEdgeConfig,
			AppConfiguration expectedAppConfiguration) {
		if (expectedAppConfiguration.schedulerExecutionOrder.isEmpty()) {
			return;
		}

		// Prepare Queue
		var controllers = new LinkedList<>(this.componentUtil.removeIdsWhichNotExist(
				expectedAppConfiguration.schedulerExecutionOrder, expectedAppConfiguration.components));

		if (controllers.isEmpty()) {
			return;
		}

		List<String> schedulerIds;
		try {
			schedulerIds = this.componentUtil.getSchedulerIds();
		} catch (OpenemsNamedException e) {
			errors.add(e.getMessage());
			return;
		}

		var nextControllerId = controllers.poll();

		// Remove found Controllers from Queue in order
		for (var controllerId : schedulerIds) {
			if (controllerId.equals(nextControllerId)) {
				nextControllerId = controllers.poll();
			}
		}
		if (nextControllerId != null) {
			errors.add("Controller [" + nextControllerId + "] is not/wrongly configured in Scheduler");
		}
	}

	private void validateDependecies(List<String> errors, List<Dependency> configDependencies,
			List<DependencyDeclaration> neededDependencies, AppManagerImpl appManager) {

		// find dependencies that are not in config
		var notRegisteredDependencies = neededDependencies.stream().filter(
				t -> configDependencies == null || !configDependencies.stream().anyMatch(o -> o.key.equals(t.key)))
				.collect(Collectors.toList());

		// check if exactly one app is available of the needed appId
		for (var dependency : notRegisteredDependencies) {
			List<String> minErrors = null;
			for (var appConfig : dependency.appConfigs) {
				var appConfigErrors = new LinkedList<String>();
				if (appConfig.specificInstanceId != null) {
					try {
						var instance = appManager.findInstanceById(appConfig.specificInstanceId);
						checkProperties(errors, instance.properties, appConfig, dependency.key);
					} catch (NoSuchElementException e) {
						appConfigErrors.add("Specific InstanceId[" + appConfig.specificInstanceId + "] not found!");
					}
				} else {

					var list = appManager.getInstantiatedApps().stream().filter(t -> t.appId.equals(appConfig.appId))
							.collect(Collectors.toList());
					if (list.size() != 1) {
						errors.add("Missing dependency with Key[" + dependency.key + "] needed App[" + appConfig.appId
								+ "]");
					} else {
						checkProperties(errors, list.get(0).properties, appConfig, dependency.key);
					}
				}

				if (minErrors == null || minErrors.size() > appConfigErrors.size()) {
					minErrors = appConfigErrors;
				}
			}

			errors.addAll(minErrors);
		}

		if (configDependencies == null) {
			return;
		}
		// check if dependency apps are available
		for (var dependency : configDependencies) {
			try {
				var appInstance = appManager.findInstanceById(dependency.instanceId);
				var dd = neededDependencies.stream().filter(d -> d.key.equals(dependency.key)).findAny();
				if (dd.isEmpty()) {
					errors.add("Can not get DependencyDeclaration of Dependency[" + dependency.key + "]");
					continue;
				}

				// get app config
				var appConfig = dd.get().appConfigs.stream() //
						.filter(c -> c.specificInstanceId != null) //
						.filter(c -> c.specificInstanceId.equals(appInstance.instanceId)).findAny();

				if (appConfig.isEmpty()) {
					appConfig = dd.get().appConfigs.stream() //
							.filter(c -> c.appId != null) //
							.filter(c -> c.appId.equals(appInstance.appId)).findAny();

					if (appConfig.isEmpty()) {
						errors.add("Can not get DependencyAppConfig of Dependency[" + dependency.key + "]");
						continue;
					}
				}

				// when available check properties
				checkProperties(errors, appInstance.properties, appConfig.get(), dependency.key);
			} catch (NoSuchElementException e) {
				errors.add("App with instance[" + dependency.instanceId + "] not available!");
			}
		}
	}

	private static final void checkProperties(List<String> errors, JsonObject actualAppProperties,
			DependencyDeclaration.AppDependencyConfig appDependencyConfig, String dependecyKey) {
		if (appDependencyConfig == null) {
			errors.add("SubApp with Key[" + dependecyKey + "] not found!");
			return;
		}

		for (var property : appDependencyConfig.properties.entrySet()) {
			var actualValue = actualAppProperties.get(property.getKey());
			if (actualValue == null) {
				errors.add("Value for Key[" + property.getKey() + "] not found!");
				continue;
			}
			var actual = actualValue.toString().replace("\"", "");
			var needed = property.getValue().toString().replace("\"", "");
			if (!actual.equals(needed)) {
				errors.add("Value for Key[" + property.getKey() + "] does not match: expected[" + needed + "] actual["
						+ actual + "]  !");
			}
		}
	}

	@Override
	public String getName(Language language) {
		return AbstractOpenemsApp.getTranslation(language, this.getAppId() + ".Name");
	}

	@Override
	public String getImage() {
		var imageName = this.getClass().getSimpleName() + ".png";
		var image = base64OfImage(this.getClass().getResource(imageName));
		if (image != null) {
			return image;
		}
		return OpenemsApp.FALLBACK_IMAGE;
	}

	protected static String getTranslation(Language language, String key) {
		return TranslationUtil.getTranslation(getTranslationBundle(language), key);
	}

	protected static ResourceBundle getTranslationBundle(Language language) {
		if (language == null) {
			language = Language.DEFAULT;
		}
		// TODO add language support
		switch (language) {
		case CZ:
		case ES:
		case FR:
		case NL:
			language = Language.EN;
			break;
		case DE:
		case EN:
			break;
		}
		return ResourceBundle.getBundle("io.openems.edge.core.appmanager.translation", language.getLocal());
	}

	protected static final String base64OfImage(URL url) {
		if (url == null) {
			return null;
		}
		final var prefix = "data:image/png;base64,";
		try (var is = url.openStream()) {
			return prefix + Base64.getEncoder().encodeToString(is.readAllBytes());
		} catch (IOException e) {
			// image not found
			e.printStackTrace();
			return null;
		}
	}

	protected static final Component getComponentWithFactoryId(List<Component> components, String factoryId) {
		return components.stream().filter(t -> t.getFactoryId().equals(factoryId)).findFirst().orElse(null);
	}

}
