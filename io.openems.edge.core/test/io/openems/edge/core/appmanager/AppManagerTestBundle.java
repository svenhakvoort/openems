package io.openems.edge.core.appmanager;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;

import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.ReflectionUtils;
import io.openems.edge.common.host.Host;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentContext;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.core.appmanager.dependency.AppManagerAppHelperImpl;
import io.openems.edge.core.appmanager.dependency.ComponentAggregateTaskImpl;
import io.openems.edge.core.appmanager.dependency.DependencyUtil;
import io.openems.edge.core.appmanager.dependency.SchedulerAggregateTaskImpl;
import io.openems.edge.core.appmanager.dependency.StaticIpAggregateTaskImpl;
import io.openems.edge.core.appmanager.validator.CheckCardinality;
import io.openems.edge.core.appmanager.validator.Checkable;
import io.openems.edge.core.appmanager.validator.Validator;

public class AppManagerTestBundle {

	public final DummyConfigurationAdmin cm;
	public final DummyComponentManager componentManger;
	public final ComponentUtil componentUtil;
	public final Validator validator;

	public final AppManagerImpl sut;
	public final AppManagerUtil appManagerUtil;

	public final CheckablesBundle checkablesBundle;

	public AppManagerTestBundle(JsonObject initialComponentConfig, MyConfig initialAppManagerConfig,
			Function<AppManagerTestBundle, List<OpenemsApp>> availableAppsSupplier) throws Exception {
		if (initialComponentConfig == null) {
			initialComponentConfig = JsonUtils.buildJsonObject() //
					.add("scheduler0", JsonUtils.buildJsonObject() //
							.addProperty("factoryId", "Scheduler.AllAlphabetically") //
							.add("properties", JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.add("controllers.ids", JsonUtils.buildJsonArray() //
											.build()) //
									.build()) //
							.build()) //
					.add(Host.SINGLETON_COMPONENT_ID, JsonUtils.buildJsonObject() //
							.addProperty("factoryId", Host.SINGLETON_SERVICE_PID) //
							.addProperty("alias", "") //
							.add("properties", JsonUtils.buildJsonObject() //
									.addProperty("networkConfiguration", //
											"{\n" //
													+ "  \"interfaces\": {\n" //
													+ "    \"enx*\": {\n" //
													+ "      \"dhcp\": false,\n" //
													+ "      \"addresses\": [\n" //
													+ "        \"10.4.0.1/16\",\n" //
													+ "        \"192.168.1.9/29\"\n" //
													+ "      ]\n" //
													+ "    },\n" //
													+ "    \"eth0\": {\n" //
													+ "      \"dhcp\": true,\n" //
													+ "      \"linkLocalAddressing\": true,\n" //
													+ "      \"addresses\": [\n" //
													+ "        \"192.168.100.100/24\"\n" //
													+ "      ]\n" //
													+ "    }\n" //
													+ "  }\n" //
													+ "}") //
									.build()) //
							.build()) //
					.build();
		}

		if (initialAppManagerConfig == null) {
			initialAppManagerConfig = MyConfig.create() //
					.setApps(JsonUtils.buildJsonArray() //
							.build() //
							.toString())
					.build();
		}

		this.cm = new DummyConfigurationAdmin();
		this.cm.getOrCreateEmptyConfiguration(AppManager.SINGLETON_SERVICE_PID);

		this.componentManger = new DummyComponentManager();
		this.componentManger.setConfigJson(JsonUtils.buildJsonObject() //
				.add("components", initialComponentConfig) //
				.add("factories", JsonUtils.buildJsonObject() //
						.build()) //
				.build() //
		);

		// create config for scheduler
		this.cm.getOrCreateEmptyConfiguration(
				this.componentManger.getEdgeConfig().getComponent("scheduler0").get().getPid());

		this.componentUtil = new ComponentUtilImpl(this.componentManger, this.cm);

		final var componentTask = new ComponentAggregateTaskImpl(this.componentManger);
		final var schedulerTask = new SchedulerAggregateTaskImpl(componentTask, this.componentUtil);
		final var staticIpTask = new StaticIpAggregateTaskImpl(this.componentUtil);

		this.sut = new AppManagerImpl();
		this.componentManger.addComponent(this.sut);
		this.componentManger.setConfigurationAdmin(this.cm);
		this.appManagerUtil = new AppManagerUtilImpl();
		ReflectionUtils.setAttribute(this.appManagerUtil.getClass(), this.appManagerUtil, "appManager", this.sut);

		this.checkablesBundle = new CheckablesBundle(new CheckCardinality(this.sut, this.appManagerUtil,
				getComponentContext(CheckCardinality.COMPONENT_NAME)));

		var dummyValidator = new DummyValidator();
		dummyValidator.setCheckables(this.checkablesBundle.all());
		this.validator = dummyValidator;

		var appManagerAppHelper = new AppManagerAppHelperImpl(this.componentManger, this.componentUtil, this.validator,
				componentTask, schedulerTask, staticIpTask);

		// use this so the appManagerAppHelper does not has to be a OpenemsComponent and
		// the attribute can still be private
		ReflectionUtils.setAttribute(appManagerAppHelper.getClass(), appManagerAppHelper, "appManager", this.sut);
		ReflectionUtils.setAttribute(appManagerAppHelper.getClass(), appManagerAppHelper, "appManagerUtil",
				this.appManagerUtil);

		ReflectionUtils.setAttribute(DependencyUtil.class, null, "appHelper", appManagerAppHelper);

		new ComponentTest(this.sut) //
				.addReference("cm", this.cm) //
				.addReference("componentManager", this.componentManger) //
				.addReference("appHelper", appManagerAppHelper) //
				.addReference("validator", this.validator) //
				.addReference("availableApps", availableAppsSupplier.apply(this)) //
				.activate(initialAppManagerConfig);

	}

	/**
	 * Checks if the {@link Validator} has any errors.
	 * 
	 * @throws Exception on error
	 */
	public void assertNoValidationErrors() throws Exception {
		var worker = new AppValidateWorker(this.sut);
		worker.validateApps();

		// should not have found defective Apps
		if (!worker.defectiveApps.isEmpty()) {
			throw new Exception(worker.defectiveApps.entrySet().stream() //
					.map(e -> e.getKey() + "[" + e.getValue() + "]") //
					.collect(Collectors.joining("|")));
		}
	}

	/**
	 * Prints out the instantiated {@link OpenemsAppInstance}s.
	 */
	public void printApps() {
		JsonUtils.prettyPrint(this.sut.getInstantiatedApps().stream().map(OpenemsAppInstance::toJsonObject)
				.collect(JsonUtils.toJsonArray()));
	}

	public static class CheckablesBundle {

		public final CheckCardinality checkCardinality;

		private CheckablesBundle(CheckCardinality checkCardinality) {
			this.checkCardinality = checkCardinality;
		}

		/**
		 * Gets all {@link Checkable}.
		 * 
		 * @return the {@link Checkable}
		 */
		public final List<Checkable> all() {
			var result = new ArrayList<Checkable>();
			result.add(this.checkCardinality);
			return result;
		}
	}

	/**
	 * Gets the {@link ComponentContext} for an {@link OpenemsApp} of the given
	 * appId.
	 * 
	 * @param appId the {@link OpenemsApp#getAppId()} of the {@link OpenemsApp}
	 * @return the {@link ComponentContext}
	 */
	public static ComponentContext getComponentContext(String appId) {
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(ComponentConstants.COMPONENT_NAME, appId);
		return new DummyComponentContext(properties);
	}

}
