package org.mockserver.mock;

import org.junit.Before;
import org.junit.Test;
import org.mockserver.closurecallback.websocketregistry.WebSocketClientRegistry;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.matchers.TimeToLive;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.scheduler.Scheduler;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.ui.MockServerMatcherNotifier.Cause.API;

/**
 * @author jamesdbloom
 */
public class MockServerMatcherSequentialResponsesTest {

    private MockServerMatcher mockServerMatcher;

    @Before
    public void prepareTestFixture() {
        MockServerLogger mockLogFormatter = mock(MockServerLogger.class);
        Scheduler scheduler = mock(Scheduler.class);
        WebSocketClientRegistry webSocketClientRegistry = mock(WebSocketClientRegistry.class);
        mockServerMatcher = new MockServerMatcher(mockLogFormatter, scheduler, webSocketClientRegistry);
    }

    @Test
    public void respondWhenPathMatchesExpectationWithLimitedMatchesWithMultipleResponses() {
        // when
        Expectation expectationZero = new Expectation(new HttpRequest().withPath("somepath"), Times.exactly(2), TimeToLive.unlimited(), 0).thenRespond(response().withBody("somebody1"));
        mockServerMatcher.add(expectationZero, API);
        Expectation expectationOne = new Expectation(new HttpRequest().withPath("somepath"), Times.exactly(1), TimeToLive.unlimited(), 0).thenRespond(response().withBody("somebody2"));
        mockServerMatcher.add(expectationOne, API);
        Expectation expectationTwo = new Expectation(new HttpRequest().withPath("somepath")).thenRespond(response().withBody("somebody3"));
        mockServerMatcher.add(expectationTwo, API);

        // then
        assertEquals(expectationZero, mockServerMatcher.firstMatchingExpectation(new HttpRequest().withPath("somepath")));
        assertEquals(expectationZero, mockServerMatcher.firstMatchingExpectation(new HttpRequest().withPath("somepath")));
        assertEquals(expectationOne, mockServerMatcher.firstMatchingExpectation(new HttpRequest().withPath("somepath")));
        assertEquals(expectationTwo, mockServerMatcher.firstMatchingExpectation(new HttpRequest().withPath("somepath")));
    }

    @Test
    public void respondWhenPathMatchesExpectationWithPriorityAndLimitedMatchesWithMultipleResponses() {
        // when
        Expectation expectationZero = new Expectation(new HttpRequest().withPath("somepath"), Times.exactly(2), TimeToLive.unlimited(), 0).thenRespond(response().withBody("somebody1"));
        mockServerMatcher.add(expectationZero, API);
        Expectation expectationOne = new Expectation(new HttpRequest().withPath("somepath"), Times.exactly(1), TimeToLive.unlimited(), 10).thenRespond(response().withBody("somebody2"));
        mockServerMatcher.add(expectationOne, API);
        Expectation expectationTwo = new Expectation(new HttpRequest().withPath("somepath")).thenRespond(response().withBody("somebody3"));
        mockServerMatcher.add(expectationTwo, API);

        // then
        assertEquals(expectationOne, mockServerMatcher.firstMatchingExpectation(new HttpRequest().withPath("somepath")));
        assertEquals(expectationZero, mockServerMatcher.firstMatchingExpectation(new HttpRequest().withPath("somepath")));
        assertEquals(expectationZero, mockServerMatcher.firstMatchingExpectation(new HttpRequest().withPath("somepath")));
        assertEquals(expectationTwo, mockServerMatcher.firstMatchingExpectation(new HttpRequest().withPath("somepath")));
    }

    @Test
    public void respondWhenPathMatchesExpectationWithPriorityWithMultipleResponses() {
        // when
        Expectation expectationZero = new Expectation(new HttpRequest().withPath("somepath"), Times.unlimited(), TimeToLive.unlimited(), 0).thenRespond(response().withBody("somebody1"));
        mockServerMatcher.add(expectationZero, API);
        Expectation expectationOne = new Expectation(new HttpRequest().withPath("somepath"), Times.unlimited(), TimeToLive.unlimited(), 10).thenRespond(response().withBody("somebody2"));
        mockServerMatcher.add(expectationOne, API);
        Expectation expectationTwo = new Expectation(new HttpRequest().withPath("somepath"), Times.unlimited(), TimeToLive.unlimited(), 5).thenRespond(response().withBody("somebody3"));
        mockServerMatcher.add(expectationTwo, API);

        // then - match in priority order 10 (one) -> 5 (two) -> 0 (zero)
        assertEquals(expectationOne, mockServerMatcher.firstMatchingExpectation(new HttpRequest().withPath("somepath")));

        // when
        Expectation expectationZeroWithHigherPriority = new Expectation(new HttpRequest().withPath("somepath"), Times.unlimited(), TimeToLive.unlimited(), 15)
            .withId(expectationZero.getId())
            .thenRespond(response().withBody("somebody1"));
        mockServerMatcher.update(new Expectation[]{expectationZeroWithHigherPriority}, API);

        // then - match in priority order 15 (zero) -> 10 (one) -> 5 (two)
        assertEquals(expectationZeroWithHigherPriority, mockServerMatcher.firstMatchingExpectation(new HttpRequest().withPath("somepath")));

        // when
        Expectation expectationTwoWithHigherPriority = new Expectation(new HttpRequest().withPath("somepath"), Times.unlimited(), TimeToLive.unlimited(), 20)
            .withId(expectationTwo.getId())
            .thenRespond(response().withBody("somebody3"));
        mockServerMatcher.update(new Expectation[]{expectationTwoWithHigherPriority}, API);

        // then - match in priority order 20 (two) -> 15 (zero) -> 10 (one)
        assertEquals(expectationTwoWithHigherPriority, mockServerMatcher.firstMatchingExpectation(new HttpRequest().withPath("somepath")));
    }

    @Test
    public void respondWhenPathMatchesMultipleDifferentResponses() {
        // when
        Expectation expectationZero = new Expectation(new HttpRequest().withPath("somepath1")).thenRespond(response().withBody("somebody1"));
        mockServerMatcher.add(expectationZero, API);
        Expectation expectationOne = new Expectation(new HttpRequest().withPath("somepath2")).thenRespond(response().withBody("somebody2"));
        mockServerMatcher.add(expectationOne, API);

        // then
        assertEquals(expectationZero, mockServerMatcher.firstMatchingExpectation(new HttpRequest().withPath("somepath1")));
        assertEquals(expectationZero, mockServerMatcher.firstMatchingExpectation(new HttpRequest().withPath("somepath1")));
        assertEquals(expectationOne, mockServerMatcher.firstMatchingExpectation(new HttpRequest().withPath("somepath2")));
        assertEquals(expectationOne, mockServerMatcher.firstMatchingExpectation(new HttpRequest().withPath("somepath2")));
        assertEquals(expectationZero, mockServerMatcher.firstMatchingExpectation(new HttpRequest().withPath("somepath1")));
        assertEquals(expectationOne, mockServerMatcher.firstMatchingExpectation(new HttpRequest().withPath("somepath2")));
    }

    @Test
    public void doesNotRespondAfterMatchesFinishedExpectedTimes() {
        // when
        Expectation expectationZero = new Expectation(new HttpRequest().withPath("somepath"), Times.exactly(2), TimeToLive.unlimited(), 0).thenRespond(response().withBody("somebody"));
        mockServerMatcher.add(expectationZero, API);

        // then
        assertEquals(expectationZero, mockServerMatcher.firstMatchingExpectation(new HttpRequest().withPath("somepath")));
        assertEquals(expectationZero, mockServerMatcher.firstMatchingExpectation(new HttpRequest().withPath("somepath")));
        assertNull(mockServerMatcher.firstMatchingExpectation(new HttpRequest().withPath("somepath")));
    }

    @Test
    public void doesNotRespondAfterTimeToLiveFinishedExpectedTimes() throws InterruptedException {
        // when
        Expectation expectationZero = new Expectation(new HttpRequest().withPath("somepath"), Times.unlimited(), TimeToLive.exactly(SECONDS, 2L), 0).thenRespond(response().withBody("somebody"));
        mockServerMatcher.add(expectationZero, API);

        // then
        assertEquals(expectationZero, mockServerMatcher.firstMatchingExpectation(new HttpRequest().withPath("somepath")));
        MILLISECONDS.sleep(2250L);
        assertEquals(isNull(), mockServerMatcher.firstMatchingExpectation(new HttpRequest().withPath("somepath")));
        assertNull(mockServerMatcher.firstMatchingExpectation(new HttpRequest().withPath("somepath")));
    }


}
