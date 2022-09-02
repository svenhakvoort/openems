package io.openems.edge.core.appmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.TestADependencyToC;
import io.openems.edge.app.TestBDependencyToC;
import io.openems.edge.app.TestC;
import io.openems.edge.app.evcs.KebaEvcs;
import io.openems.edge.app.integratedsystem.FeneconHome;
import io.openems.edge.app.timeofusetariff.AwattarHourly;
import io.openems.edge.app.timeofusetariff.StromdaoCorrently;
import io.openems.edge.common.test.DummyUser;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.DeleteAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppInstance;

public class AppManagerAppHelperImplTest {

	private final User user = new DummyUser("1", "password", Language.DEFAULT, Role.ADMIN);

	private AppManagerTestBundle appManagerTestBundle;

	private FeneconHome homeApp;
	private KebaEvcs kebaEvcsApp;
	private AwattarHourly awattarApp;
	private StromdaoCorrently stromdao;

	private TestADependencyToC testAApp;
	private TestBDependencyToC testBApp;
	private TestC testCApp;

	@Before
	public void beforeEach() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			this.homeApp = new FeneconHome(t.componentManger,
					AppManagerTestBundle.getComponentContext("App.FENECON.Home"), t.cm, t.componentUtil);
			this.kebaEvcsApp = new KebaEvcs(t.componentManger,
					AppManagerTestBundle.getComponentContext("App.Evcs.Keba"), t.cm, t.componentUtil);
			this.awattarApp = new AwattarHourly(t.componentManger,
					AppManagerTestBundle.getComponentContext("App.TimeVariablePrice.Awattar"), t.cm, t.componentUtil);
			this.stromdao = new StromdaoCorrently(t.componentManger,
					AppManagerTestBundle.getComponentContext("App.TimeVariablePrice.Stromdao"), t.cm, t.componentUtil);

			this.testAApp = new TestADependencyToC(t.componentManger,
					AppManagerTestBundle.getComponentContext("App.Test.TestADependencyToC"), t.cm, t.componentUtil);
			this.testBApp = new TestBDependencyToC(t.componentManger,
					AppManagerTestBundle.getComponentContext("App.Test.TestBDependencyToC"), t.cm, t.componentUtil);
			this.testCApp = new TestC(t.componentManger, AppManagerTestBundle.getComponentContext("App.Test.TestC"),
					t.cm, t.componentUtil);

			return ImmutableList.of(this.homeApp, this.kebaEvcsApp, this.awattarApp, this.stromdao, this.testAApp,
					this.testBApp, this.testCApp);
		});

	}

	@Test
	public void testCreatePolicyIfNotExisting() throws OpenemsNamedException {
		assertEquals(0, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
				new AddAppInstance.Request(this.testAApp.getAppId(), "", //
						JsonUtils.buildJsonObject() //
								.addProperty("CREATE_POLICY", DependencyDeclaration.CreatePolicy.IF_NOT_EXISTING.name())
								.build()));

		assertEquals(2, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
				new AddAppInstance.Request(this.testBApp.getAppId(), "", //
						JsonUtils.buildJsonObject() //
								.addProperty("CREATE_POLICY", DependencyDeclaration.CreatePolicy.IF_NOT_EXISTING.name())
								.build()));

		assertEquals(3, this.appManagerTestBundle.sut.getInstantiatedApps().size());
	}

	@Test
	public void testCreatePolicyAlways() throws OpenemsNamedException {
		assertEquals(0, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
				new AddAppInstance.Request(this.testAApp.getAppId(), "", //
						JsonUtils.buildJsonObject() //
								.addProperty("CREATE_POLICY", DependencyDeclaration.CreatePolicy.ALWAYS.name())
								.build()));

		assertEquals(2, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
				new AddAppInstance.Request(this.testBApp.getAppId(), "", //
						JsonUtils.buildJsonObject() //
								.addProperty("CREATE_POLICY", DependencyDeclaration.CreatePolicy.ALWAYS.name())
								.build()));

		assertEquals(4, this.appManagerTestBundle.sut.getInstantiatedApps().size());
	}

	@Test
	public void testCreatePolicyNever() throws OpenemsNamedException {
		assertEquals(0, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
				new AddAppInstance.Request(this.testAApp.getAppId(), "", //
						JsonUtils.buildJsonObject() //
								.addProperty("CREATE_POLICY", DependencyDeclaration.CreatePolicy.NEVER.name())
								.build()));

		assertEquals(1, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
				new AddAppInstance.Request(this.testBApp.getAppId(), "", //
						JsonUtils.buildJsonObject() //
								.addProperty("CREATE_POLICY", DependencyDeclaration.CreatePolicy.NEVER.name())
								.build()));

		assertEquals(2, this.appManagerTestBundle.sut.getInstantiatedApps().size());
	}

	@Test
	public void testUpdatePolicyNever() throws OpenemsNamedException {
		assertEquals(0, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
				new AddAppInstance.Request(this.testAApp.getAppId(), "", //
						JsonUtils.buildJsonObject() //
								.addProperty("UPDATE_POLICY", DependencyDeclaration.UpdatePolicy.NEVER.name()) //
								.addProperty("NUMBER", 1) //
								.build()));

		assertEquals(2, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		this.appManagerTestBundle.sut.handleJsonrpcRequest(this.user,
				new UpdateAppInstance.Request(this.getAppByAppId(this.testAApp.getAppId()).instanceId, "",
						JsonUtils.buildJsonObject() //
								.addProperty("UPDATE_POLICY", DependencyDeclaration.UpdatePolicy.NEVER.name()) //
								.addProperty("NUMBER", 2) //
								.build()));

		var instance = this.getAppByAppId(this.testCApp.getAppId());
		assertEquals(1, instance.properties.get(TestC.Property.NUMBER.name()).getAsInt());
	}

	@Test
	public void testUpdatePolicyAlways() throws OpenemsNamedException {
		assertEquals(0, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
				new AddAppInstance.Request(this.testAApp.getAppId(), "", //
						JsonUtils.buildJsonObject() //
								.addProperty("UPDATE_POLICY", DependencyDeclaration.UpdatePolicy.ALWAYS.name()) //
								.addProperty("NUMBER", 1) //
								.build()));

		assertEquals(2, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
				new AddAppInstance.Request(this.testBApp.getAppId(), "", //
						JsonUtils.buildJsonObject().build()));

		assertEquals(3, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		this.appManagerTestBundle.sut.handleJsonrpcRequest(this.user,
				new UpdateAppInstance.Request(this.getAppByAppId(this.testAApp.getAppId()).instanceId, "",
						JsonUtils.buildJsonObject() //
								.addProperty("UPDATE_POLICY", DependencyDeclaration.UpdatePolicy.ALWAYS.name()) //
								.addProperty("NUMBER", 2) //
								.build()));

		var instance = this.getAppByAppId(this.testCApp.getAppId());
		assertEquals(2, instance.properties.get(TestC.Property.NUMBER.name()).getAsInt());
	}

	@Test
	public void testUpdatePolicyIfMine() throws OpenemsNamedException {
		assertEquals(0, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
				new AddAppInstance.Request(this.testAApp.getAppId(), "", //
						JsonUtils.buildJsonObject() //
								.addProperty("UPDATE_POLICY", DependencyDeclaration.UpdatePolicy.IF_MINE.name()) //
								.addProperty("NUMBER", 1) //
								.build()));

		assertEquals(2, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		this.appManagerTestBundle.sut.handleJsonrpcRequest(this.user,
				new UpdateAppInstance.Request(this.getAppByAppId(this.testAApp.getAppId()).instanceId, "",
						JsonUtils.buildJsonObject() //
								.addProperty("UPDATE_POLICY", DependencyDeclaration.UpdatePolicy.IF_MINE.name()) //
								.addProperty("NUMBER", 2) //
								.build()));

		var instance = this.getAppByAppId(this.testCApp.getAppId());
		assertEquals(2, instance.properties.get(TestC.Property.NUMBER.name()).getAsInt());

		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
				new AddAppInstance.Request(this.testBApp.getAppId(), "", //
						JsonUtils.buildJsonObject().build()));

		assertEquals(3, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		this.appManagerTestBundle.sut.handleJsonrpcRequest(this.user,
				new UpdateAppInstance.Request(this.getAppByAppId(this.testAApp.getAppId()).instanceId, "",
						JsonUtils.buildJsonObject() //
								.addProperty("UPDATE_POLICY", DependencyDeclaration.UpdatePolicy.IF_MINE.name()) //
								.addProperty("NUMBER", 3) //
								.build()));

		instance = this.getAppByAppId(this.testCApp.getAppId());
		assertEquals(2, instance.properties.get(TestC.Property.NUMBER.name()).getAsInt());
	}

	@Test
	public void testDeletePolicyNever() throws OpenemsNamedException {
		assertEquals(0, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
				new AddAppInstance.Request(this.testAApp.getAppId(), "", //
						JsonUtils.buildJsonObject() //
								.addProperty("DELETE_POLICY", DependencyDeclaration.DeletePolicy.NEVER.name()) //
								.build()));

		assertEquals(2, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		var instance = this.getAppByAppId(this.testAApp.getAppId());
		this.appManagerTestBundle.sut.handleDeleteAppInstanceRequest(this.user,
				new DeleteAppInstance.Request(instance.instanceId));

		assertEquals(1, this.appManagerTestBundle.sut.getInstantiatedApps().size());
	}

	@Test
	public void testDeletePolicyAlways() throws OpenemsNamedException {
		assertEquals(0, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
				new AddAppInstance.Request(this.testAApp.getAppId(), "", //
						JsonUtils.buildJsonObject() //
								.addProperty("DELETE_POLICY", DependencyDeclaration.DeletePolicy.ALWAYS.name()) //
								.build()));

		assertEquals(2, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
				new AddAppInstance.Request(this.testBApp.getAppId(), "", //
						JsonUtils.buildJsonObject().build()));

		assertEquals(3, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		var instance = this.getAppByAppId(this.testAApp.getAppId());
		this.appManagerTestBundle.sut.handleDeleteAppInstanceRequest(this.user,
				new DeleteAppInstance.Request(instance.instanceId));

		assertEquals(1, this.appManagerTestBundle.sut.getInstantiatedApps().size());
	}

	@Test
	public void testDeletePolicyIfMine() throws OpenemsNamedException {
		assertEquals(0, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
				new AddAppInstance.Request(this.testAApp.getAppId(), "", //
						JsonUtils.buildJsonObject() //
								.addProperty("DELETE_POLICY", DependencyDeclaration.DeletePolicy.IF_MINE.name()) //
								.build()));

		assertEquals(2, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
				new AddAppInstance.Request(this.testBApp.getAppId(), "", //
						JsonUtils.buildJsonObject().build()));

		assertEquals(3, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		var instance = this.getAppByAppId(this.testAApp.getAppId());
		this.appManagerTestBundle.sut.handleDeleteAppInstanceRequest(this.user,
				new DeleteAppInstance.Request(instance.instanceId));

		assertEquals(2, this.appManagerTestBundle.sut.getInstantiatedApps().size());
	}

	@Test
	public void testDependencyDeletePolicyAllowed() throws OpenemsNamedException {
		assertEquals(0, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
				new AddAppInstance.Request(this.testAApp.getAppId(), "", //
						JsonUtils.buildJsonObject() //
								.addProperty("DEPENDENCY_DELETE_POLICY",
										DependencyDeclaration.DependencyDeletePolicy.ALLOWED.name()) //
								.build()));

		assertEquals(2, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		var instance = this.getAppByAppId(this.testCApp.getAppId());
		this.appManagerTestBundle.sut.handleDeleteAppInstanceRequest(this.user,
				new DeleteAppInstance.Request(instance.instanceId));

		assertEquals(1, this.appManagerTestBundle.sut.getInstantiatedApps().size());
	}

	@Test(expected = OpenemsNamedException.class)
	public void testDependencyDeletePolicyNotAllowed() throws OpenemsNamedException {
		assertEquals(0, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
				new AddAppInstance.Request(this.testAApp.getAppId(), "", //
						JsonUtils.buildJsonObject() //
								.addProperty("DEPENDENCY_DELETE_POLICY",
										DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED.name()) //
								.build()));

		assertEquals(2, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		var instance = this.getAppByAppId(this.testCApp.getAppId());
		this.appManagerTestBundle.sut.handleDeleteAppInstanceRequest(this.user,
				new DeleteAppInstance.Request(instance.instanceId));
	}

	@Test
	public void testDependencyUpdatePolicyAllowAll()
			throws OpenemsNamedException, InterruptedException, ExecutionException {
		assertEquals(0, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
				new AddAppInstance.Request(this.testAApp.getAppId(), "", //
						JsonUtils.buildJsonObject() //
								.addProperty("DEPENDENCY_UPDATE_POLICY",
										DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ALL.name()) //
								.addProperty("NUMBER", 1) //
								.build()));

		assertEquals(2, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		var newAlias = "newAppAlias";
		var completable = this.appManagerTestBundle.sut.handleJsonrpcRequest(this.user,
				new UpdateAppInstance.Request(this.getAppByAppId(this.testCApp.getAppId()).instanceId, newAlias,
						JsonUtils.buildJsonObject() //
								.addProperty("NUMBER", 2) //
								.build()));

		var result = completable.get().getResult();
		assertTrue(!result.has("warnings") || result.get("warnings").getAsJsonArray().size() == 0);

		var instance = this.getAppByAppId(this.testCApp.getAppId());
		assertEquals(newAlias, instance.alias);
		assertEquals(2, instance.properties.get("NUMBER").getAsInt());
	}

	@Test(expected = OpenemsNamedException.class)
	public void testDependencyUpdatePolicyAllowNone()
			throws OpenemsNamedException, InterruptedException, ExecutionException {
		assertEquals(0, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
				new AddAppInstance.Request(this.testAApp.getAppId(), "", //
						JsonUtils.buildJsonObject() //
								.addProperty("DEPENDENCY_UPDATE_POLICY",
										DependencyDeclaration.DependencyUpdatePolicy.ALLOW_NONE.name()) //
								.addProperty("NUMBER", 1) //
								.build()));

		assertEquals(2, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		var newAlias = "newAppAlias";
		this.appManagerTestBundle.sut.handleJsonrpcRequest(this.user,
				new UpdateAppInstance.Request(this.getAppByAppId(this.testCApp.getAppId()).instanceId, newAlias,
						JsonUtils.buildJsonObject() //
								.addProperty("NUMBER", 2) //
								.build()));
	}

	@Test
	public void testDependencyUpdatePolicyAllowOnlyUnconfiguredProperties()
			throws OpenemsNamedException, InterruptedException, ExecutionException {
		assertEquals(0, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user, new AddAppInstance.Request(
				this.testAApp.getAppId(), "", //
				JsonUtils.buildJsonObject() //
						.addProperty("DEPENDENCY_UPDATE_POLICY",
								DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ONLY_UNCONFIGURED_PROPERTIES.name()) //
						.addProperty("NUMBER", 1) //
						.build()));

		assertEquals(2, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		var newAlias = "newAppAlias";
		var completable = this.appManagerTestBundle.sut.handleJsonrpcRequest(this.user,
				new UpdateAppInstance.Request(this.getAppByAppId(this.testCApp.getAppId()).instanceId, newAlias,
						JsonUtils.buildJsonObject() //
								.addProperty("NUMBER", 2) //
								.build()));

		var result = completable.get().getResult();
		assertTrue(result.has("warnings"));
		assertTrue(result.get("warnings").getAsJsonArray().size() > 0);

		var instance = this.getAppByAppId(this.testCApp.getAppId());
		assertEquals(newAlias, instance.alias);
		assertEquals(1, instance.properties.get("NUMBER").getAsInt());

	}

	private OpenemsAppInstance getAppByAppId(String appId) {
		return this.appManagerTestBundle.sut.getInstantiatedApps().stream().filter(i -> i.appId.equals(appId)).findAny()
				.get();
	}

}
