package io.openems.edge.core.weather;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Core Weather", //
		description = "Implements the Weather data")
@interface Config {

	@AttributeDefinition(name = "Station-ID", description = "ID of weathercloud Station to track (without the letter 'd' prefix)")
	String weatherCloudStation() default "";

	String webconsole_configurationFactory_nameHint() default "Core Weather";

}