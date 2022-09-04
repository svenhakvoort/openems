package io.openems.edge.bridge.esmr;

import io.openems.edge.bridge.esmr.util.DataBits;
import io.openems.edge.bridge.esmr.util.Parity;
import io.openems.edge.bridge.esmr.util.StopBits;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Bridge ESMR", //
		description = "Provides a service for connecting to, reading and writing an ESMR device.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "esmr0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Serial-Device", description = "Serial Device Name")
	String portName() default "/dev/ttyUSB0";

	@AttributeDefinition(name = "Baudrate", description = "Serial Device Speed")
	int baudrate() default 2400;

	@AttributeDefinition(name = "Parity", description = "Serial Device parity")
	Parity parity() default Parity.EVEN;

	@AttributeDefinition(name = "Data bits", description = "Serial Device data bits")
	DataBits dataBits() default DataBits.DATABITS_8;

	@AttributeDefinition(name = "Stop bits", description = "Serial Device stop bits")
	StopBits stopBits() default StopBits.STOPBITS_1;

}