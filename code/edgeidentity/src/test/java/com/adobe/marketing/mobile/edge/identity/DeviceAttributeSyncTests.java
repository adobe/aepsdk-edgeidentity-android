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
import java.util.Collections;
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
	private List<Event> results;
	private Map<String, String> deviceStored;
	private NamedCollection deviceCollection;
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
		results = new ArrayList<>();
		deviceStored = new HashMap<>();
		deviceCollection = mock(NamedCollection.class);
		when(deviceCollection.getString(anyString(), any()))
			.thenAnswer(call -> deviceStored.get(call.<String>getArgument(0)));
		doAnswer(call -> deviceStored.put(call.getArgument(0), call.getArgument(1)))
			.when(deviceCollection)
			.setString(anyString(), anyString());
		doAnswer(call -> deviceStored.remove(call.<String>getArgument(0))).when(deviceCollection).remove(anyString());
		published = new ArrayList<>();
		doAnswer(call -> {
				final Event event = call.getArgument(0);
				return isResultEvent(event) ? results.add(event) : dispatched.add(event);
			})
			.when(api)
			.dispatch(any());
		doAnswer(call -> published.add(call.getArgument(0))).when(api).createSharedState(anyMap(), any());
		sync = newSync();
	}

	private DeviceAttributeSync newSync() {
		return new DeviceAttributeSync(
			api,
			new ProfileAttributeStore(collection),
			new ProfileAttributeStore(deviceCollection)
		);
	}

	@After
	public void teardown() {
		mobileCore.close();
	}

	private static boolean isResultEvent(final Event event) {
		return (
			EventType.EDGE_IDENTITY.equals(event.getType()) && EventSource.RESPONSE_CONTENT.equals(event.getSource())
		);
	}

	private void seedSynced(final String json) {
		deviceStored.put(IdentityConstants.DeviceAttributes.SYNCED_VALUES, json);
		sync = newSync();
	}

	private void consentRegistered() {
		when(api.getSharedState(eq(IdentityConstants.SharedState.Hub.NAME), any(), anyBoolean(), any()))
			.thenReturn(
				new SharedStateResult(
					SharedStateStatus.SET,
					Map.of("extensions", Map.of(IdentityConstants.SharedState.Consent.NAME, Map.of("version", "3.0.0")))
				)
			);
	}

	private void collectConsent(final String value) {
		consentRegistered();
		when(api.getXDMSharedState(eq(IdentityConstants.SharedState.Consent.NAME), any(), anyBoolean(), any()))
			.thenReturn(
				new SharedStateResult(
					SharedStateStatus.SET,
					Map.of("consents", Map.of("collect", Map.of("val", value)))
				)
			);
	}

	private void messagingPushToken(final String token) {
		when(api.getSharedState(eq(IdentityConstants.SharedState.Messaging.NAME), any(), anyBoolean(), any()))
			.thenReturn(
				new SharedStateResult(
					SharedStateStatus.SET,
					Map.of(IdentityConstants.SharedState.Messaging.PUSH_IDENTIFIER, token)
				)
			);
	}

	@SuppressWarnings("unchecked")
	/** Mirrors IdentityExtension reset: IdentityState clears the profile store before the sync resets. */
	private void resetLikeExtension() {
		stored.clear();
		sync.reset(null);
	}

	private Map<String, Object> syncedValues() {
		return new ProfileAttributeStore(deviceCollection).getMap(IdentityConstants.DeviceAttributes.SYNCED_VALUES);
	}

	private void edgeResponse(final Map<String, Object> data, final Event parent) {
		final Event.Builder builder = new Event.Builder(
			"Device Attributes Response",
			IdentityConstants.DeviceAttributes.BYPASS_TYPE,
			EventSource.RESPONSE_CONTENT
		)
			.setEventData(data);
		if (parent != null) builder.chainToParentEvent(parent);
		sync.handleDeviceAttributeResult(builder.build());
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
			IdentityConstants.DeviceAttributes.RULE_RESPONSE_SOURCE
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
		edgeResponse(Map.of("status", status), edgeRequest);
	}

	private void succeedAll() {
		for (int i = 0; i < dispatched.size(); i++) {
			final Event event = dispatched.get(i);
			if (IdentityConstants.DeviceAttributes.BYPASS_TYPE.equals(event.getType())) edgeResult(event, "success");
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
		assertEquals(IdentityConstants.DeviceAttributes.BYPASS_TYPE, edge.getType());
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
		assertEquals(Map.of("tokens", Map.of("pushNotification", "token-1")), syncedValues());
	}

	@Test
	public void persistedPushToken_isDedupedAfterRestart() {
		final Map<String, Object> token = Map.of("tokens", Map.of("pushNotification", "token-1"));
		sync.handleDeviceAttributeRuleResponse(consequence(token));
		edgeResult(dispatched.get(0), "success");

		sync = newSync();
		sync.handleDeviceAttributeRuleResponse(consequence(token));

		assertEquals(1, dispatched.size());
	}

	@Test
	public void nullPushToken_isDroppedBeforeAndAfterRestart() {
		final Map<String, Object> tokens = new HashMap<>();
		tokens.put("pushNotification", null);
		final Map<String, Object> attributes = Map.of("tokens", tokens);
		sync.handleDeviceAttributeRuleResponse(consequence(attributes));
		assertTrue(dispatched.isEmpty());
		assertTrue(deviceStored.isEmpty());
		sync = newSync();
		sync.handleDeviceAttributeRuleResponse(consequence(attributes));

		assertTrue(dispatched.isEmpty());
	}

	@Test
	public void consentYes_ownerMatchIsNotSentAndIsCopiedToStore() {
		collectConsent("y");
		messagingPushToken("token-1");

		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));

		assertTrue(dispatched.isEmpty());
		assertTrue(results.isEmpty());
		assertTrue(stored.isEmpty());
		assertEquals(Map.of("tokens", Map.of("pushNotification", "token-1")), syncedValues());
	}

	@Test
	public void ownerMatch_publishesStandardStateChainedToRuleResponse() {
		collectConsent("y");
		messagingPushToken("token-1");
		final Event rule = consequence(
			Map.of("timezone", "Asia/Tokyo", "tokens", Map.of("pushNotification", "token-1"))
		);

		sync.handleDeviceAttributeRuleResponse(rule);

		assertEquals(1, dispatched.size());
		final Map<String, Object> expected = Map.of("tokens", Map.of("pushNotification", "token-1"));
		verify(api).createSharedState(eq(Collections.singletonMap("profileAttributes", expected)), eq(rule));
		assertEquals(1, published.size());
	}

	@Test
	public void ownerMatch_publishesEvenWhenEverythingIsDeduped() {
		collectConsent("y");
		messagingPushToken("token-1");
		final Event rule = consequence(Map.of("tokens", Map.of("pushNotification", "token-1")));

		sync.handleDeviceAttributeRuleResponse(rule);

		assertTrue(dispatched.isEmpty());
		final Map<String, Object> expected = Map.of("tokens", Map.of("pushNotification", "token-1"));
		verify(api).createSharedState(eq(Collections.singletonMap("profileAttributes", expected)), eq(rule));
		assertEquals(1, published.size());
	}

	@Test
	public void ownerMatch_publishesEvenWithoutAppIdentifier() {
		when(application.getPackageName()).thenReturn("");
		collectConsent("y");
		messagingPushToken("token-1");
		final Event rule = consequence(
			Map.of("timezone", "Asia/Tokyo", "tokens", Map.of("pushNotification", "token-1"))
		);

		sync.handleDeviceAttributeRuleResponse(rule);

		assertTrue(dispatched.isEmpty());
		final Map<String, Object> expected = Map.of("tokens", Map.of("pushNotification", "token-1"));
		verify(api).createSharedState(eq(Collections.singletonMap("profileAttributes", expected)), eq(rule));
		assertEquals(expected, syncedValues());
	}

	@Test
	public void noOwnerMatch_doesNotPublish() {
		collectConsent("y");
		messagingPushToken("token-old");
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-new"))));
		assertEquals(1, dispatched.size());
		assertTrue(published.isEmpty());

		edgeResult(dispatched.get(0), "success");
		assertEquals(1, published.size());
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-new"))));
		assertEquals(1, published.size());
	}

	@Test
	public void ownerMatchCopiedToStore_isDedupedWhenConsentIsLaterDenied() {
		stored.put("timeZone", "Asia/Tokyo");
		collectConsent("y");
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		assertTrue(dispatched.isEmpty());

		collectConsent("n");
		sync = newSync();
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		assertTrue(dispatched.isEmpty());
	}

	@Test
	public void consentYes_ownerMatchCopiesOnlyMatchingLeaf() {
		collectConsent("y");
		messagingPushToken("token-1");

		sync.handleDeviceAttributeRuleResponse(
			consequence(Map.of("timezone", "Asia/Tokyo", "tokens", Map.of("pushNotification", "token-1")))
		);

		assertEquals(
			Map.of("timezone", "Asia/Tokyo", "app", Map.of("id", "com.example", "platform", "fcm")),
			dispatched.get(0).getEventData()
		);
		assertEquals(Map.of("tokens", Map.of("pushNotification", "token-1")), syncedValues());
	}

	@Test
	public void consentYes_changedPushTokenIsSentAfterComparisonWithMessagingState() {
		collectConsent("y");
		messagingPushToken("token-old");

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
		collectConsent("y");

		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));

		assertTrue(dispatched.isEmpty());
	}

	@Test
	public void consentExtensionNotRegistered_treatsConsentAsYes() {
		when(api.getSharedState(eq(IdentityConstants.SharedState.Hub.NAME), any(), anyBoolean(), any()))
			.thenReturn(
				new SharedStateResult(
					SharedStateStatus.SET,
					Map.of("extensions", Map.of("com.adobe.edge", Map.of("version", "3.0.0")))
				)
			);
		messagingPushToken("token-1");

		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));

		assertTrue(dispatched.isEmpty());
		assertEquals(Map.of("tokens", Map.of("pushNotification", "token-1")), syncedValues());
		verify(api, never())
			.getXDMSharedState(eq(IdentityConstants.SharedState.Consent.NAME), any(), anyBoolean(), any());
	}

	@Test
	public void hubSharedStateUnavailable_doesNotTreatConsentAsYes() {
		messagingPushToken("token-1");

		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));

		assertEquals(1, dispatched.size());
		assertTrue(deviceStored.isEmpty());
	}

	@Test
	public void consentNo_sendsDeviceAttributesThroughBypass() {
		collectConsent("n");
		stored.put("timeZone", "Asia/Tokyo");

		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));

		assertEquals(1, dispatched.size());
		assertEquals(IdentityConstants.DeviceAttributes.BYPASS_TYPE, dispatched.get(0).getType());
	}

	@Test
	public void consentNo_unchangedValueIsDedupedAgainstPersistedDeviceStore() {
		seedSynced("{\"tokens\":{\"pushNotification\":\"token-1\"}}");
		collectConsent("n");

		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));

		assertTrue(dispatched.isEmpty());
	}

	@Test
	public void pendingConsent_sendsDeviceAttributesThroughBypass() {
		consentRegistered();
		stored.put("timeZone", "Asia/Tokyo");
		when(api.getXDMSharedState(eq(IdentityConstants.SharedState.Consent.NAME), any(), anyBoolean(), any()))
			.thenReturn(new SharedStateResult(SharedStateStatus.PENDING, null));

		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));

		assertEquals(1, dispatched.size());
		assertEquals(IdentityConstants.DeviceAttributes.BYPASS_TYPE, dispatched.get(0).getType());
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
			new Event.Builder("Other", IdentityConstants.DeviceAttributes.BYPASS_TYPE, EventSource.REQUEST_CONTENT)
				.build(),
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
	public void parentThenChild_confirmationReplacesParentAndDrainsQueue() {
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("prefs", Map.of())));
		edgeResult(dispatched.get(0), "success");
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("prefs", Map.of("lang", "en"))));
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Europe/Paris")));
		edgeResult(dispatched.get(1), "success");

		assertEquals(Map.of("prefs", Map.of("lang", "en")), lastProfileAttributes());
		assertEquals(Map.of(), dispatched.get(0).getEventData().get("prefs"));
		assertEquals(3, dispatched.size());
		edgeResult(dispatched.get(2), "success");
		sync.publishProfileAttributes(null);
		assertEquals(Map.of("prefs", Map.of("lang", "en"), "timezone", "Europe/Paris"), lastProfileAttributes());
		sync = newSync();
		sync.publishProfileAttributes(null);
		assertEquals(Map.of("prefs", Map.of("lang", "en"), "timezone", "Europe/Paris"), lastProfileAttributes());
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("prefs", Map.of("lang", "en"))));
		assertEquals(3, dispatched.size());
	}

	@Test
	public void childThenParent_confirmationRemovesStaleChildBeforeAndAfterRestart() {
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("prefs", Map.of("lang", "en"))));
		edgeResult(dispatched.get(0), "success");
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("prefs", Map.of())));
		edgeResult(dispatched.get(1), "success");
		assertEquals(Map.of("prefs", Map.of()), lastProfileAttributes());

		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("prefs", Map.of("lang", "en"))));
		assertEquals(3, dispatched.size());
		edgeResult(dispatched.get(2), "failed");
		sync = newSync();
		sync.publishProfileAttributes(null);
		assertEquals(Map.of("prefs", Map.of()), lastProfileAttributes());
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("prefs", Map.of("lang", "en"))));
		assertEquals(4, dispatched.size());
		edgeResult(dispatched.get(3), "success");
		assertEquals(Map.of("prefs", Map.of("lang", "en")), lastProfileAttributes());
	}

	@Test
	public void relatedPathCleanup_preservesSiblingsAndLiteralDottedKeys() {
		sync.handleDeviceAttributeRuleResponse(
			consequence(
				Map.of("prefs", Map.of("settings", Map.of("lang", "en"), "theme", "dark"), "prefs.settings", "literal")
			)
		);
		edgeResult(dispatched.get(0), "success");
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("prefs", Map.of("settings", "disabled"))));
		edgeResult(dispatched.get(1), "success");
		assertEquals(
			Map.of("prefs", Map.of("settings", "disabled", "theme", "dark"), "prefs.settings", "literal"),
			lastProfileAttributes()
		);
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("prefs", Map.of("settings", Map.of("lang", "en")))));
		edgeResult(dispatched.get(2), "success");
		assertEquals(
			Map.of("prefs", Map.of("settings", Map.of("lang", "en"), "theme", "dark"), "prefs.settings", "literal"),
			lastProfileAttributes()
		);
	}

	@Test
	public void restoredParent_thenChildConfirmationReplacesParent() {
		seedSynced("{\"prefs\":{}}");
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("prefs", Map.of("lang", "en"))));
		edgeResult(dispatched.get(0), "success");
		assertEquals(Map.of("prefs", Map.of("lang", "en")), lastProfileAttributes());
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("prefs", Map.of("lang", "en"))));
		assertEquals(1, dispatched.size());
	}

	@Test
	public void failedParentReplacement_preservesConfirmedChildren() {
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("prefs", Map.of("lang", "en"))));
		edgeResult(dispatched.get(0), "success");
		final Object persisted = deviceStored.get(IdentityConstants.DeviceAttributes.SYNCED_VALUES);
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("prefs", Map.of())));
		edgeResult(dispatched.get(1), "failed");
		assertEquals(persisted, deviceStored.get(IdentityConstants.DeviceAttributes.SYNCED_VALUES));
		assertEquals(Map.of("prefs", Map.of("lang", "en")), lastProfileAttributes());
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("prefs", Map.of("lang", "en"))));
		assertEquals(2, dispatched.size());
	}

	@Test
	public void persistedTimezone_isDedupedAfterRestart() {
		seedSynced("{\"timezone\":\"Asia/Tokyo\"}");
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
	public void explicitNull_isDroppedWithoutClearingConfirmedToken() {
		final Map<String, Object> data = new HashMap<>();
		data.put("tokens", new HashMap<>(Map.of("pushNotification", Map.of("token", "s1"))));
		sync.handleDeviceAttributeRuleResponse(consequence(data));
		final Map<String, Object> tokens = new HashMap<>();
		tokens.put("pushNotification", null);
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("tokens", tokens)));
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("tokens", tokens)));
		succeedAll();

		assertEquals(1, dispatched.size());
		assertEquals(Map.of("tokens", Map.of("pushNotification", Map.of("token", "s1"))), lastProfileAttributes());
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
		resetLikeExtension();
		assertTrue(deviceStored.isEmpty());
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
		resetLikeExtension();
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
				final Event event = call.getArgument(0);
				return isResultEvent(event) ? results.add(event) : dispatched.add(event);
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
	public void matchingResultWithInvalidStatus_releasesQueueAndReportsFailure() {
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Europe/Paris")));
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Europe/London")));
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "America/Denver")));
		final List<Map<String, Object>> invalidResponses = List.of(
			Map.of(),
			Map.of("status", "retry"),
			Map.of("status", true)
		);
		final List<String> timezones = List.of("Asia/Tokyo", "Europe/Paris", "Europe/London");
		for (int index = 0; index < invalidResponses.size(); index++) {
			edgeResponse(invalidResponses.get(index), dispatched.get(index));
			assertEquals(index + 2, dispatched.size());
			assertEquals(
				Map.of("status", "failed", "reason", "unknown", "attributes", Map.of("timezone", timezones.get(index))),
				results.get(index).getEventData()
			);
		}
		assertTrue(stored.isEmpty());
		assertTrue(deviceStored.isEmpty());
		assertTrue(published.isEmpty());
	}

	@Test
	public void unchainedResultMatchedByRequestEventId_releasesQueue() {
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Europe/Paris")));

		edgeResponse(
			Map.of("status", "failed", "requestEventId", dispatched.get(0).getUniqueIdentifier(), "reason", "decode"),
			null
		);

		assertEquals(2, dispatched.size());
		assertEquals(
			Map.of("status", "failed", "reason", "decode", "attributes", Map.of("timezone", "Asia/Tokyo")),
			results.get(0).getEventData()
		);
	}

	@Test
	public void unchainedSuccessMatchedByRequestEventId_commitsValues() {
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));

		edgeResponse(Map.of("status", "success", "requestEventId", dispatched.get(0).getUniqueIdentifier()), null);

		assertEquals(Map.of("timezone", "Asia/Tokyo"), syncedValues());
		assertEquals("success", results.get(0).getEventData().get("status"));
	}

	@Test
	public void nonMatchingResult_doesNotAdvanceQueueOrReport() {
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Europe/Paris")));

		edgeResponse(Map.of("status", "success", "requestEventId", "other"), null);
		edgeResponse(Map.of("status", "success"), consequence(Map.of()));

		assertEquals(1, dispatched.size());
		assertTrue(results.isEmpty());
		assertTrue(deviceStored.isEmpty());
	}

	@Test
	public void queuedConsequence_readsLatestConsent() {
		consentRegistered();
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));
		when(api.getXDMSharedState(eq(IdentityConstants.SharedState.Consent.NAME), isNull(), anyBoolean(), any()))
			.thenReturn(
				new SharedStateResult(SharedStateStatus.SET, Map.of("consents", Map.of("collect", Map.of("val", "y"))))
			);
		when(api.getSharedState(eq(IdentityConstants.SharedState.Messaging.NAME), isNull(), anyBoolean(), any()))
			.thenReturn(new SharedStateResult(SharedStateStatus.SET, Map.of("pushidentifier", "token-1")));

		edgeResult(dispatched.get(0), "success");
		assertEquals(1, dispatched.size());
		verify(api, atLeastOnce())
			.getXDMSharedState(eq(IdentityConstants.SharedState.Consent.NAME), isNull(), eq(false), any());
	}

	@Test
	public void success_dispatchesSyncResultWithOnlySentAttributes() {
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		edgeResult(dispatched.get(0), "success");
		sync.handleDeviceAttributeRuleResponse(
			consequence(Map.of("timezone", "Asia/Tokyo", "tokens", Map.of("pushNotification", "token-1")))
		);
		final Event edgeRequest = dispatched.get(1);
		assertFalse(edgeRequest.getEventData().containsKey("timezone"));
		final Event response = new Event.Builder(
			"Device Attributes Response",
			IdentityConstants.DeviceAttributes.BYPASS_TYPE,
			EventSource.RESPONSE_CONTENT
		)
			.setEventData(Map.of("status", "success"))
			.chainToParentEvent(edgeRequest)
			.build();
		sync.handleDeviceAttributeResult(response);

		assertEquals(2, dispatched.size());
		assertEquals(2, results.size());
		final Event result = results.get(1);
		assertEquals("Device Attributes Sync Result", result.getName());
		assertEquals(EventType.EDGE_IDENTITY, result.getType());
		assertEquals(EventSource.RESPONSE_CONTENT, result.getSource());
		assertEquals(response.getUniqueIdentifier(), result.getParentID());
		assertEquals(
			Map.of(
				"status",
				"success",
				"reason",
				"",
				"attributes",
				Map.of("tokens", Map.of("pushNotification", "token-1"))
			),
			result.getEventData()
		);
	}

	@Test
	public void failedAndDropped_dispatchFailedSyncResultWithReason() {
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Europe/Paris")));
		edgeResponse(Map.of("status", "failed", "reason", "network", "httpStatus", 503), dispatched.get(0));
		edgeResult(dispatched.get(1), "dropped");

		assertEquals(2, results.size());
		assertEquals("Device Attributes Sync Result", results.get(0).getName());
		assertEquals(EventType.EDGE_IDENTITY, results.get(0).getType());
		assertEquals(EventSource.RESPONSE_CONTENT, results.get(0).getSource());
		assertEquals(
			Map.of("status", "failed", "reason", "network", "attributes", Map.of("timezone", "Asia/Tokyo")),
			results.get(0).getEventData()
		);
		assertEquals(
			Map.of("status", "failed", "reason", "unknown", "attributes", Map.of("timezone", "Europe/Paris")),
			results.get(1).getEventData()
		);
	}

	@Test
	public void failure_nonStringReasonIsReportedAsUnknown() {
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		edgeResponse(Map.of("status", "failed", "reason", 503), dispatched.get(0));

		assertEquals(
			Map.of("status", "failed", "reason", "unknown", "attributes", Map.of("timezone", "Asia/Tokyo")),
			results.get(0).getEventData()
		);
	}

	@Test
	public void profileAttributes_arePublishedOnlyInStandardSharedState() {
		stored.put("timeZone", "Asia/Tokyo");
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));
		edgeResult(dispatched.get(0), "success");

		verify(api, never()).createXDMSharedState(any(), any());
		assertEquals(
			Map.of(
				"profileAttributes",
				Map.of("tokens", Map.of("pushNotification", "token-1"), "timezone", "Asia/Tokyo")
			),
			published.get(0)
		);
	}

	@Test
	public void reorderedArray_isNotSent() {
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("tags", List.of("a", "b", "c"))));
		edgeResult(dispatched.get(0), "success");

		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("tags", List.of("c", "a", "b"))));
		assertEquals(1, dispatched.size());

		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("tags", List.of("a", "b"))));
		assertEquals(2, dispatched.size());
	}

	@Test
	public void commit_replacesSubtreeWithNonMapOrEmptyMapAndMergesMaps() {
		seedSynced("{\"prefs\":{\"lang\":\"en\",\"theme\":\"dark\"},\"other\":1}");
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("prefs", Map.of("lang", "fr"))));
		edgeResult(dispatched.get(0), "success");
		assertEquals(Map.of("prefs", Map.of("lang", "fr", "theme", "dark"), "other", 1), syncedValues());

		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("prefs", "off")));
		edgeResult(dispatched.get(1), "success");
		assertEquals(Map.of("prefs", "off", "other", 1), syncedValues());

		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("other", Map.of())));
		edgeResult(dispatched.get(2), "success");
		assertEquals(Map.of("prefs", "off", "other", Map.of()), syncedValues());
	}

	@Test
	public void nestedPushObject_deduplicatesChangedLeavesIndependently() {
		sync.handleDeviceAttributeRuleResponse(
			consequence(Map.of("tokens", Map.of("pushNotification", Map.of("token", "t1", "metadata", "old"))))
		);
		edgeResult(dispatched.get(0), "success");
		dispatched.clear();
		sync.handleDeviceAttributeRuleResponse(
			consequence(Map.of("tokens", Map.of("pushNotification", Map.of("token", "t1", "metadata", "new"))))
		);
		assertEquals(
			Map.of("pushNotification", Map.of("metadata", "new")),
			dispatched.get(0).getEventData().get("tokens")
		);
	}

	@Test
	public void invalidLeaves_areDroppedWithoutDiscardingValidSiblings() {
		final Map<String, Object> attributes = new HashMap<>();
		attributes.put("timezone", "Asia/Tokyo");
		attributes.put("nullValue", null);
		attributes.put("invalidValue", Double.NaN);
		sync.handleDeviceAttributeRuleResponse(consequence(attributes));
		assertEquals(
			Map.of("timezone", "Asia/Tokyo", "app", Map.of("id", "com.example", "platform", "fcm")),
			dispatched.get(0).getEventData()
		);
	}

	@Test
	public void queuedConsequence_readsLatestDedupConfiguration() {
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		when(api.getSharedState(eq(IdentityConstants.SharedState.Configuration.NAME), isNull(), anyBoolean(), any()))
			.thenReturn(
				new SharedStateResult(
					SharedStateStatus.SET,
					Map.of("edgeidentity.optimizeOperationalProfileDataSync", false)
				)
			);
		edgeResult(dispatched.get(0), "success");
		assertEquals(2, dispatched.size());
		assertEquals("Asia/Tokyo", dispatched.get(1).getEventData().get("timezone"));
	}

	@Test
	public void queuedConsequence_doesNotTrustOwnerAfterLatestConsentIsDenied() {
		collectConsent("y");
		messagingPushToken("token-1");
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("custom", "first")));
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));
		when(api.getXDMSharedState(eq(IdentityConstants.SharedState.Consent.NAME), isNull(), anyBoolean(), any()))
			.thenReturn(
				new SharedStateResult(SharedStateStatus.SET, Map.of("consents", Map.of("collect", Map.of("val", "n"))))
			);
		edgeResult(dispatched.get(0), "success");
		assertEquals(2, dispatched.size());
		assertEquals(Map.of("pushNotification", "token-1"), dispatched.get(1).getEventData().get("tokens"));
	}

	@Test
	public void queuedConsequence_readsLatestMessagingAndTimezoneOwners() {
		collectConsent("y");
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("custom", "first")));
		sync.handleDeviceAttributeRuleResponse(
			consequence(Map.of("timezone", "Europe/London", "tokens", Map.of("pushNotification", "token-1")))
		);
		stored.put("timeZone", "Europe/London");
		when(api.getSharedState(eq(IdentityConstants.SharedState.Messaging.NAME), isNull(), anyBoolean(), any()))
			.thenReturn(new SharedStateResult(SharedStateStatus.SET, Map.of("pushidentifier", "token-1")));
		edgeResult(dispatched.get(0), "success");
		assertEquals(1, dispatched.size());
		assertEquals(1, results.size());
		assertEquals(
			Map.of("custom", "first", "timezone", "Europe/London", "tokens", Map.of("pushNotification", "token-1")),
			syncedValues()
		);
	}

	@Test
	public void failureDropAndUnrelatedSuccess_doNotConfirmTokens() {
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));
		edgeResult(consequence(Map.of()), "success");
		edgeResult(dispatched.get(0), "failed");
		sync.handleDeviceAttributeRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));
		edgeResult(dispatched.get(1), "dropped");
		assertEquals(2, results.size());
		for (final Event result : results) assertEquals("failed", result.getEventData().get("status"));
		assertTrue(deviceStored.isEmpty());
		assertTrue(published.isEmpty());
	}

	@Test
	public void nullTimezone_isDroppedWithoutClearingOwner() {
		stored.put("timeZone", "Asia/Tokyo");
		final Map<String, Object> clear = new HashMap<>();
		clear.put("timezone", null);
		sync.handleDeviceAttributeRuleResponse(consequence(clear));
		sync.handleDeviceAttributeRuleResponse(consequence(clear));
		assertEquals("Asia/Tokyo", stored.get("timeZone"));
		assertTrue(dispatched.isEmpty());
		assertTrue(deviceStored.isEmpty());
	}
}
