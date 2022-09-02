package io.openems.edge.app.api;

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
import io.openems.common.utils.EnumUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.api.RestJsonApiReadWrite.Property;
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
 * Describes a App for ReadWrite Rest JSON Api.
 *
 * <pre>
  {
    "appId":"App.Api.RestJson.ReadWrite",
    "alias":"Rest/JSON-Api Read-Write",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"CONTROLLER_ID": "ctrlApiRest0",
    	"API_TIMEOUT": 60
    },
    "dependencies": [
    	{
        	"key": "READ_ONLY",
        	"instanceId": UUID
    	}
    ],
    "appDescriptor": {
    	"websiteUrl": URL
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.Api.RestJson.ReadWrite")
public class RestJsonApiReadWrite extends AbstractOpenemsApp<Property> implements OpenemsApp {

	public static enum Property {
		// Components
		CONTROLLER_ID, //
		// User-Values
		API_TIMEOUT;
	}

	@Activate
	public RestJsonApiReadWrite(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildInput(Property.API_TIMEOUT) //
								.setLabel(TranslationUtil.getTranslation(bundle, "App.Api.apiTimeout.label")) //
								.setDescription(
										TranslationUtil.getTranslation(bundle, "App.Api.apiTimeout.description")) //
								.setInputType(Type.NUMBER) //
								.setDefaultValue(60) //
								.setMin(30) //
								.isRequired(true) //
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
		return new OpenemsAppCategory[] { OpenemsAppCategory.API };
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {
			var controllerId = this.getId(t, p, Property.CONTROLLER_ID, "ctrlApiRest0");

			var apiTimeout = EnumUtils.getAsInt(p, Property.API_TIMEOUT);

			List<EdgeConfig.Component> components = Lists.newArrayList(//
					new EdgeConfig.Component(controllerId, this.getName(l), "Controller.Api.Rest.ReadWrite",
							JsonUtils.buildJsonObject() //
									.addProperty("apiTimeout", apiTimeout) //
									.build()));

			var dependencies = Lists.newArrayList(new DependencyDeclaration("READ_ONLY", //
					DependencyDeclaration.CreatePolicy.NEVER, //
					DependencyDeclaration.UpdatePolicy.ALWAYS, //
					DependencyDeclaration.DeletePolicy.NEVER, //
					DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ONLY_UNCONFIGURED_PROPERTIES, //
					DependencyDeclaration.DependencyDeletePolicy.ALLOWED, //
					DependencyDeclaration.AppDependencyConfig.create() //
							.setAppId("App.Api.RestJson.ReadOnly") //
							.setProperties(JsonUtils.buildJsonObject() //
									.addProperty(ModbusTcpApiReadOnly.Property.ACTIVE.name(),
											t == ConfigurationTarget.DELETE) //
									.build())
							.build()));

			return new AppConfiguration(components, null, null, dependencies);
		};
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

}
