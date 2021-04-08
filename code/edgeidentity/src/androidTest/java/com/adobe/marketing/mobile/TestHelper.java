/*
  Copyright 2021 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.adobe.marketing.mobile.MonitorExtension.EventSpec;
import com.adobe.marketing.mobile.edge.identity.ADBCountDownLatch;
import com.adobe.marketing.mobile.services.ServiceProvider;

import androidx.test.platform.app.InstrumentationRegistry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test helper for functional testing to read, write, reset and assert against eventhub events, shared states and persistence data.
 */
public class TestHelper {
	private static final String TAG = "TestHelper";
	static final int WAIT_TIMEOUT_MS = 1000;
	static final int WAIT_EVENT_TIMEOUT_MS = 2000;
	static Application defaultApplication;

	// List of threads to wait for after test execution
	private static List<String> knownThreads = new ArrayList<String>();

	{
		knownThreads.add("pool"); // used for threads that execute the listeners code
		knownThreads.add("ADB"); // module internal threads
	}

	/**
	 * {@code TestRule} which sets up the MobileCore for testing before each test execution, and
	 * tearsdown the MobileCore after test execution.
	 * <p>
	 * To use, add the following to your test class:
	 * <pre>
	 *    @Rule
	 *    public TestHelper.SetupCoreRule coreRule = new TestHelper.SetupCoreRule();
	 * </pre>
	 */
	public static class SetupCoreRule implements TestRule {

		@Override
		public Statement apply(final Statement base, final Description description) {
			return new Statement() {
				@Override
				public void evaluate() throws Throwable {
					if (defaultApplication == null) {
						Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
						defaultApplication = Instrumentation.newApplication(CustomApplication.class, context);
					}

					ServiceProvider.getInstance().setNetworkService(new MockNetworkService()); // set mock network service for testing
					MobileCore.setLogLevel(LoggingMode.VERBOSE);
					MobileCore.setApplication(defaultApplication);

					try {
						base.evaluate();
					} catch (Throwable e) {
						MobileCore.log(LoggingMode.DEBUG, "SetupCoreRule", "Wait after test failure.");
						throw e; // rethrow test failure
					} finally {
						// After test execution
						MobileCore.log(LoggingMode.DEBUG, "SetupCoreRule", "Finished '" + description.getMethodName() + "'");
						waitForThreads(5000); // wait to allow thread to run after test execution
						Core core = MobileCore.getCore();

						if (core != null && core.eventHub != null) {
							core.eventHub.shutdown();
							core.eventHub = null;
						}

						MobileCore.setCore(null);
						TestPersistenceHelper.resetKnownPersistence();
						resetTestExpectations();
					}
				}
			};
		}
	}

	/**
	 * {@code TestRule} which registers the {@code MonitorExtension}, allowing test cases to assert
	 * events passing through the {@code EventHub}. This {@code TestRule} must be applied after
	 * the {@link SetupCoreRule} to ensure the {@code MobileCore} is setup for testing first.
	 * <p>
	 * To use, add the following to your test class:
	 * <pre>
	 *  @Rule
	 *    public RuleChain rule = RuleChain.outerRule(new SetupCoreRule())
	 * 							.around(new RegisterMonitorExtensionRule());
	 * </pre>
	 */
	public static class RegisterMonitorExtensionRule implements TestRule {

		@Override
		public Statement apply(final Statement base, final Description description) {
			return new Statement() {
				@Override
				public void evaluate() throws Throwable {
					MonitorExtension.registerExtension();

					try {
						base.evaluate();
					} finally {
						MonitorExtension.reset();
					}
				}
			};
		}
	}

	/**
	 * Waits for all the {@code #knownThreads} to finish or fails the test after timeoutMillis if some of them are still running
	 * when the timer expires. If timeoutMillis is 0, a default timeout will be set = 1000ms
	 *
	 * @param timeoutMillis max waiting time
	 */
	public static void waitForThreads(final int timeoutMillis) {
		int TEST_DEFAULT_TIMEOUT_MS = 1000;
		int TEST_DEFAULT_SLEEP_MS = 50;
		int TEST_INITIAL_SLEEP_MS = 100;

		long startTime = System.currentTimeMillis();
		int timeoutTestMillis = timeoutMillis > 0 ? timeoutMillis : TEST_DEFAULT_TIMEOUT_MS;
		int sleepTime = Math.min(timeoutTestMillis, TEST_DEFAULT_SLEEP_MS);

		sleep(TEST_INITIAL_SLEEP_MS);
		Set<Thread> threadSet = getEligibleThreads();

		while (threadSet.size() > 0 && ((System.currentTimeMillis() - startTime) < timeoutTestMillis)) {
			MobileCore.log(LoggingMode.DEBUG, TAG, "waitForThreads - Still waiting for " + threadSet.size() + " thread(s)");

			for (Thread t : threadSet) {

				MobileCore.log(LoggingMode.DEBUG, TAG, "waitForThreads - Waiting for thread " + t.getName() + " (" + t.getId() + ")");
				boolean done = false;
				boolean timedOut = false;

				while (!done && !timedOut) {
					if (t.getState().equals(Thread.State.TERMINATED)
							|| t.getState().equals(Thread.State.TIMED_WAITING)
							|| t.getState().equals(Thread.State.WAITING)) {
						//Cannot use the join() API since we use a cached thread pool, which
						//means that we keep idle threads around for 60secs (default timeout).
						done = true;
					} else {
						//blocking
						sleep(sleepTime);
						timedOut = (System.currentTimeMillis() - startTime) > timeoutTestMillis;
					}
				}

				if (timedOut) {
					MobileCore.log(LoggingMode.DEBUG, TAG,
								   "waitForThreads - Timeout out waiting for thread " + t.getName() + " (" + t.getId() + ")");
				} else {
					MobileCore.log(LoggingMode.DEBUG, TAG,
								   "waitForThreads - Done waiting for thread " + t.getName() + " (" + t.getId() + ")");
				}
			}

			threadSet = getEligibleThreads();
		}

		MobileCore.log(LoggingMode.DEBUG, TAG, "waitForThreads - All known threads are terminated.");
	}

	/**
	 * Retrieves all the known threads that are still running
	 *
	 * @return set of running tests
	 */
	private static Set<Thread> getEligibleThreads() {
		Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
		Set<Thread> eligibleThreads = new HashSet<Thread>();

		for (Thread t : threadSet) {
			if (isAppThread(t) && !t.getState().equals(Thread.State.WAITING) && !t.getState().equals(Thread.State.TERMINATED)
					&& !t.getState().equals(Thread.State.TIMED_WAITING)) {
				eligibleThreads.add(t);
			}
		}

		return eligibleThreads;
	}

	/**
	 * Checks if current thread is not a daemon and its name starts with one of the known thread names specified here
	 * {@link #knownThreads}
	 *
	 * @param t current thread to verify
	 * @return true if it is a known thread, false otherwise
	 */
	private static boolean isAppThread(final Thread t) {
		if (t.isDaemon()) {
			return false;
		}

		for (String prefix : knownThreads) {
			if (t.getName().startsWith(prefix)) {
				return true;
			}
		}

		return false;
	}


	/**
	 * Resets the network and event test expectations.
	 */
	public static void resetTestExpectations() {
		MobileCore.log(LoggingMode.DEBUG, TAG, "Resetting functional test expectations for events");
		MonitorExtension.reset();
	}

	// ---------------------------------------------------------------------------------------------
	// Event Test Helpers
	// ---------------------------------------------------------------------------------------------

	/**
	 * Sets an expectation for a specific event type and source and how many times the event should be dispatched.
	 *
	 * @param type   the event type
	 * @param source the event source
	 * @param count  the expected number of times the event is dispatched
	 * @throws IllegalArgumentException if {@code count} is less than 1
	 */
	public static void setExpectationEvent(final String type, final String source, final int count) {
		if (count < 1) {
			throw new IllegalArgumentException("Cannot set expectation event count less than 1!");
		}

		MonitorExtension.setExpectedEvent(type, source, count);
	}

	/**
	 * Asserts if all the expected events were received and fails if an unexpected event was seen.
	 *
	 * @param ignoreUnexpectedEvents if set on false, an assertion is made on unexpected events, otherwise the unexpected events are ignored
	 * @throws InterruptedException
	 * @see #setExpectationEvent(String, String, int)
	 * @see #assertUnexpectedEvents()
	 */
	public static void assertExpectedEvents(final boolean ignoreUnexpectedEvents) throws InterruptedException {
		Map<EventSpec, ADBCountDownLatch> expectedEvents = MonitorExtension.getExpectedEvents();

		if (expectedEvents.isEmpty()) {
			fail("There are no event expectations set, use this API after calling setExpectationEvent");
			return;
		}

		for (Map.Entry<EventSpec, ADBCountDownLatch> expected : expectedEvents.entrySet()) {
			boolean awaitResult = expected.getValue().await(WAIT_EVENT_TIMEOUT_MS,
								  TimeUnit.MILLISECONDS);
			assertTrue("Timed out waiting for event type " + expected.getKey().type + " and source " + expected.getKey().source,
					   awaitResult);
			int expectedCount = expected.getValue().getInitialCount();
			int receivedCount = expected.getValue().getCurrentCount();
			String failMessage = String.format("Expected %d events for '%s', but received %d", expectedCount, expected.getKey(),
											   receivedCount);
			assertEquals(failMessage, expectedCount, receivedCount);
		}

		if (!ignoreUnexpectedEvents) {
			assertUnexpectedEvents(false);
		}
	}

	/**
	 * Asserts if any unexpected event was received. Use this method to verify the received events
	 * are correct when setting event expectations. Waits a short time before evaluating received
	 * events to allow all events to come in.
	 *
	 * @see #setExpectationEvent
	 */
	public static void assertUnexpectedEvents() throws InterruptedException {
		assertUnexpectedEvents(true);
	}

	/**
	 * Asserts if any unexpected event was received. Use this method to verify the received events
	 * are correct when setting event expectations.
	 *
	 * @param shouldWait waits a short time to allow events to be received when true
	 * @see #setExpectationEvent
	 */
	public static void assertUnexpectedEvents(final boolean shouldWait) throws InterruptedException {
		// Short wait to allow events to come in
		if (shouldWait) {
			sleep(WAIT_TIMEOUT_MS);
		}

		int unexpectedEventsReceivedCount = 0;
		StringBuilder unexpectedEventsErrorString = new StringBuilder();

		Map<EventSpec, List<Event>> receivedEvents = MonitorExtension.getReceivedEvents();
		Map<EventSpec, ADBCountDownLatch> expectedEvents = MonitorExtension.getExpectedEvents();

		for (Map.Entry<EventSpec, List<Event>> receivedEvent : receivedEvents.entrySet()) {
			ADBCountDownLatch expectedEventLatch = expectedEvents.get(receivedEvent.getKey());

			if (expectedEventLatch != null) {
				expectedEventLatch.await(WAIT_EVENT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
				int expectedCount = expectedEventLatch.getInitialCount();
				int receivedCount = receivedEvent.getValue().size();
				String failMessage = String.format("Expected %d events for '%s', but received %d", expectedCount,
												   receivedEvent.getKey(), receivedCount);
				assertEquals(failMessage, expectedCount, receivedCount);
			} else {
				unexpectedEventsReceivedCount += receivedEvent.getValue().size();
				unexpectedEventsErrorString.append(String.format("(%s,%s,%d)",
												   receivedEvent.getKey().type,
												   receivedEvent.getKey().source,
												   receivedEvent.getValue().size())
												  );
				MobileCore.log(LoggingMode.DEBUG, TAG,
							   "Received unexpected event with type: " + receivedEvent.getKey().type + " source: " +
							   receivedEvent.getKey().source);
			}
		}

		assertEquals(String.format("Received %d unexpected event(s): %s", unexpectedEventsReceivedCount,
								   unexpectedEventsErrorString.toString()),
					 0, unexpectedEventsReceivedCount);
	}

	/**
	 * Returns the {@code Event}(s) dispatched through the Event Hub, or empty if none was found.
	 * Use this API after calling {@link #setExpectationEvent(String, String, int)} to wait for
	 * the expected events. The wait time for each event is {@link #WAIT_EVENT_TIMEOUT_MS}ms.
	 *
	 * @param type   the event type as in the expectation
	 * @param source the event source as in the expectation
	 * @return list of events with the provided {@code type} and {@code source}, or empty if none was dispatched
	 * @throws InterruptedException
	 * @throws IllegalArgumentException if {@code type} or {@code source} are null or empty strings
	 */
	public static List<Event> getDispatchedEventsWith(final String type, final String source) throws InterruptedException {
		return getDispatchedEventsWith(type, source, WAIT_EVENT_TIMEOUT_MS);
	}

	/**
	 * Returns the {@code Event}(s) dispatched through the Event Hub, or empty if none was found.
	 * Use this API after calling {@link #setExpectationEvent(String, String, int)} to wait for the right amount of time
	 *
	 * @param type    the event type as in the expectation
	 * @param source  the event source as in the expectation
	 * @param timeout how long should this method wait for the expected event, in milliseconds.
	 * @return list of events with the provided {@code type} and {@code source}, or empty if none was dispatched
	 * @throws InterruptedException
	 * @throws IllegalArgumentException if {@code type} or {@code source} are null or empty strings
	 */
	public static List<Event> getDispatchedEventsWith(final String type, final String source,
			int timeout) throws InterruptedException {
		EventSpec eventSpec = new EventSpec(source, type);

		Map<EventSpec, List<Event>> receivedEvents = MonitorExtension.getReceivedEvents();
		Map<EventSpec, ADBCountDownLatch> expectedEvents = MonitorExtension.getExpectedEvents();

		ADBCountDownLatch expectedEventLatch = expectedEvents.get(eventSpec);

		if (expectedEventLatch != null) {
			boolean awaitResult = expectedEventLatch.await(timeout, TimeUnit.MILLISECONDS);
			assertTrue("Timed out waiting for event type " + eventSpec.type + " and source " + eventSpec.source, awaitResult);
		} else {
			sleep(WAIT_TIMEOUT_MS);
		}

		return receivedEvents.containsKey(eventSpec) ? receivedEvents.get(eventSpec) : Collections.<Event>emptyList();
	}


	/**
	 * Synchronous call to get the shared state for the specified {@code stateOwner}.
	 * This API throws an assertion failure in case of timeout.
	 *
	 * @param stateOwner the owner extension of the shared state (typically the name of the extension)
	 * @param timeout    how long should this method wait for the requested shared state, in milliseconds
	 * @return latest shared state of the given {@code stateOwner} or null if no shared state was found
	 * @throws InterruptedException
	 */
	public static Map<String, Object> getSharedStateFor(final String stateOwner, int timeout) throws InterruptedException {
		Event event = new Event.Builder("Get Shared State Request", TestConstants.EventType.MONITOR,
										TestConstants.EventSource.SHARED_STATE_REQUEST)
		.setEventData(new HashMap<String, Object>() {
			{
				put(TestConstants.EventDataKey.STATE_OWNER, stateOwner);
			}
		})
		.build();

		final ADBCountDownLatch latch = new ADBCountDownLatch(1);
		final Map<String, Object> sharedState = new HashMap<>();
		MobileCore.dispatchEventWithResponseCallback(event,
		new AdobeCallback<Event>() {
			@Override
			public void call(Event event) {
				if (event.getEventData() != null) {
					sharedState.putAll(event.getEventData());
				}

				latch.countDown();
			}
		},
		new ExtensionErrorCallback<ExtensionError>() {
			@Override
			public void error(ExtensionError extensionError) {
				MobileCore.log(LoggingMode.ERROR, TAG, "Failed to get shared state for " + stateOwner + ": " + extensionError);
			}
		});

		assertTrue("Timeout waiting for shared state " + stateOwner, latch.await(timeout, TimeUnit.MILLISECONDS));
		return sharedState.isEmpty() ? null : sharedState;
	}

	/**
	 * Synchronous call to get the XDM shared state for the specified {@code stateOwner}.
	 * This API throws an assertion failure in case of timeout.
	 *
	 * @param stateOwner the owner extension of the shared state (typically the name of the extension)
	 * @param timeout    how long should this method wait for the requested shared state, in milliseconds
	 * @return latest shared state of the given {@code stateOwner} or null if no shared state was found
	 * @throws InterruptedException
	 */
	public static Map<String, Object> getXDMSharedStateFor(final String stateOwner,
			int timeout) throws InterruptedException {
		Event event = new Event.Builder("Get Shared State Request", TestConstants.EventType.MONITOR,
										TestConstants.EventSource.XDM_SHARED_STATE_REQUEST)
		.setEventData(new HashMap<String, Object>() {
			{
				put(TestConstants.EventDataKey.STATE_OWNER, stateOwner);
			}
		})
		.build();

		final ADBCountDownLatch latch = new ADBCountDownLatch(1);
		final Map<String, Object> sharedState = new HashMap<>();
		MobileCore.dispatchEventWithResponseCallback(event,
		new AdobeCallback<Event>() {
			@Override
			public void call(Event event) {
				if (event.getEventData() != null) {
					sharedState.putAll(event.getEventData());
				}

				latch.countDown();
			}
		},
		new ExtensionErrorCallback<ExtensionError>() {
			@Override
			public void error(ExtensionError extensionError) {
				MobileCore.log(LoggingMode.ERROR, TAG, "Failed to get shared state for " + stateOwner + ": " + extensionError);
			}
		});

		assertTrue("Timeout waiting for shared state " + stateOwner, latch.await(timeout, TimeUnit.MILLISECONDS));
		return sharedState.isEmpty() ? null : sharedState;
	}


	/**
	 * Pause test execution for the given {@code milliseconds}
	 *
	 * @param milliseconds the time to sleep the current thread.
	 */
	public static void sleep(int milliseconds) {
		try {
			Thread.sleep(milliseconds);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Dummy Application for the test instrumentation
	 */
	public static class CustomApplication extends Application {
		public CustomApplication() {
		}
	}

}