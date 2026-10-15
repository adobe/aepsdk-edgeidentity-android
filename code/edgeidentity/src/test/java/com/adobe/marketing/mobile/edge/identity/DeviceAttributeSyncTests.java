/*
  Copyright 2026 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.edge.identity;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import android.app.Application;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.SharedStateResult;
import com.adobe.marketing.mobile.SharedStateStatus;
import com.adobe.marketing.mobile.services.NamedCollection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

public class DeviceAttributeSyncTests {

	private ExtensionApi api;
	private Map<String, String> stored;
	private NamedCollection collection;
	private List<Event> dispatched;
	private List<Map<String, Object>> published;
	private DeviceAttributeSync sync;
	private MockedStatic<MobileCore> mobileCore;
	private Application application;

	@Before
	public void setup() {
		application = mock(Application.class);
		when(application.getPackageName()).thenReturn("com.example");
		mobileCore = mockStatic(MobileCore.class);
		mobileCore.when(MobileCore::getApplication).thenReturn(application);
		api = mock(ExtensionApi.class);
		stored = new HashMap<>();
		collection = mock(NamedCollection.class);
		when(collection.getString(anyString(), any())).thenAnswer(call -> stored.get(call.<String>getArgument(0)));
		doAnswer(call -> stored.put(call.getArgument(0), call.getArgument(1)))
			.when(collection)
			.setString(anyString(), anyString());
		doAnswer(call -> stored.remove(call.<String>getArgument(0))).when(collection).remove(anyString());
		doAnswer(call -> {
				stored.clear();
				return null;
			})
			.when(collection)
			.removeAll();
		dispatched = new ArrayList<>();
		published = new ArrayList<>();
		doAnswer(call -> dispatched.add(call.getArgument(0))).when(api).dispatch(any());
		doAnswer(call -> published.add(call.getArgument(0))).when(api).createSharedState(anyMap(), any());
		sync = new DeviceAttributeSync(api, new ProfileAttributeStore(collection));
	}

	@After
	public void teardown() {
		mobileCore.close();
	}

	private static Event request(final Map<String, Object> data) {
		return new Event.Builder(
			IdentityConstants.EventNames.UPDATE_DEVICE_ATTRIBUTES,
			EventType.EDGE_IDENTITY,
			EventSource.REQUEST_CONTENT
		)
			.setEventData(data)
			.build();
	}

	private static Event consequence(final Map<String, Object> data) {
		return new Event.Builder(
			"Forward operational data",
			EventType.EDGE_IDENTITY,
			DeviceAttributeSync.RULE_RESPONSE_SOURCE
		)
			.setEventData(data)
			.build();
	}

	private void dedupConfig(final boolean enabled) {
		when(api.getSharedState(eq(IdentityConstants.SharedState.Configuration.NAME), any(), anyBoolean(), any()))
			.thenReturn(
				new SharedStateResult(
					SharedStateStatus.SET,
					Map.of("edgeidentity.optimizeOperationalProfileDataSync", enabled)
				)
			);
	}

	private void edgeResult(final Event edgeRequest, final String status) {
		sync.handleDeviceAttributeResult(
			new Event.Builder(
				"Device Attributes Response",
				DeviceAttributeSync.BYPASS_TYPE,
				EventSource.RESPONSE_CONTENT
			)
				.setEventData(Map.of("status", status))
				.chainToParentEvent(edgeRequest)
				.build()
		);
	}

	private void succeedAll() {
		for (int i = 0; i < dispatched.size(); i++) {
			final Event event = dispatched.get(i);
			if (DeviceAttributeSync.BYPASS_TYPE.equals(event.getType())) edgeResult(event, "success");
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> lastProfileAttributes() {
		return (Map<String, Object>) published.get(published.size() - 1).get("profileAttributes");
	}

	@Test
	public void handleDeviceAttributes_dispatchesOperationalDataChainedToApiEvent() {
		final Event request = request(Map.of("timezone", "Asia/Tokyo"));
		sync.handleDeviceAttributes(request);

		assertEquals(1, dispatched.size());
		final Event operational = dispatched.get(0);
		assertEquals(IdentityConstants.DeviceAttributes.OPERATIONAL_DATA_TYPE, operational.getType());
		assertEquals(EventSource.REQUEST_CONTENT, operational.getSource());
		assertEquals(request.getUniqueIdentifier(), operational.getParentID());
		assertNull(operational.getResponseID());
		assertEquals("Asia/Tokyo", operational.getEventData().get("timezone"));
		assertFalse(operational.getEventData().containsKey("app"));
	}

	@Test
	public void ruleResponse_pushOnlyAddsSdkAppAfterFilteringAndOverridesCallerApp() {
		sync.handleDeviceAttributeRuleResponse(
			consequence(
				Map.of(
					"tokens",
					Map.of("pushNotification", "token-1"),
					"app",
					Map.of("id", "caller.app", "platform", "apns")
				)
			)
		);
		assertEquals(
			Map.of(
				"tokens",
				Map.of("pushNotification", "token-1"),
				"app",
				Map.of("id", "com.example", "platform", "fcm")
			),
			dispatched.get(0).getEventData()
		);
	}

	@Test
	public void ruleResponse_missingApplicationDoesNotSendOrPersist() {
		mobileCore.when(MobileCore::getApplication).thenReturn(null);
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));
		assertTrue(dispatched.isEmpty());
		assertTrue(stored.isEmpty());
		assertTrue(published.isEmpty());
	}

	@Test
	public void ruleResponse_emptyPackageDoesNotSendOrPersist() {
		when(application.getPackageName()).thenReturn("");
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		assertTrue(dispatched.isEmpty());
		assertTrue(stored.isEmpty());
		assertTrue(published.isEmpty());
	}

	@Test
	public void handleDeviceAttributes_ignoresEmptyOrInvalidData() {
		sync.handleDeviceAttributes(request(new HashMap<>()));
		sync.handleDeviceAttributes(request(null));
		assertTrue(dispatched.isEmpty());
	}

	@Test
	public void ruleResponse_sendsForwardedValuesToEdgeAndPublishesState() {
		final Map<String, Object> app = Map.of("id", "com.example", "platform", "fcm");
		final Event forward = consequence(Map.of("timezone", "Asia/Tokyo", "app", app));
		sync.handleDeviceAttributeRuleResponse(forward);

		assertEquals(1, dispatched.size());
		final Event edge = dispatched.get(0);
		assertEquals(DeviceAttributeSync.BYPASS_TYPE, edge.getType());
		assertEquals(EventSource.REQUEST_CONTENT, edge.getSource());
		assertEquals(forward.getUniqueIdentifier(), edge.getParentID());
		assertEquals(Map.of("timezone", "Asia/Tokyo", "app", app), edge.getEventData());
		assertNull(stored.get("timeZone"));
		assertTrue(published.isEmpty());

		edgeResult(edge, "success");
		assertEquals("Asia/Tokyo", stored.get("timeZone"));
		assertEquals(Map.of("timezone", "Asia/Tokyo"), lastProfileAttributes());
	}

	@Test
	public void edgeSuccess_publishesPushTokenForMessaging() {
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));
		assertEquals(Map.of("id", "com.example", "platform", "fcm"), dispatched.get(0).getEventData().get("app"));
		edgeResult(dispatched.get(0), "success");
		assertEquals(Map.of("tokens", Map.of("pushNotification", "token-1")), lastProfileAttributes());
		assertNotNull(stored.get("deviceAttributes"));
	}

	@Test
	public void persistedPushToken_isDedupedAfterRestart() {
		final Map<String, Object> token = Map.of("tokens", Map.of("pushNotification", "token-1"));
		sync.handleDeviceAttributeRuleResponse(consequence(token));
		edgeResult(dispatched.get(0), "success");

		sync = new DeviceAttributeSync(api, new ProfileAttributeStore(collection));
		sync.handleDeviceAttributeRuleResponse(consequence(token));

		assertEquals(1, dispatched.size());
	}

	@Test
	public void persistedNull_isDedupedAfterRestart() {
		final Map<String, Object> tokens = new HashMap<>();
		tokens.put("pushNotification", null);
		final Map<String, Object> attributes = Map.of("tokens", tokens);
		sync.handleDeviceAttributeRuleResponse(consequence(attributes));
		edgeResult(dispatched.get(0), "success");
		assertTrue(stored.get("deviceAttributes").contains("\"pushNotification\":null"));
		assertEquals(attributes, new ProfileAttributeStore(collection).getMap("deviceAttributes"));

		sync = new DeviceAttributeSync(api, new ProfileAttributeStore(collection));
		sync.handleDeviceAttributeRuleResponse(consequence(attributes));

		assertEquals(1, dispatched.size());
	}

	@Test
	public void consentYes_doesNotSendOrPersistDeviceAttributes() {
		when(api.getXDMSharedState(eq(IdentityConstants.SharedState.Consent.NAME), any(), anyBoolean(), any()))
			.thenReturn(
				new SharedStateResult(SharedStateStatus.SET, Map.of("consents", Map.of("collect", Map.of("val", "y"))))
			);
		when(api.getSharedState(eq(IdentityConstants.SharedState.Messaging.NAME), any(), anyBoolean(), any()))
			.thenReturn(
				new SharedStateResult(
					SharedStateStatus.SET,
					Map.of(IdentityConstants.SharedState.Messaging.PUSH_IDENTIFIER, "token-1")
				)
			);

		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));

		assertTrue(dispatched.isEmpty());
		assertTrue(stored.isEmpty());
		assertTrue(published.isEmpty());
	}

	@Test
	public void consentYes_changedPushTokenIsSentAfterComparisonWithMessagingState() {
		when(api.getXDMSharedState(eq(IdentityConstants.SharedState.Consent.NAME), any(), anyBoolean(), any()))
			.thenReturn(
				new SharedStateResult(SharedStateStatus.SET, Map.of("consents", Map.of("collect", Map.of("val", "y"))))
			);
		when(api.getSharedState(eq(IdentityConstants.SharedState.Messaging.NAME), any(), anyBoolean(), any()))
			.thenReturn(
				new SharedStateResult(
					SharedStateStatus.SET,
					Map.of(IdentityConstants.SharedState.Messaging.PUSH_IDENTIFIER, "token-old")
				)
			);

		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-new"))));

		assertEquals(1, dispatched.size());
		assertEquals(
			Map.of(
				"tokens",
				Map.of("pushNotification", "token-new"),
				"app",
				Map.of("id", "com.example", "platform", "fcm")
			),
			dispatched.get(0).getEventData()
		);
		edgeResult(dispatched.get(0), "success");
		assertEquals(Map.of("tokens", Map.of("pushNotification", "token-new")), lastProfileAttributes());
	}

	@Test
	public void consentYes_timezoneUsesPersistedOwnerValueForDedup() {
		stored.put("timeZone", "Asia/Tokyo");
		when(api.getXDMSharedState(eq(IdentityConstants.SharedState.Consent.NAME), any(), anyBoolean(), any()))
			.thenReturn(
				new SharedStateResult(SharedStateStatus.SET, Map.of("consents", Map.of("collect", Map.of("val", "y"))))
			);

		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));

		assertTrue(dispatched.isEmpty());
	}

	@Test
	public void consentExtensionNotRegistered_defaultsToYesAndSkipsBypass() {
		when(api.getSharedState(eq(IdentityConstants.SharedState.Hub.NAME), any(), anyBoolean(), any()))
			.thenReturn(
				new SharedStateResult(
					SharedStateStatus.SET,
					Map.of("extensions", Map.of(IdentityConstants.SharedState.Consent.NAME, Map.of()))
				)
			);
		when(api.getSharedState(eq(IdentityConstants.SharedState.Messaging.NAME), any(), anyBoolean(), any()))
			.thenReturn(
				new SharedStateResult(
					SharedStateStatus.SET,
					Map.of(IdentityConstants.SharedState.Messaging.PUSH_IDENTIFIER, "token-1")
				)
			);

		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));

		assertTrue(dispatched.isEmpty());
		assertTrue(stored.isEmpty());
	}

	@Test
	public void consentNo_sendsDeviceAttributesThroughBypass() {
		when(api.getXDMSharedState(eq(IdentityConstants.SharedState.Consent.NAME), any(), anyBoolean(), any()))
			.thenReturn(
				new SharedStateResult(SharedStateStatus.SET, Map.of("consents", Map.of("collect", Map.of("val", "n"))))
			);

		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));

		assertEquals(1, dispatched.size());
		assertEquals(DeviceAttributeSync.BYPASS_TYPE, dispatched.get(0).getType());
	}

	@Test
	public void consentNo_unchangedValueIsDedupedAgainstPersistedDeviceStore() {
		stored.put("deviceAttributes", "{\"tokens\":{\"pushNotification\":\"token-1\"}}");
		sync = new DeviceAttributeSync(api, new ProfileAttributeStore(collection));
		when(api.getXDMSharedState(eq(IdentityConstants.SharedState.Consent.NAME), any(), anyBoolean(), any()))
			.thenReturn(
				new SharedStateResult(SharedStateStatus.SET, Map.of("consents", Map.of("collect", Map.of("val", "n"))))
			);

		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));

		assertTrue(dispatched.isEmpty());
	}

	@Test
	public void pendingConsent_sendsDeviceAttributesThroughBypass() {
		when(api.getXDMSharedState(eq(IdentityConstants.SharedState.Consent.NAME), any(), anyBoolean(), any()))
			.thenReturn(new SharedStateResult(SharedStateStatus.PENDING, null));
		when(api.getSharedState(eq(IdentityConstants.SharedState.Hub.NAME), any(), anyBoolean(), any()))
			.thenReturn(
				new SharedStateResult(
					SharedStateStatus.SET,
					Map.of("extensions", Map.of(IdentityConstants.SharedState.Consent.NAME, Map.of("version", "1.0.0")))
				)
			);

		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));

		assertEquals(1, dispatched.size());
		assertEquals(DeviceAttributeSync.BYPASS_TYPE, dispatched.get(0).getType());
	}

	@Test
	public void edgeFailure_doesNotPublishAndAllowsResend() {
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		edgeResult(dispatched.get(0), "failed");
		assertNull(stored.get("timeZone"));
		assertTrue(published.isEmpty());

		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		assertEquals(2, dispatched.size());
	}

	@Test
	public void edgeResult_forUnknownRequestIsIgnored() {
		edgeResult(
			new Event.Builder("Other", DeviceAttributeSync.BYPASS_TYPE, EventSource.REQUEST_CONTENT).build(),
			"success"
		);
		assertTrue(published.isEmpty());
	}

	@Test
	public void ruleResponse_appOnlyConsequenceIsNotSent() {
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("app", Map.of("id", "com.example"))));
		assertTrue(dispatched.isEmpty());
	}

	@Test
	public void multipleConsequences_areSentInOrderAndLaterDuplicatesAreDropped() {
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleDeviceAttributeRuleResponse(
			consequence(Map.of("timezone", "Asia/Tokyo", "tokens", Map.of("pushNotification", Map.of("token", "t1"))))
		);

		assertEquals(1, dispatched.size());
		assertTrue(published.isEmpty());
		edgeResult(dispatched.get(0), "success");
		assertEquals(2, dispatched.size());
		assertEquals(
			Map.of("timezone", "Asia/Tokyo", "app", Map.of("id", "com.example", "platform", "fcm")),
			dispatched.get(0).getEventData()
		);
		assertEquals(
			Map.of(
				"tokens",
				Map.of("pushNotification", Map.of("token", "t1")),
				"app",
				Map.of("id", "com.example", "platform", "fcm")
			),
			dispatched.get(1).getEventData()
		);
	}

	@Test
	public void changedValue_isSentAgain() {
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Europe/Paris")));
		assertEquals(1, dispatched.size());
		succeedAll();

		assertEquals(2, dispatched.size());
		assertEquals("Europe/Paris", dispatched.get(1).getEventData().get("timezone"));
		assertEquals("Europe/Paris", stored.get("timeZone"));
	}

	@Test
	public void persistedTimezone_isDedupedAfterRestart() {
		stored.put("deviceAttributes", "{\"timezone\":\"Asia/Tokyo\"}");
		sync = new DeviceAttributeSync(api, new ProfileAttributeStore(collection));
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		assertTrue(dispatched.isEmpty());
	}

	@Test
	public void dedupEnabledByConfiguration_skipsConfirmedDuplicates() {
		dedupConfig(true);
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		edgeResult(dispatched.get(0), "success");
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		assertEquals(1, dispatched.size());
	}

	@Test
	public void dedupDisabledByConfiguration_sendsDuplicates() {
		dedupConfig(false);
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		assertEquals(1, dispatched.size());
		edgeResult(dispatched.get(0), "success");
		assertEquals(2, dispatched.size());
	}

	@Test
	public void explicitNull_isSentAndRemembered() {
		final Map<String, Object> data = new HashMap<>();
		data.put("tokens", new HashMap<>(Map.of("pushNotification", Map.of("token", "s1"))));
		sync.handleDeviceAttributeRuleResponse(consequence(data));
		final Map<String, Object> tokens = new HashMap<>();
		tokens.put("pushNotification", null);
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("tokens", tokens)));
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("tokens", tokens)));
		succeedAll();

		assertEquals(2, dispatched.size());
		assertEquals(
			Map.of("tokens", tokens, "app", Map.of("id", "com.example", "platform", "fcm")),
			dispatched.get(1).getEventData()
		);
	}

	@Test
	public void customNestedValues_areDedupedPerLeaf() {
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("custom", Map.of("a", 1, "b", 2))));
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("custom", Map.of("a", 1, "b", 3))));
		edgeResult(dispatched.get(0), "success");

		assertEquals(2, dispatched.size());
		assertEquals(
			Map.of("custom", Map.of("b", 3), "app", Map.of("id", "com.example", "platform", "fcm")),
			dispatched.get(1).getEventData()
		);
	}

	@Test
	public void reset_clearsSentValues() {
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo", "custom", Map.of("a", 1))));
		succeedAll();
		sync.reset(null);
		assertNull(stored.get("timeZone"));
		assertNull(stored.get("deviceAttributes"));
		assertTrue(lastProfileAttributes().isEmpty());

		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo", "custom", Map.of("a", 1))));
		assertEquals(2, dispatched.size());
	}

	@Test
	public void queuedDuplicate_isSentAfterFailure() {
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		final Event queued = consequence(Map.of("timezone", "Asia/Tokyo"));
		sync.handleDeviceAttributeRuleResponse(queued);
		assertEquals(1, dispatched.size());

		edgeResult(dispatched.get(0), "failed");
		assertNull(stored.get("timeZone"));
		assertTrue(published.isEmpty());
		assertEquals(2, dispatched.size());
		assertEquals(queued.getUniqueIdentifier(), dispatched.get(1).getParentID());

		edgeResult(dispatched.get(1), "success");
		assertEquals("Asia/Tokyo", stored.get("timeZone"));
		assertEquals(Map.of("timezone", "Asia/Tokyo"), lastProfileAttributes());
	}

	@Test
	public void droppedRequest_releasesNextConsequence() {
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Europe/Paris")));
		edgeResult(dispatched.get(0), "dropped");
		assertEquals(2, dispatched.size());
		assertTrue(published.isEmpty());
		assertEquals("Europe/Paris", dispatched.get(1).getEventData().get("timezone"));
	}

	@Test
	public void unrelatedAndDuplicateResults_doNotReleaseNextRequest() {
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Europe/Paris")));
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Europe/London")));
		edgeResult(consequence(Map.of()), "success");
		assertEquals(1, dispatched.size());

		edgeResult(dispatched.get(0), "success");
		edgeResult(dispatched.get(0), "success");
		assertEquals(2, dispatched.size());
		assertEquals("Asia/Tokyo", stored.get("timeZone"));

		edgeResult(dispatched.get(1), "success");
		assertEquals(3, dispatched.size());
		assertEquals("Europe/Paris", stored.get("timeZone"));
	}

	@Test
	public void reset_discardsQueueAndIgnoresLateResult() {
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		final Event oldRequest = dispatched.get(0);
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Europe/Paris")));
		sync.reset(null);
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Europe/London")));
		edgeResult(oldRequest, "success");

		assertEquals(2, dispatched.size());
		assertNull(stored.get("timeZone"));
		assertTrue(lastProfileAttributes().isEmpty());
		edgeResult(dispatched.get(1), "success");
		assertEquals("Europe/London", stored.get("timeZone"));
	}

	@Test
	public void success_publishesBeforeDispatchingNextRequest() {
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Europe/Paris")));
		doAnswer(call -> {
				assertEquals("Asia/Tokyo", stored.get("timeZone"));
				assertEquals(Map.of("timezone", "Asia/Tokyo"), lastProfileAttributes());
				dispatched.add(call.getArgument(0));
				return null;
			})
			.when(api)
			.dispatch(any());

		edgeResult(dispatched.get(0), "success");
		assertEquals(2, dispatched.size());
	}

	@Test
	public void queue_skipsUnchangedAndEmptyConsequencesWithoutBlockingLaterChanges() {
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("app", Map.of("id", "com.example"))));
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Europe/Paris")));

		edgeResult(dispatched.get(0), "success");
		assertEquals(2, dispatched.size());
		assertEquals("Europe/Paris", dispatched.get(1).getEventData().get("timezone"));
	}

	@Test
	public void timezoneClear_isDedupedOnlyAfterSuccess() {
		stored.put("timeZone", "Asia/Tokyo");
		final Map<String, Object> clear = new HashMap<>();
		clear.put("timezone", null);
		sync.handleDeviceAttributeRuleResponse(consequence(clear));
		sync.handleDeviceAttributeRuleResponse(consequence(clear));
		edgeResult(dispatched.get(0), "failed");
		assertEquals("Asia/Tokyo", stored.get("timeZone"));
		assertEquals(2, dispatched.size());

		edgeResult(dispatched.get(1), "success");
		assertNull(stored.get("timeZone"));
		sync.handleDeviceAttributeRuleResponse(consequence(clear));
		assertEquals(2, dispatched.size());
		assertEquals(clear, lastProfileAttributes());
	}
}
