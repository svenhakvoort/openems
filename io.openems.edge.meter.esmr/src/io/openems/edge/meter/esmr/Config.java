package io.openems.edge.meter.esmr;

import io.openems.edge.meter.api.MeterType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Meter ESMR", //
		description = "Implements the ESMR grid meter.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "meter0";

	@AttributeDefinition(name = "Mbus PrimaryAddress", description = "PrimaryAddress of the M-Bus device.")
	int primaryAddress() default 10;

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Meter-Type", description = "What is measured by this Meter?")
	MeterType type() default MeterType.GRID;

	@AttributeDefinition(name = "Mbus-ID", description = "ID of M-Bus bridge.")
	String mbus_id() default "mbus0";

	String webconsole_configurationFactory_nameHint() default "Meter ESMR M-Bus [{id}]";

}