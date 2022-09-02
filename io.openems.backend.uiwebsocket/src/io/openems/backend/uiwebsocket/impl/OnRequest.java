package io.openems.backend.uiwebsocket.impl;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.common.jsonrpc.request.AddEdgeToUserRequest;
import io.openems.backend.common.jsonrpc.request.GetAlertingConfigRequest;
import io.openems.backend.common.jsonrpc.request.GetSetupProtocolDataRequest;
import io.openems.backend.common.jsonrpc.request.GetSetupProtocolRequest;
import io.openems.backend.common.jsonrpc.request.GetUserInformationRequest;
import io.openems.backend.common.jsonrpc.request.RegisterUserRequest;
import io.openems.backend.common.jsonrpc.request.SendLogMessageRequest;
import io.openems.backend.common.jsonrpc.request.SetAlertingConfigRequest;
import io.openems.backend.common.jsonrpc.request.SetUserInformationRequest;
import io.openems.backend.common.jsonrpc.request.SubmitSetupProtocolRequest;
import io.openems.backend.common.jsonrpc.response.AddEdgeToUserResponse;
import io.openems.backend.common.jsonrpc.response.GetAlertingConfigResponse;
import io.openems.backend.common.jsonrpc.response.GetUserInformationResponse;
import io.openems.backend.common.metadata.EdgeUser;
import io.openems.backend.common.metadata.User;
import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.AuthenticateWithPasswordRequest;
import io.openems.common.jsonrpc.request.AuthenticateWithTokenRequest;
import io.openems.common.jsonrpc.request.EdgeRpcRequest;
import io.openems.common.jsonrpc.request.LogoutRequest;
import io.openems.common.jsonrpc.request.SubscribeChannelsRequest;
import io.openems.common.jsonrpc.request.SubscribeSystemLogRequest;
import io.openems.common.jsonrpc.request.UpdateUserLanguageRequest;
import io.openems.common.jsonrpc.response.AuthenticateResponse;
import io.openems.common.jsonrpc.response.Base64PayloadResponse;
import io.openems.common.jsonrpc.response.EdgeRpcResponse;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;

public class OnRequest implements io.openems.common.websocket.OnRequest {

	private final Logger log = LoggerFactory.getLogger(OnRequest.class);
	private final UiWebsocketImpl parent;

	public OnRequest(UiWebsocketImpl parent) {
		this.parent = parent;
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> run(WebSocket ws, JsonrpcRequest request)
			throws OpenemsNamedException {
		WsData wsData = ws.getAttachment();

		// Start with authentication requests
		CompletableFuture<? extends JsonrpcResponseSuccess> result = null;
		switch (request.getMethod()) {
		case AuthenticateWithTokenRequest.METHOD:
			return this.handleAuthenticateWithTokenRequest(wsData, AuthenticateWithTokenRequest.from(request));

		case AuthenticateWithPasswordRequest.METHOD:
			return this.handleAuthenticateWithPasswordRequest(wsData, AuthenticateWithPasswordRequest.from(request));

		case RegisterUserRequest.METHOD:
			return this.handleRegisterUserReuqest(wsData, RegisterUserRequest.from(request));
		}

		// should be authenticated
		var user = this.assertUser(wsData, request);

		switch (request.getMethod()) {
		case LogoutRequest.METHOD:
			result = this.handleLogoutRequest(wsData, user, LogoutRequest.from(request));
			break;
		case EdgeRpcRequest.METHOD:
			result = this.handleEdgeRpcRequest(wsData, user, EdgeRpcRequest.from(request));
			break;
		case AddEdgeToUserRequest.METHOD:
			result = this.handleAddEdgeToUserRequest(user, AddEdgeToUserRequest.from(request));
			break;
		case GetUserInformationRequest.METHOD:
			result = this.handleGetUserInformationRequest(user, GetUserInformationRequest.from(request));
			break;
		case SetUserInformationRequest.METHOD:
			result = this.handleSetUserInformationRequest(user, SetUserInformationRequest.from(request));
			break;
		case GetSetupProtocolRequest.METHOD:
			result = this.handleGetSetupProtocolRequest(user, GetSetupProtocolRequest.from(request));
			break;
		case SubmitSetupProtocolRequest.METHOD:
			result = this.handleSubmitSetupProtocolRequest(user, SubmitSetupProtocolRequest.from(request));
			break;
		case UpdateUserLanguageRequest.METHOD:
			result = this.handleUpdateUserLanguageRequest(user, UpdateUserLanguageRequest.from(request));
			break;
		case GetAlertingConfigRequest.METHOD:
			result = this.handleGetAlertingConfigRequest(user, GetAlertingConfigRequest.from(request));
			break;
		case SetAlertingConfigRequest.METHOD:
			result = this.handleSetAlertingConfigRequest(user, SetAlertingConfigRequest.from(request));
			break;
		case GetSetupProtocolDataRequest.METHOD:
			result = this.handleGetSetupProtocolDataRequest(user, GetSetupProtocolDataRequest.from(request));
			break;
		case SendLogMessageRequest.METHOD:
			result = this.handleSendLogMessageRequest(user, SendLogMessageRequest.from(request));
			break;
		}

		if (result != null) {
			// was able to handle request directly
			return result;
		}

		// forward to generic request handler
		return this.parent.jsonRpcRequestHandler.handleRequest(this.parent.getName(), user, request);
	}

	private class AlertingException extends IllegalArgumentException {
		private static final long serialVersionUID = 1427696037440674667L;

		private AlertingException(String userId, String edgeId) {
			super("settings_err:'" //
					+ "There is no User [" + userId + "] in Edge [" + edgeId + "]!'");
		}
	}

	private CompletableFuture<? extends JsonrpcResponseSuccess> handleGetAlertingConfigRequest(User user,
			GetAlertingConfigRequest request) {
		String edgeId = request.getEdgeId();
		String userId = user.getId();

		Optional<EdgeUser> edgeUser = this.parent.metadata.getEdgeUserTo(edgeId, userId);

		if (edgeUser.isPresent()) {
			return CompletableFuture.completedFuture(//
					new GetAlertingConfigResponse(request.getId(), edgeUser.get().getTimeToWait()));
		} else {
			throw new AlertingException(userId, edgeId);
		}
	}

	private CompletableFuture<? extends JsonrpcResponseSuccess> handleSetAlertingConfigRequest(User user,
			SetAlertingConfigRequest request) throws NotImplementedException {
		String edgeId = request.getEdgeId();
		String userId = user.getId();
		int timeToWait = request.getTimeToWait();

		Optional<EdgeUser> edgeUser = this.parent.metadata.getEdgeUserTo(edgeId, userId);

		if (edgeUser.isPresent()) {
			EdgeUser userRole = edgeUser.get();
			userRole.setTimeToWait(timeToWait);

			return CompletableFuture.completedFuture(//
					new GetAlertingConfigResponse(request.getId(), userRole.getTimeToWait()));
		} else {
			throw new AlertingException(userId, edgeId);
		}
	}

	/**
	 * Handles a {@link AuthenticateWithTokenRequest}.
	 *
	 * @param wsData  the WebSocket attachment
	 * @param request the {@link AuthenticateWithTokenRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleAuthenticateWithTokenRequest(WsData wsData,
			AuthenticateWithTokenRequest request) throws OpenemsNamedException {
		return this.handleAuthentication(wsData, request.getId(),
				this.parent.metadata.authenticate(request.getToken()));
	}

	/**
	 * Handles a {@link AuthenticateWithPasswordRequest}.
	 *
	 * @param wsData  the WebSocket attachment
	 * @param request the {@link AuthenticateWithPasswordRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleAuthenticateWithPasswordRequest(WsData wsData,
			AuthenticateWithPasswordRequest request) throws OpenemsNamedException {
		if (request.getUsername().isPresent()) {
			return this.handleAuthentication(wsData, request.getId(),
					this.parent.metadata.authenticate(request.getUsername().get(), request.getPassword()));
		}
		return this.handleAuthentication(wsData, request.getId(),
				this.parent.metadata.authenticate(request.getPassword()));
	}

	/**
	 * Common handler for {@link AuthenticateWithTokenRequest} and
	 * {@link AuthenticateWithPasswordRequest}.
	 *
	 * @param wsData    the WebSocket attachment
	 * @param requestId the ID of the original {@link JsonrpcRequest}
	 * @param user      the authenticated {@link User}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleAuthentication(WsData wsData, UUID requestId, User user)
			throws OpenemsNamedException {
		this.parent.logInfo(this.log, "User [" + user.getId() + ":" + user.getName() + "] connected.");

		wsData.setUserId(user.getId());
		wsData.setToken(user.getToken());
		return CompletableFuture.completedFuture(new AuthenticateResponse(requestId, user.getToken(), user,
				User.generateEdgeMetadatas(user, this.parent.metadata), user.getLanguage()));
	}

	/**
	 * Handles a {@link RegisterUserRequest}.
	 *
	 * @param wsData  the WebSocket attachment
	 * @param request the {@link RegisterUserRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleRegisterUserReuqest(WsData wsData,
			RegisterUserRequest request) throws OpenemsNamedException {
		this.parent.metadata.registerUser(request.getUser(), request.getOem());

		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	/**
	 * Handles a {@link LogoutRequest}.
	 *
	 * @param wsData  the WebSocket attachment
	 * @param user    the authenticated {@link User}
	 * @param request the {@link LogoutRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleLogoutRequest(WsData wsData, User user,
			LogoutRequest request) throws OpenemsNamedException {
		wsData.logout();
		this.parent.metadata.logout(user);
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	/**
	 * Gets the authenticated User or throws an Exception if User is not
	 * authenticated.
	 *
	 * @param wsData  the WebSocket attachment
	 * @param request the JsonrpcRequest
	 * @return the {@link User}
	 * @throws OpenemsNamedException if User is not authenticated
	 */
	private User assertUser(WsData wsData, JsonrpcRequest request) throws OpenemsNamedException {
		var userIdOpt = wsData.getUserId();
		if (!userIdOpt.isPresent()) {
			throw OpenemsError.COMMON_USER_NOT_AUTHENTICATED
					.exception("User-ID is empty. Ignoring request [" + request.getMethod() + "]");
		}
		var userOpt = this.parent.metadata.getUser(userIdOpt.get());
		if (!userOpt.isPresent()) {
			throw OpenemsError.COMMON_USER_NOT_AUTHENTICATED.exception("User with ID [" + userIdOpt.get()
					+ "] is unknown. Ignoring request [" + request.getMethod() + "]");
		}
		return userOpt.get();
	}

	/**
	 * Handles an {@link EdgeRpcRequest}.
	 *
	 * @param wsData         the WebSocket attachment
	 * @param user           the authenticated {@link User}
	 * @param edgeRpcRequest the {@link EdgeRpcRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<EdgeRpcResponse> handleEdgeRpcRequest(WsData wsData, User user,
			EdgeRpcRequest edgeRpcRequest) throws OpenemsNamedException {
		var edgeId = edgeRpcRequest.getEdgeId();
		var request = edgeRpcRequest.getPayload();
		user.assertEdgeRoleIsAtLeast(EdgeRpcRequest.METHOD, edgeId, Role.GUEST);

		CompletableFuture<JsonrpcResponseSuccess> resultFuture;
		switch (request.getMethod()) {

		case SubscribeChannelsRequest.METHOD:
			resultFuture = this.handleSubscribeChannelsRequest(wsData, edgeId, user,
					SubscribeChannelsRequest.from(request));
			break;

		case SubscribeSystemLogRequest.METHOD:
			resultFuture = this.handleSubscribeSystemLogRequest(wsData, edgeId, user,
					SubscribeSystemLogRequest.from(request));
			break;

		default:
			// unable to handle; try generic handler
			return null;
		}

		// Wrap reply in EdgeRpcResponse
		var result = new CompletableFuture<EdgeRpcResponse>();
		resultFuture.whenComplete((r, ex) -> {
			if (ex != null) {
				result.completeExceptionally(ex);
			} else if (r != null) {
				result.complete(new EdgeRpcResponse(edgeRpcRequest.getId(), r));
			} else {
				result.completeExceptionally(
						new OpenemsNamedException(OpenemsError.JSONRPC_UNHANDLED_METHOD, request.getMethod()));
			}
		});
		return result;
	}

	/**
	 * Handles a {@link SubscribeChannelsRequest}.
	 *
	 * @param wsData  the WebSocket attachment
	 * @param edgeId  the Edge-ID
	 * @param user    the {@link User} - no specific level required
	 * @param request the SubscribeChannelsRequest
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSubscribeChannelsRequest(WsData wsData, String edgeId,
			User user, SubscribeChannelsRequest request) throws OpenemsNamedException {
		// activate SubscribedChannelsWorker
		var worker = wsData.getSubscribedChannelsWorker(edgeId);
		worker.handleSubscribeChannelsRequest(user.getRole(edgeId).orElse(Role.GUEST), request);

		// JSON-RPC response
		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	/**
	 * Handles a {@link SubscribeSystemLogRequest}.
	 *
	 * @param wsData  the WebSocket attachment
	 * @param edgeId  the Edge-ID
	 * @param user    the {@link User}
	 * @param request the {@link SubscribeSystemLogRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleSubscribeSystemLogRequest(WsData wsData, String edgeId,
			User user, SubscribeSystemLogRequest request) throws OpenemsNamedException {
		user.assertEdgeRoleIsAtLeast(SubscribeSystemLogRequest.METHOD, edgeId, Role.OWNER);
		var token = wsData.assertToken();

		// Forward to Edge
		return this.parent.edgeWebsocket.handleSubscribeSystemLogRequest(edgeId, user, token, request);
	}

	/**
	 * Handles an {@link AddEdgeToUserRequest}.
	 *
	 * @param user    the {@link User}
	 * @param request the {@link AddEdgeToUserRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<AddEdgeToUserResponse> handleAddEdgeToUserRequest(User user, AddEdgeToUserRequest request)
			throws OpenemsNamedException {
		var edge = this.parent.metadata.addEdgeToUser(user, request.getSetupPassword());

		return CompletableFuture.completedFuture(new AddEdgeToUserResponse(request.getId(), edge));
	}

	/**
	 * Handles a {@link GetUserInformationRequest}.
	 *
	 * @param user    the {@link User}
	 * @param request the {@link GetUserInformationRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GetUserInformationResponse> handleGetUserInformationRequest(User user,
			GetUserInformationRequest request) throws OpenemsNamedException {
		var userInformation = this.parent.metadata.getUserInformation(user);

		return CompletableFuture.completedFuture(new GetUserInformationResponse(request.getId(), userInformation));
	}

	/**
	 * Handles a {@link SetUserInformationRequest}.
	 *
	 * @param user    the {@link User}r
	 * @param request the {@link SetUserInformationRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GenericJsonrpcResponseSuccess> handleSetUserInformationRequest(User user,
			SetUserInformationRequest request) throws OpenemsNamedException {
		this.parent.metadata.setUserInformation(user, request.getJsonObject());

		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	/**
	 * Handles a {@link SubmitSetupProtocolRequest}.
	 *
	 * @param user    the {@link User}r
	 * @param request the {@link SubmitSetupProtocolRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GenericJsonrpcResponseSuccess> handleSubmitSetupProtocolRequest(User user,
			SubmitSetupProtocolRequest request) throws OpenemsNamedException {
		var protocolId = this.parent.metadata.submitSetupProtocol(user, request.getJsonObject());

		var response = JsonUtils.buildJsonObject() //
				.addProperty("setupProtocolId", protocolId) //
				.build();

		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId(), response));
	}

	/**
	 * Handles a {@link GetSetupProtocolRequest}.
	 *
	 * @param user    the {@link User}r
	 * @param request the {@link GetSetupProtocolRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<Base64PayloadResponse> handleGetSetupProtocolRequest(User user,
			GetSetupProtocolRequest request) throws OpenemsNamedException {
		var protocol = this.parent.metadata.getSetupProtocol(user, request.getSetupProtocolId());

		return CompletableFuture.completedFuture(new Base64PayloadResponse(request.getId(), protocol));
	}

	/**
	 * Handles a {@link UpdateUserLanguageRequest}.
	 *
	 * @param user    the {@link User}r
	 * @param request the {@link UpdateUserLanguageRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GenericJsonrpcResponseSuccess> handleUpdateUserLanguageRequest(User user,
			UpdateUserLanguageRequest request) throws OpenemsNamedException {
		this.parent.metadata.updateUserLanguage(user, request.getLanguage());

		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	/**
	 * Handles a {@link GetSetupProtocolDataRequest}.
	 * 
	 * @param user    the {@link User}
	 * @param request the {@link GetSetupProtocolDataRequest}
	 * @return the JSON-RPC Success Response Future
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<GenericJsonrpcResponseSuccess> handleGetSetupProtocolDataRequest(User user,
			GetSetupProtocolDataRequest request) throws OpenemsNamedException {
		var latestProtocolJson = this.parent.metadata.getSetupProtocolData(user, request.getEdgeId());

		return CompletableFuture
				.completedFuture(new GenericJsonrpcResponseSuccess(request.getId(), latestProtocolJson));
	}

	/**
	 * Handles a {@link SendLogMessageRequest}. Logs given message from request.
	 * 
	 * @param user    the {@link User}r
	 * @param request the {@link SendLogMessageRequest}
	 * @return the JSON-RPC Success Response Future
	 */
	private CompletableFuture<GenericJsonrpcResponseSuccess> handleSendLogMessageRequest(User user,
			SendLogMessageRequest request) {
		var msg = "User [" + user.getId() + ":" + user.getName() + "] UI: " + request.getParams();

		switch (request.getLevel()) {
		case DEBUG:
			this.log.debug(msg);
			break;
		case INFO:
			this.log.info(msg);
			break;
		case WARNING:
			this.log.warn(msg);
			break;
		case ERROR:
			this.log.error(msg);
			break;
		default:
			this.log.warn("Unable to log message with level [" + request.getLevel() + "]");
			break;
		}

		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

}
