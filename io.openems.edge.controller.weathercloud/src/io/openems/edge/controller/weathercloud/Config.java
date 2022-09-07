package io.openems.edge.controller.weathercloud;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Controller WeatherCloud", //
		description = "Implements the WeatherCloud connection")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "weatherCloud0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Station-ID", description = "ID of weathercloud Station to track")
	String weatherCloudStation() default "";

	String webconsole_configurationFactory_nameHint() default "Controller WeatherCloud [{id}]";

}