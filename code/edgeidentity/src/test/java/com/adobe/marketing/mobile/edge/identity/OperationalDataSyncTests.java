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

public class OperationalDataSyncTests {

	private ExtensionApi api;
	private Map<String, String> stored;
	private NamedCollection collection;
	private List<Event> dispatched;
	private List<Event> results;
	private List<Event> confirmed;
	private Map<String, String> deviceStored;
	private NamedCollection deviceCollection;
	private List<Map<String, Object>> published;
	private OperationalDataSync sync;
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
		confirmed = new ArrayList<>();
		deviceStored = new HashMap<>();
		deviceCollection = mock(NamedCollection.class);
		when(deviceCollection.getString(anyString(), any()))
			.thenAnswer(call -> deviceStored.get(call.<String>getArgument(0)));
		doAnswer(call -> deviceStored.put(call.getArgument(0), call.getArgument(1)))
			.when(deviceCollection)
			.setString(anyString(), anyString());
		doAnswer(call -> deviceStored.remove(call.<String>getArgument(0))).when(deviceCollection).remove(anyString());
		published = new ArrayList<>();
		doAnswer(call -> record(call.getArgument(0))).when(api).dispatch(any());
		doAnswer(call -> published.add(call.getArgument(0))).when(api).createSharedState(anyMap(), any());
		sync = newSync();
	}

	private OperationalDataSync newSync() {
		return new OperationalDataSync(
			api,
			new ProfileAttributeStore(collection),
			new ProfileAttributeStore(deviceCollection)
		);
	}

	@After
	public void teardown() {
		mobileCore.close();
	}

	private boolean record(final Event event) {
		if (EventType.MESSAGING.equals(event.getType())) return confirmed.add(event);
		return isResultEvent(event) ? results.add(event) : dispatched.add(event);
	}

	private static boolean isResultEvent(final Event event) {
		return (
			EventType.EDGE_IDENTITY.equals(event.getType()) && EventSource.RESPONSE_CONTENT.equals(event.getSource())
		);
	}

	private void seedSynced(final Map<String, Object> attributes) {
		new OperationalDataDeduplicator(new ProfileAttributeStore(deviceCollection)).commit(attributes);
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
		return new OperationalDataDeduplicator(new ProfileAttributeStore(deviceCollection)).readSynced();
	}

	private void edgeResponse(final Map<String, Object> data, final Event parent) {
		final Event.Builder builder = new Event.Builder(
			"Device Attributes Response",
			IdentityConstants.OperationalData.EDGE_DEVICE_ATTRIBUTES_TYPE,
			EventSource.RESPONSE_CONTENT
		)
			.setEventData(data);
		if (parent != null) builder.chainToParentEvent(parent);
		sync.handleOperationalDataResult(builder.build());
	}

	private static Event request(final Map<String, Object> data) {
		return new Event.Builder(
			IdentityConstants.EventNames.UPDATE_OPERATIONAL_DATA,
			EventType.EDGE_IDENTITY,
			IdentityConstants.OperationalData.REQUEST_SOURCE
		)
			.setEventData(data)
			.build();
	}

	private static Event consequence(final Map<String, Object> data) {
		return new Event.Builder(
			"Forward operational data",
			EventType.EDGE_IDENTITY,
			IdentityConstants.OperationalData.RULE_RESPONSE_SOURCE
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
			if (IdentityConstants.OperationalData.EDGE_DEVICE_ATTRIBUTES_TYPE.equals(event.getType())) edgeResult(
				event,
				"success"
			);
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> lastProfileAttributes() {
		return (Map<String, Object>) published.get(published.size() - 1).get("profileAttributes");
	}

	@Test
	public void handleOperationalData_dispatchesOperationalDataChainedToApiEvent() {
		final Event request = request(Map.of("timezone", "Asia/Tokyo"));
		sync.handleOperationalData(request);

		assertEquals(1, dispatched.size());
		final Event operational = dispatched.get(0);
		assertEquals(IdentityConstants.OperationalData.OPERATIONAL_DATA_TYPE, operational.getType());
		assertEquals(EventSource.REQUEST_CONTENT, operational.getSource());
		assertEquals(request.getUniqueIdentifier(), operational.getParentID());
		assertNull(operational.getResponseID());
		assertEquals("Asia/Tokyo", operational.getEventData().get("timezone"));
		assertFalse(operational.getEventData().containsKey("app"));
	}

	@Test
	public void ruleResponse_pushOnlyAddsSdkAppAfterFilteringAndOverridesCallerApp() {
		sync.handleOperationalDataRuleResponse(
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
		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));
		assertTrue(dispatched.isEmpty());
		assertTrue(stored.isEmpty());
		assertTrue(published.isEmpty());
	}

	@Test
	public void ruleResponse_emptyPackageDoesNotSendOrPersist() {
		when(application.getPackageName()).thenReturn("");
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		assertTrue(dispatched.isEmpty());
		assertTrue(stored.isEmpty());
		assertTrue(published.isEmpty());
	}

	@Test
	public void handleOperationalData_ignoresEmptyOrInvalidData() {
		sync.handleOperationalData(request(new HashMap<>()));
		sync.handleOperationalData(request(null));
		assertTrue(dispatched.isEmpty());
	}

	@Test
	public void ruleResponse_sendsForwardedValuesToEdgeAndPublishesState() {
		final Map<String, Object> app = Map.of("id", "com.example", "platform", "fcm");
		final Event forward = consequence(Map.of("timezone", "Asia/Tokyo", "app", app));
		sync.handleOperationalDataRuleResponse(forward);

		assertEquals(1, dispatched.size());
		final Event edge = dispatched.get(0);
		assertEquals(IdentityConstants.OperationalData.EDGE_DEVICE_ATTRIBUTES_TYPE, edge.getType());
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
		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));
		assertEquals(Map.of("id", "com.example", "platform", "fcm"), dispatched.get(0).getEventData().get("app"));
		edgeResult(dispatched.get(0), "success");
		assertEquals(Map.of("tokens", Map.of("pushNotification", "token-1")), lastProfileAttributes());
		assertEquals(Map.of("tokens", Map.of("pushNotification", "token-1")), syncedValues());
	}

	@Test
	public void persistedPushToken_isDedupedAfterRestart() {
		final Map<String, Object> token = Map.of("tokens", Map.of("pushNotification", "token-1"));
		sync.handleOperationalDataRuleResponse(consequence(token));
		edgeResult(dispatched.get(0), "success");

		sync = newSync();
		sync.handleOperationalDataRuleResponse(consequence(token));

		assertEquals(1, dispatched.size());
	}

	@Test
	public void nullPushToken_isSentButNotStoredBeforeAndAfterRestart() {
		final Map<String, Object> tokens = new HashMap<>();
		tokens.put("pushNotification", null);
		final Map<String, Object> attributes = Map.of("tokens", tokens);
		sync.handleOperationalDataRuleResponse(consequence(attributes));
		assertEquals(tokens, dispatched.get(0).getEventData().get("tokens"));
		edgeResult(dispatched.get(0), "success");
		assertTrue(deviceStored.isEmpty());
		sync = newSync();
		sync.handleOperationalDataRuleResponse(consequence(attributes));

		assertEquals(2, dispatched.size());
		assertEquals(tokens, dispatched.get(1).getEventData().get("tokens"));
	}

	@Test
	public void consentYes_ownerMatchIsNotSentAndIsCopiedToStore() {
		collectConsent("y");
		messagingPushToken("token-1");

		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));

		assertTrue(dispatched.isEmpty());
		assertEquals(1, results.size());
		assertEquals("unchanged", results.get(0).getEventData().get("reason"));
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

		sync.handleOperationalDataRuleResponse(rule);

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

		sync.handleOperationalDataRuleResponse(rule);

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

		sync.handleOperationalDataRuleResponse(rule);

		assertTrue(dispatched.isEmpty());
		final Map<String, Object> expected = Map.of("tokens", Map.of("pushNotification", "token-1"));
		verify(api).createSharedState(eq(Collections.singletonMap("profileAttributes", expected)), eq(rule));
		assertEquals(expected, syncedValues());
	}

	@Test
	public void noOwnerMatch_doesNotPublish() {
		collectConsent("y");
		messagingPushToken("token-old");
		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-new"))));
		assertEquals(1, dispatched.size());
		assertTrue(published.isEmpty());

		edgeResult(dispatched.get(0), "success");
		assertEquals(1, published.size());
		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-new"))));
		assertEquals(1, published.size());
	}

	@Test
	public void ownerMatchCopiedToStore_isDedupedWhenConsentIsLaterDenied() {
		stored.put("timeZone", "Asia/Tokyo");
		collectConsent("y");
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		assertTrue(dispatched.isEmpty());

		collectConsent("n");
		sync = newSync();
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		assertTrue(dispatched.isEmpty());
	}

	@Test
	public void consentYes_ownerMatchCopiesOnlyMatchingLeaf() {
		collectConsent("y");
		messagingPushToken("token-1");

		sync.handleOperationalDataRuleResponse(
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

		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-new"))));

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

		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));

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

		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));

		assertTrue(dispatched.isEmpty());
		assertEquals(Map.of("tokens", Map.of("pushNotification", "token-1")), syncedValues());
		verify(api, never())
			.getXDMSharedState(eq(IdentityConstants.SharedState.Consent.NAME), any(), anyBoolean(), any());
	}

	@Test
	public void hubSharedStateUnavailable_doesNotTreatConsentAsYes() {
		messagingPushToken("token-1");

		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));

		assertEquals(1, dispatched.size());
		assertTrue(deviceStored.isEmpty());
	}

	@Test
	public void consentNo_sendsOperationalData() {
		collectConsent("n");
		stored.put("timeZone", "Asia/Tokyo");

		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));

		assertEquals(1, dispatched.size());
		assertEquals(IdentityConstants.OperationalData.EDGE_DEVICE_ATTRIBUTES_TYPE, dispatched.get(0).getType());
	}

	@Test
	public void consentNo_unchangedValueIsDedupedAgainstPersistedDeviceStore() {
		seedSynced(Map.of("tokens", Map.of("pushNotification", "token-1")));
		collectConsent("n");

		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));

		assertTrue(dispatched.isEmpty());
	}

	@Test
	public void pendingConsent_sendsOperationalData() {
		consentRegistered();
		stored.put("timeZone", "Asia/Tokyo");
		when(api.getXDMSharedState(eq(IdentityConstants.SharedState.Consent.NAME), any(), anyBoolean(), any()))
			.thenReturn(new SharedStateResult(SharedStateStatus.PENDING, null));

		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));

		assertEquals(1, dispatched.size());
		assertEquals(IdentityConstants.OperationalData.EDGE_DEVICE_ATTRIBUTES_TYPE, dispatched.get(0).getType());
	}

	@Test
	public void edgeFailure_doesNotPublishAndAllowsResend() {
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		edgeResult(dispatched.get(0), "failed");
		assertNull(stored.get("timeZone"));
		assertTrue(published.isEmpty());

		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		assertEquals(2, dispatched.size());
	}

	@Test
	public void edgeResult_forUnknownRequestIsIgnored() {
		edgeResult(
			new Event.Builder(
				"Other",
				IdentityConstants.OperationalData.EDGE_DEVICE_ATTRIBUTES_TYPE,
				EventSource.REQUEST_CONTENT
			)
				.build(),
			"success"
		);
		assertTrue(published.isEmpty());
	}

	@Test
	public void ruleResponse_appOnlyConsequenceIsNotSent() {
		sync.handleOperationalDataRuleResponse(consequence(Map.of("app", Map.of("id", "com.example"))));
		assertTrue(dispatched.isEmpty());
	}

	@Test
	public void multipleConsequences_areSentInOrderAndLaterDuplicatesAreDropped() {
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleOperationalDataRuleResponse(
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
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Europe/Paris")));
		assertEquals(1, dispatched.size());
		succeedAll();

		assertEquals(2, dispatched.size());
		assertEquals("Europe/Paris", dispatched.get(1).getEventData().get("timezone"));
		assertEquals("Europe/Paris", stored.get("timeZone"));
	}

	@Test
	public void parentThenChild_confirmationReplacesParentAndDrainsQueue() {
		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", Map.of())));
		edgeResult(dispatched.get(0), "success");
		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Europe/Paris")));
		edgeResult(dispatched.get(1), "success");

		assertEquals(Map.of("tokens", Map.of("pushNotification", "token-1")), lastProfileAttributes());
		assertEquals(Map.of(), dispatched.get(0).getEventData().get("tokens"));
		assertEquals(3, dispatched.size());
		edgeResult(dispatched.get(2), "success");
		sync.publishProfileAttributes(null);
		final Map<String, Object> expected = Map.of(
			"tokens",
			Map.of("pushNotification", "token-1"),
			"timezone",
			"Europe/Paris"
		);
		assertEquals(expected, lastProfileAttributes());
		sync = newSync();
		sync.publishProfileAttributes(null);
		assertEquals(expected, lastProfileAttributes());
		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));
		assertEquals(3, dispatched.size());
	}

	@Test
	public void childThenEmptyParent_keepsConfirmedChildBeforeAndAfterRestart() {
		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));
		edgeResult(dispatched.get(0), "success");
		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", Map.of())));
		assertEquals(Map.of(), dispatched.get(1).getEventData().get("tokens"));
		edgeResult(dispatched.get(1), "success");
		assertEquals(Map.of("tokens", Map.of("pushNotification", "token-1")), lastProfileAttributes());

		sync = newSync();
		sync.publishProfileAttributes(null);
		assertEquals(Map.of("tokens", Map.of("pushNotification", "token-1")), lastProfileAttributes());
		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", Map.of())));
		assertEquals(3, dispatched.size());
	}

	@Test
	public void literalDottedKey_isNotTreatedAsKnownPath() {
		final Map<String, Object> attributes = Map.of(
			"tokens",
			Map.of("pushNotification", "token-1"),
			"tokens.pushNotification",
			"literal"
		);
		sync.handleOperationalDataRuleResponse(consequence(attributes));
		edgeResult(dispatched.get(0), "success");
		assertEquals(Map.of("tokens", Map.of("pushNotification", "token-1")), lastProfileAttributes());

		sync.handleOperationalDataRuleResponse(consequence(attributes));
		assertEquals(2, dispatched.size());
		assertEquals(
			Map.of("tokens.pushNotification", "literal", "app", Map.of("id", "com.example", "platform", "fcm")),
			dispatched.get(1).getEventData()
		);
	}

	@Test
	public void emptyParent_isNotStoredAndChildConfirmationIsStored() {
		seedSynced(Map.of("tokens", Map.of()));
		assertTrue(deviceStored.isEmpty());
		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));
		edgeResult(dispatched.get(0), "success");
		assertEquals(Map.of("tokens", Map.of("pushNotification", "token-1")), lastProfileAttributes());
		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));
		assertEquals(1, dispatched.size());
	}

	@Test
	public void failedParentReplacement_preservesConfirmedChildren() {
		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));
		edgeResult(dispatched.get(0), "success");
		final Map<String, String> persisted = new HashMap<>(deviceStored);
		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", Map.of())));
		edgeResult(dispatched.get(1), "failed");
		assertEquals(persisted, deviceStored);
		assertEquals(Map.of("tokens", Map.of("pushNotification", "token-1")), lastProfileAttributes());
		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));
		assertEquals(2, dispatched.size());
	}

	@Test
	public void persistedTimezone_isDedupedAfterRestart() {
		seedSynced(Map.of("timezone", "Asia/Tokyo"));
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		assertTrue(dispatched.isEmpty());
	}

	@Test
	public void dedupEnabledByConfiguration_skipsConfirmedDuplicates() {
		dedupConfig(true);
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		edgeResult(dispatched.get(0), "success");
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		assertEquals(1, dispatched.size());
	}

	@Test
	public void dedupDisabledByConfiguration_sendsDuplicates() {
		dedupConfig(false);
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		assertEquals(1, dispatched.size());
		edgeResult(dispatched.get(0), "success");
		assertEquals(2, dispatched.size());
	}

	@Test
	public void dedupDisabledByConfiguration_forwardsUnexpectedAndNullValuesOnEveryCall() {
		dedupConfig(false);
		collectConsent("y");
		messagingPushToken("token-1");
		seedSynced(Map.of("tokens", Map.of("pushNotification", "token-1")));
		final Map<String, Object> attributes = new HashMap<>();
		attributes.put("timezone", null);
		attributes.put("tokens", Map.of("pushNotification", "token-1"));
		attributes.put("custom", Double.NaN);
		sync.handleOperationalDataRuleResponse(consequence(attributes));
		sync.handleOperationalDataRuleResponse(consequence(attributes));
		succeedAll();

		attributes.put("app", Map.of("id", "com.example", "platform", "fcm"));
		assertEquals(2, dispatched.size());
		assertEquals(attributes, dispatched.get(0).getEventData());
		assertEquals(attributes, dispatched.get(1).getEventData());
		assertEquals(Map.of("pushNotification", "token-1"), deviceStored);
	}

	@Test
	public void explicitNull_isSentWithoutClearingConfirmedToken() {
		final Map<String, Object> data = new HashMap<>();
		data.put("tokens", Map.of("pushNotification", "s1"));
		sync.handleOperationalDataRuleResponse(consequence(data));
		final Map<String, Object> tokens = new HashMap<>();
		tokens.put("pushNotification", null);
		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", tokens)));
		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", tokens)));
		succeedAll();

		assertEquals(3, dispatched.size());
		assertEquals(tokens, dispatched.get(1).getEventData().get("tokens"));
		assertEquals(tokens, dispatched.get(2).getEventData().get("tokens"));
		assertEquals(Map.of("tokens", Map.of("pushNotification", "s1")), lastProfileAttributes());
	}

	@Test
	public void customNestedValues_areAlwaysSentInFull() {
		sync.handleOperationalDataRuleResponse(consequence(Map.of("custom", Map.of("a", 1, "b", 2))));
		sync.handleOperationalDataRuleResponse(consequence(Map.of("custom", Map.of("a", 1, "b", 3))));
		edgeResult(dispatched.get(0), "success");

		assertEquals(2, dispatched.size());
		assertEquals(
			Map.of("custom", Map.of("a", 1, "b", 3), "app", Map.of("id", "com.example", "platform", "fcm")),
			dispatched.get(1).getEventData()
		);
	}

	@Test
	public void reset_clearsSentValues() {
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo", "custom", Map.of("a", 1))));
		succeedAll();
		resetLikeExtension();
		assertTrue(deviceStored.isEmpty());
		assertTrue(lastProfileAttributes().isEmpty());

		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo", "custom", Map.of("a", 1))));
		assertEquals(2, dispatched.size());
	}

	@Test
	public void queuedDuplicate_isSentAfterFailure() {
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		final Event queued = consequence(Map.of("timezone", "Asia/Tokyo"));
		sync.handleOperationalDataRuleResponse(queued);
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
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Europe/Paris")));
		edgeResult(dispatched.get(0), "dropped");
		assertEquals(2, dispatched.size());
		assertTrue(published.isEmpty());
		assertEquals("Europe/Paris", dispatched.get(1).getEventData().get("timezone"));
	}

	@Test
	public void unrelatedAndDuplicateResults_doNotReleaseNextRequest() {
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Europe/Paris")));
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Europe/London")));
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
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		final Event oldRequest = dispatched.get(0);
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Europe/Paris")));
		resetLikeExtension();
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Europe/London")));
		edgeResult(oldRequest, "success");

		assertEquals(2, dispatched.size());
		assertNull(stored.get("timeZone"));
		assertTrue(lastProfileAttributes().isEmpty());
		edgeResult(dispatched.get(1), "success");
		assertEquals("Europe/London", stored.get("timeZone"));
	}

	@Test
	public void success_publishesBeforeDispatchingNextRequest() {
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Europe/Paris")));
		doAnswer(call -> {
				assertEquals("Asia/Tokyo", stored.get("timeZone"));
				assertEquals(Map.of("timezone", "Asia/Tokyo"), lastProfileAttributes());
				return record(call.getArgument(0));
			})
			.when(api)
			.dispatch(any());

		edgeResult(dispatched.get(0), "success");
		assertEquals(2, dispatched.size());
	}

	@Test
	public void queue_skipsUnchangedAndEmptyConsequencesWithoutBlockingLaterChanges() {
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleOperationalDataRuleResponse(consequence(Map.of("app", Map.of("id", "com.example"))));
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Europe/Paris")));

		edgeResult(dispatched.get(0), "success");
		assertEquals(2, dispatched.size());
		assertEquals("Europe/Paris", dispatched.get(1).getEventData().get("timezone"));
	}

	@Test
	public void matchingResultWithInvalidStatus_releasesQueueAndReportsFailure() {
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Europe/Paris")));
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Europe/London")));
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "America/Denver")));
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
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Europe/Paris")));

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
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));

		edgeResponse(Map.of("status", "success", "requestEventId", dispatched.get(0).getUniqueIdentifier()), null);

		assertEquals(Map.of("timezone", "Asia/Tokyo"), syncedValues());
		assertEquals("success", results.get(0).getEventData().get("status"));
	}

	@Test
	public void nonMatchingResult_doesNotAdvanceQueueOrReport() {
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Europe/Paris")));

		edgeResponse(Map.of("status", "success", "requestEventId", "other"), null);
		edgeResponse(Map.of("status", "success"), consequence(Map.of()));

		assertEquals(1, dispatched.size());
		assertTrue(results.isEmpty());
		assertTrue(deviceStored.isEmpty());
	}

	@Test
	public void queuedConsequence_readsLatestConsent() {
		consentRegistered();
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));
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
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		edgeResult(dispatched.get(0), "success");
		sync.handleOperationalDataRuleResponse(
			consequence(Map.of("timezone", "Asia/Tokyo", "tokens", Map.of("pushNotification", "token-1")))
		);
		final Event edgeRequest = dispatched.get(1);
		assertFalse(edgeRequest.getEventData().containsKey("timezone"));
		final Event response = new Event.Builder(
			"Device Attributes Response",
			IdentityConstants.OperationalData.EDGE_DEVICE_ATTRIBUTES_TYPE,
			EventSource.RESPONSE_CONTENT
		)
			.setEventData(Map.of("status", "success"))
			.chainToParentEvent(edgeRequest)
			.build();
		sync.handleOperationalDataResult(response);

		assertEquals(2, dispatched.size());
		assertEquals(2, results.size());
		final Event result = results.get(1);
		assertEquals("Operational Data Sync Result", result.getName());
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
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Europe/Paris")));
		edgeResponse(Map.of("status", "failed", "reason", "network", "httpStatus", 503), dispatched.get(0));
		edgeResult(dispatched.get(1), "dropped");

		assertEquals(2, results.size());
		assertEquals("Operational Data Sync Result", results.get(0).getName());
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
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		edgeResponse(Map.of("status", "failed", "reason", 503), dispatched.get(0));

		assertEquals(
			Map.of("status", "failed", "reason", "unknown", "attributes", Map.of("timezone", "Asia/Tokyo")),
			results.get(0).getEventData()
		);
	}

	@Test
	public void profileAttributes_arePublishedOnlyInStandardSharedState() {
		stored.put("timeZone", "Asia/Tokyo");
		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));
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
	public void pushTokenArrays_areAlwaysSentAndNeverStored() {
		sync.handleOperationalDataRuleResponse(
			consequence(Map.of("tokens", Map.of("pushNotification", List.of("a", "b", "c"))))
		);
		edgeResult(dispatched.get(0), "success");

		sync.handleOperationalDataRuleResponse(
			consequence(Map.of("tokens", Map.of("pushNotification", List.of("c", "a", "b"))))
		);
		assertEquals(2, dispatched.size());
		assertTrue(deviceStored.isEmpty());

		sync.handleOperationalDataRuleResponse(
			consequence(Map.of("tokens", Map.of("pushNotification", List.of("a", "b"))))
		);
		edgeResult(dispatched.get(1), "success");
		assertEquals(3, dispatched.size());
	}

	@Test
	public void commit_ignoresUnexpectedTypesAndKeepsConfirmedStrings() {
		seedSynced(Map.of("tokens", Map.of("pushNotification", "token-1"), "timezone", "Asia/Tokyo"));
		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-2"))));
		edgeResult(dispatched.get(0), "success");
		assertEquals(Map.of("tokens", Map.of("pushNotification", "token-2"), "timezone", "Asia/Tokyo"), syncedValues());

		sync.handleOperationalDataRuleResponse(
			consequence(Map.of("tokens", Map.of("pushNotification", Map.of("token", "token-3"))))
		);
		edgeResult(dispatched.get(1), "success");
		assertEquals(Map.of("tokens", Map.of("pushNotification", "token-2"), "timezone", "Asia/Tokyo"), syncedValues());

		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", Map.of()))));
		edgeResult(dispatched.get(2), "success");
		assertEquals(Map.of("tokens", Map.of("pushNotification", "token-2"), "timezone", "Asia/Tokyo"), syncedValues());
	}

	@Test
	public void nestedPushObject_isAlwaysSentAndNeverStored() {
		sync.handleOperationalDataRuleResponse(
			consequence(Map.of("tokens", Map.of("pushNotification", Map.of("token", "t1", "metadata", "old"))))
		);
		edgeResult(dispatched.get(0), "success");
		dispatched.clear();
		sync.handleOperationalDataRuleResponse(
			consequence(Map.of("tokens", Map.of("pushNotification", Map.of("metadata", "old", "token", "t1"))))
		);
		assertEquals(1, dispatched.size());
		assertTrue(deviceStored.isEmpty());
		sync.handleOperationalDataRuleResponse(
			consequence(Map.of("tokens", Map.of("pushNotification", Map.of("token", "t1", "metadata", "new"))))
		);
		edgeResult(dispatched.get(0), "success");
		assertEquals(2, dispatched.size());
		assertEquals(
			Map.of("pushNotification", Map.of("token", "t1", "metadata", "new")),
			dispatched.get(1).getEventData().get("tokens")
		);
	}

	@Test
	public void invalidLeaves_areForwardedWithoutValidation() {
		final Map<String, Object> attributes = new HashMap<>();
		attributes.put("timezone", "Asia/Tokyo");
		attributes.put("nullValue", null);
		attributes.put("invalidValue", Double.NaN);
		sync.handleOperationalDataRuleResponse(consequence(attributes));
		attributes.put("app", Map.of("id", "com.example", "platform", "fcm"));
		assertEquals(attributes, dispatched.get(0).getEventData());
	}

	@Test
	public void queuedConsequence_readsLatestDedupConfiguration() {
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
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
		sync.handleOperationalDataRuleResponse(consequence(Map.of("custom", "first")));
		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));
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
		sync.handleOperationalDataRuleResponse(consequence(Map.of("custom", "first")));
		sync.handleOperationalDataRuleResponse(
			consequence(Map.of("timezone", "Europe/London", "tokens", Map.of("pushNotification", "token-1")))
		);
		stored.put("timeZone", "Europe/London");
		when(api.getSharedState(eq(IdentityConstants.SharedState.Messaging.NAME), isNull(), anyBoolean(), any()))
			.thenReturn(new SharedStateResult(SharedStateStatus.SET, Map.of("pushidentifier", "token-1")));
		edgeResult(dispatched.get(0), "success");
		assertEquals(1, dispatched.size());
		assertEquals(2, results.size());
		assertEquals("unchanged", results.get(1).getEventData().get("reason"));
		assertEquals(
			Map.of("timezone", "Europe/London", "tokens", Map.of("pushNotification", "token-1")),
			syncedValues()
		);
	}

	@Test
	public void failureDropAndUnrelatedSuccess_doNotConfirmTokens() {
		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));
		edgeResult(consequence(Map.of()), "success");
		edgeResult(dispatched.get(0), "failed");
		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));
		edgeResult(dispatched.get(1), "dropped");
		assertEquals(2, results.size());
		for (final Event result : results) assertEquals("failed", result.getEventData().get("status"));
		assertTrue(deviceStored.isEmpty());
		assertTrue(published.isEmpty());
	}

	@Test
	public void nullTimezone_isSentWithoutClearingOwner() {
		stored.put("timeZone", "Asia/Tokyo");
		final Map<String, Object> clear = new HashMap<>();
		clear.put("timezone", null);
		sync.handleOperationalDataRuleResponse(consequence(clear));
		sync.handleOperationalDataRuleResponse(consequence(clear));
		assertEquals("Asia/Tokyo", stored.get("timeZone"));
		assertEquals(1, dispatched.size());
		assertTrue(dispatched.get(0).getEventData().containsKey("timezone"));
		assertNull(dispatched.get(0).getEventData().get("timezone"));
		edgeResult(dispatched.get(0), "success");
		assertEquals(2, dispatched.size());
		assertTrue(deviceStored.isEmpty());
	}

	private void assertSyncResult(
		final Event result,
		final String status,
		final String reason,
		final Map<String, Object> attributes,
		final Event parent
	) {
		assertEquals("Operational Data Sync Result", result.getName());
		assertEquals(EventType.EDGE_IDENTITY, result.getType());
		assertEquals(EventSource.RESPONSE_CONTENT, result.getSource());
		assertEquals(Map.of("status", status, "reason", reason, "attributes", attributes), result.getEventData());
		assertEquals(parent.getUniqueIdentifier(), result.getParentID());
	}

	@Test
	public void allValuesSynced_reportsUnchangedWithoutEdgeRequest() {
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		succeedAll();
		results.clear();
		final Event rule = consequence(
			Map.of("timezone", "Asia/Tokyo", "app", Map.of("id", "caller.app", "platform", "apns"))
		);

		sync.handleOperationalDataRuleResponse(rule);

		assertEquals(1, dispatched.size());
		assertEquals(1, results.size());
		assertSyncResult(results.get(0), "success", "unchanged", Map.of("timezone", "Asia/Tokyo"), rule);
	}

	@Test
	public void allValuesOwnerMatched_reportsUnchangedAndPublishesOnce() {
		collectConsent("y");
		messagingPushToken("token-1");
		final Event rule = consequence(Map.of("tokens", Map.of("pushNotification", "token-1")));

		sync.handleOperationalDataRuleResponse(rule);

		assertTrue(dispatched.isEmpty());
		assertEquals(1, published.size());
		assertEquals(1, results.size());
		assertSyncResult(
			results.get(0),
			"success",
			"unchanged",
			Map.of("tokens", Map.of("pushNotification", "token-1")),
			rule
		);
	}

	@Test
	public void partiallySyncedValues_areSentWithoutUnchangedResult() {
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		succeedAll();
		results.clear();

		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo", "custom", "x")));

		assertEquals(2, dispatched.size());
		assertTrue(results.isEmpty());
		edgeResult(dispatched.get(1), "success");
		assertEquals(1, results.size());
		assertEquals(
			Map.of("status", "success", "reason", "", "attributes", Map.of("custom", "x")),
			results.get(0).getEventData()
		);
	}

	@Test
	public void reset_reportsActiveAndQueuedRequestsAsFailedInOrder() {
		final Event first = consequence(Map.of("timezone", "Asia/Tokyo"));
		final Event second = consequence(Map.of("custom", "a", "app", Map.of("id", "caller.app", "platform", "apns")));
		final Event third = consequence(Map.of("custom", "b"));
		sync.handleOperationalDataRuleResponse(first);
		sync.handleOperationalDataRuleResponse(second);
		sync.handleOperationalDataRuleResponse(third);
		final Event active = dispatched.get(0);

		resetLikeExtension();

		assertEquals(3, results.size());
		assertSyncResult(results.get(0), "failed", "identityReset", Map.of("timezone", "Asia/Tokyo"), active);
		assertSyncResult(results.get(1), "failed", "identityReset", Map.of("custom", "a"), second);
		assertSyncResult(results.get(2), "failed", "identityReset", Map.of("custom", "b"), third);

		edgeResult(active, "success");
		assertEquals(3, results.size());
		assertEquals(1, dispatched.size());
		assertTrue(deviceStored.isEmpty());
	}

	@Test
	public void reset_withNothingPending_reportsNoResults() {
		resetLikeExtension();
		assertTrue(results.isEmpty());

		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		succeedAll();
		results.clear();
		resetLikeExtension();
		assertTrue(results.isEmpty());
	}

	@Test
	public void missingAppIdentifier_reportsChangedValuesAndContinuesQueue() {
		when(application.getPackageName()).thenReturn("com.example", "", "com.example");
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		final Event noAppId = consequence(
			Map.of("timezone", "Asia/Tokyo", "custom", "x", "app", Map.of("id", "caller.app", "platform", "apns"))
		);
		final Event next = consequence(Map.of("custom", "y"));
		sync.handleOperationalDataRuleResponse(noAppId);
		sync.handleOperationalDataRuleResponse(next);

		edgeResult(dispatched.get(0), "success");

		assertEquals(2, results.size());
		assertSyncResult(results.get(1), "failed", "missingAppIdentifier", Map.of("custom", "x"), noAppId);
		assertEquals(2, dispatched.size());
		assertEquals(next.getUniqueIdentifier(), dispatched.get(1).getParentID());
	}

	private Event edgeResultEvent(final Event edgeRequest, final String status) {
		final Event response = new Event.Builder(
			"Device Attributes Response",
			IdentityConstants.OperationalData.EDGE_DEVICE_ATTRIBUTES_TYPE,
			EventSource.RESPONSE_CONTENT
		)
			.setEventData(Map.of("status", status))
			.chainToParentEvent(edgeRequest)
			.build();
		sync.handleOperationalDataResult(response);
		return response;
	}

	@Test
	public void success_withTokens_dispatchesConfirmedTokensAfterPublishAndBeforeResult() {
		final List<String> order = new ArrayList<>();
		doAnswer(call -> {
				final Event event = call.getArgument(0);
				order.add(event.getName());
				return record(event);
			})
			.when(api)
			.dispatch(any());
		doAnswer(call -> {
				order.add("publish");
				return published.add(call.getArgument(0));
			})
			.when(api)
			.createSharedState(anyMap(), any());
		sync.handleOperationalDataRuleResponse(
			consequence(Map.of("timezone", "Asia/Tokyo", "tokens", Map.of("pushNotification", "token-1")))
		);
		order.clear();

		final Event response = edgeResultEvent(dispatched.get(0), "success");

		assertEquals(1, confirmed.size());
		final Event event = confirmed.get(0);
		assertEquals("Operational data confirmed", event.getName());
		assertEquals(EventType.MESSAGING, event.getType());
		assertEquals(EventSource.RESPONSE_CONTENT, event.getSource());
		assertEquals(Map.of("tokens", Map.of("pushNotification", "token-1")), event.getEventData());
		assertEquals(response.getUniqueIdentifier(), event.getParentID());
		assertEquals(List.of("publish", "Operational data confirmed", "Operational Data Sync Result"), order);
	}

	@Test
	public void success_withoutTokens_doesNotDispatchConfirmedTokens() {
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		edgeResult(dispatched.get(0), "success");
		assertEquals(1, results.size());
		assertTrue(confirmed.isEmpty());
	}

	@Test
	public void failure_withTokens_doesNotDispatchConfirmedTokens() {
		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));
		edgeResult(dispatched.get(0), "failed");
		assertEquals(1, results.size());
		assertTrue(confirmed.isEmpty());
	}

	@Test
	public void unchanged_ownerMatchedToken_doesNotDispatchConfirmedTokens() {
		collectConsent("y");
		messagingPushToken("token-1");
		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));
		assertEquals(1, published.size());
		assertEquals(1, results.size());
		assertEquals("unchanged", results.get(0).getEventData().get("reason"));
		assertTrue(confirmed.isEmpty());
	}

	@Test
	public void reset_withPendingTokenRequest_doesNotDispatchConfirmedTokens() {
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleOperationalDataRuleResponse(consequence(Map.of("tokens", Map.of("pushNotification", "token-1"))));
		resetLikeExtension();
		assertEquals(2, results.size());
		assertTrue(confirmed.isEmpty());
	}

	@Test
	public void partialDedup_confirmsOnlyTheSentTokens() {
		sync.handleOperationalDataRuleResponse(
			consequence(Map.of("tokens", Map.of("pushNotification", "token-1", "other", "o-1")))
		);
		edgeResult(dispatched.get(0), "success");
		confirmed.clear();

		sync.handleOperationalDataRuleResponse(
			consequence(Map.of("tokens", Map.of("pushNotification", "token-1", "other", "o-2")))
		);
		assertEquals(Map.of("other", "o-2"), dispatched.get(1).getEventData().get("tokens"));
		final Event response = edgeResultEvent(dispatched.get(1), "success");

		assertEquals(1, confirmed.size());
		assertEquals(Map.of("tokens", Map.of("other", "o-2")), confirmed.get(0).getEventData());
		assertEquals(response.getUniqueIdentifier(), confirmed.get(0).getParentID());
	}

	@Test
	public void dispatchFailure_reportsInternalErrorAndSendsNextQueuedRequest() {
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		final Map<String, Object> app = Map.of("id", "com.example", "platform", "fcm");
		final Event failing = consequence(Map.of("app", app, "tokens", Map.of("pushNotification", "token-1")));
		sync.handleOperationalDataRuleResponse(failing);
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Europe/London")));
		final int[] sends = { 0 };
		doAnswer(call -> {
				final Event event = call.getArgument(0);
				if (
					IdentityConstants.OperationalData.EDGE_DEVICE_ATTRIBUTES_TYPE.equals(event.getType()) &&
					sends[0]++ == 0
				) {
					throw new IllegalStateException("dispatch failed");
				}
				return record(event);
			})
			.when(api)
			.dispatch(any());

		edgeResult(dispatched.get(0), "success");

		assertEquals(2, results.size());
		assertSyncResult(
			results.get(1),
			"failed",
			"internalError",
			Map.of("tokens", Map.of("pushNotification", "token-1")),
			failing
		);
		assertEquals(2, dispatched.size());
		assertEquals("Europe/London", dispatched.get(1).getEventData().get("timezone"));

		edgeResult(dispatched.get(1), "success");
		assertEquals(3, results.size());
		assertEquals("Europe/London", syncedValues().get("timezone"));
	}

	@Test
	public void successHandlingThrows_stillSendsNextQueuedRequest() {
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Europe/London")));
		assertEquals(1, dispatched.size());
		doThrow(new IllegalStateException("publish failed")).when(api).createSharedState(anyMap(), any());

		assertThrows(IllegalStateException.class, () -> edgeResult(dispatched.get(0), "success"));

		assertEquals(2, dispatched.size());
		assertEquals("Europe/London", dispatched.get(1).getEventData().get("timezone"));
	}

	@Test
	public void customKey_sentTwice_isSentBothTimesAndNeverStored() {
		sync.handleOperationalDataRuleResponse(consequence(Map.of("custom", "value")));
		edgeResult(dispatched.get(0), "success");
		sync.handleOperationalDataRuleResponse(consequence(Map.of("custom", "value")));
		edgeResult(dispatched.get(1), "success");

		assertEquals(2, dispatched.size());
		assertEquals("value", dispatched.get(1).getEventData().get("custom"));
		assertTrue(deviceStored.isEmpty());
		assertFalse(lastProfileAttributes().containsKey("custom"));
	}

	@Test
	public void mixedRequest_sendsOnlyCustomKeyAndStoresOnlyKnownValues() {
		seedSynced(Map.of("timezone", "Asia/Tokyo"));
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo", "custom", "value")));

		assertEquals(1, dispatched.size());
		assertEquals(
			Map.of("custom", "value", "app", Map.of("id", "com.example", "platform", "fcm")),
			dispatched.get(0).getEventData()
		);
		assertTrue(results.isEmpty());

		edgeResult(dispatched.get(0), "success");
		assertEquals(Map.of("timezone", "Asia/Tokyo"), syncedValues());
	}

	@Test
	public void unknownTokenKey_withSyncedPushToken_sendsOnlyUnknownKey() {
		seedSynced(Map.of("tokens", Map.of("pushNotification", "token-1")));
		sync.handleOperationalDataRuleResponse(
			consequence(Map.of("tokens", Map.of("pushNotification", "token-1", "foo", "bar")))
		);

		assertEquals(1, dispatched.size());
		assertEquals(Map.of("foo", "bar"), dispatched.get(0).getEventData().get("tokens"));
	}

	@Test
	public void storedUnknownKeys_areNotPublished() {
		deviceStored.put("timezone", "Asia/Tokyo");
		deviceStored.put("custom", "value");
		deviceStored.put("foo", "bar");
		sync.publishStoredProfileAttributes();
		assertEquals(Map.of("timezone", "Asia/Tokyo"), lastProfileAttributes());
		assertEquals(Map.of("timezone", "Asia/Tokyo"), syncedValues());
	}

	@Test
	public void storedProfileAttributes_emptyStore_doesNotPublish() {
		sync.publishStoredProfileAttributes();
		assertTrue(published.isEmpty());
	}

	@Test
	public void storedProfileAttributes_legacyConfirmedMapIsNotRestored() {
		deviceStored.put("confirmedAttributes", "{\"timezone\":\"Asia/Tokyo\"}");
		sync = newSync();
		sync.publishStoredProfileAttributes();
		assertTrue(published.isEmpty());
		sync.handleOperationalDataRuleResponse(consequence(Map.of("timezone", "Asia/Tokyo")));
		assertEquals(1, dispatched.size());
	}

	@Test
	public void storedProfileAttributes_restoresTokensAndPrioritizesProfileTimezone() {
		seedSynced(Map.of("timezone", "Asia/Tokyo", "tokens", Map.of("pushNotification", "token-1")));
		stored.put("timeZone", "Europe/London");
		sync.publishStoredProfileAttributes();
		assertEquals(
			Map.of("timezone", "Europe/London", "tokens", Map.of("pushNotification", "token-1")),
			lastProfileAttributes()
		);
	}

	@Test
	public void explicitPublication_emptyStore_stillPublishesForReset() {
		sync.publishProfileAttributes(null);
		assertEquals(1, published.size());
		assertTrue(lastProfileAttributes().isEmpty());
	}
}
