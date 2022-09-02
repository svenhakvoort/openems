package io.openems.edge.core.appmanager.dependency;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppInstance;

public interface AppManagerAppHelper {

	/**
	 * Installs an {@link OpenemsApp} with all its {@link Dependency}s.
	 *
	 * @param user       the executing user
	 * @param properties the properties of the {@link OpenemsAppInstance}
	 * @param alias      the alias of the {@link OpenemsAppInstance}
	 * @param app        the {@link OpenemsApp}
	 * @return s a list of the created {@link OpenemsAppInstance}s
	 * @throws OpenemsNamedException on error
	 */
	public UpdateValues installApp(User user, JsonObject properties, String alias, OpenemsApp app)
			throws OpenemsNamedException;

	/**
	 * Updates an existing {@link OpenemsAppInstance}.
	 *
	 * @param user        the executing user
	 * @param oldInstance the old {@link OpenemsAppInstance} with its
	 *                    configurations.
	 * @param properties  the properties of the new {@link OpenemsAppInstance}
	 * @param alias       the alias of the new {@link OpenemsAppInstance}
	 * @param app         the {@link OpenemsApp}
	 * @return s a list of the replaced {@link OpenemsAppInstance}s
	 * @throws OpenemsNamedException on error
	 */
	public UpdateValues updateApp(User user, OpenemsAppInstance oldInstance, JsonObject properties, String alias,
			OpenemsApp app) throws OpenemsNamedException;

	/**
	 * Deletes an {@link OpenemsAppInstance}.
	 *
	 * @param user     the executing user
	 * @param instance the instance to delete
	 * @return s a list of the removed {@link OpenemsAppInstance}s
	 * @throws OpenemsNamedException on error
	 */
	public UpdateValues deleteApp(User user, OpenemsAppInstance instance) throws OpenemsNamedException;

	/**
	 * Only available during a call of one of the other methods.
	 *
	 * @return null if none of the other methods is currently running else the
	 *         {@link TemporaryApps}
	 */
	public TemporaryApps getTemporaryApps();

}
