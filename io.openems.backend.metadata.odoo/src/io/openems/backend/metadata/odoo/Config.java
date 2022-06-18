package io.openems.backend.metadata.odoo;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.backend.metadata.odoo.odoo.Protocol;

@ObjectClassDefinition(//
		name = "Metadata.Odoo", //
		description = "Configures the Odoo Metadata provider")
public @interface Config {

	@AttributeDefinition(name = "Odoo Protocol", description = "The odoo protocol")
	Protocol odooProtocol() default Protocol.HTTP;

	@AttributeDefinition(name = "Odoo Host", description = "The odoo host")
	String odooHost() default "odoo";

	@AttributeDefinition(name = "Odoo Port", description = "The odoo port")
	int odooPort() default 8069;

	@AttributeDefinition(name = "Odoo UID", description = "The odoo login UID")
	int odooUid() default 1;

	@AttributeDefinition(name = "Odoo Password", description = "The odoo login password")
	String odooPassword();

	@AttributeDefinition(name = "Postgres Host", description = "The Postgres host")
	String pgHost() default "postgres";

	@AttributeDefinition(name = "Postgres Port", description = "The Postgres port")
	int pgPort() default 5432;

	@AttributeDefinition(name = "Postgres Username", description = "The Postgres username")
	String pgUser() default "openems";

	@AttributeDefinition(name = "Postgres Password", description = "The Postgres password")
	String pgPassword() default "openems";

	@AttributeDefinition(name = "Database", description = "The database name")
	String database();

	String webconsole_configurationFactory_nameHint() default "Metadata.Odoo";

}
