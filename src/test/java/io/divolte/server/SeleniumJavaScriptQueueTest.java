/*
 * Copyright 2018 GoDataDriven B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.divolte.server;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.divolte.server.ServerTestUtils.EventPayload;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.openqa.selenium.By;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.divolte.server.SeleniumTestBase.TestPages.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@ParametersAreNonnullByDefault
public class SeleniumJavaScriptQueueTest extends SeleniumTestBase {
    @Parameterized.Parameters(name = "Selenium JS test: {1} (queue={2}, quirks-mode={3})")
    public static Iterable<Object[]> sauceLabBrowsersToTest() {
        // For each browser, we need to run in and out of quirks mode.
        return getBrowserList().stream()
            .flatMap((browser) ->
                ImmutableList.of(
                    new Object[] { browser[0], browser[1], true, false  },
                    new Object[] { browser[0], browser[1], true, true  }).stream())
            .collect(Collectors.toList());
    }

    @Test
    public void shouldQueueEventsBeforeDivolteLoaded() throws Exception {
        doSetUp();
        Preconditions.checkState(null != server);

        gotoPage(QUEUE_BEFORE_ASYNC);

        assertEquals(Optional.of("pageView"), server.waitForEvent().event.eventType);
        assertEquals(Optional.of("firstEvent"), server.waitForEvent().event.eventType);
        assertEquals(Optional.of("secondEvent"), server.waitForEvent().event.eventType);
    }

    @Test
    public void shouldWorkEvenIfQueueAddedAfterwards() throws Exception {
        doSetUp();
        Preconditions.checkState(null != server);

        gotoPage(QUEUE_AFTER_SYNC);

        assertEquals(Optional.of("pageView"), server.waitForEvent().event.eventType);
        assertEquals(Optional.of("firstEvent"), server.waitForEvent().event.eventType);
        assertEquals(Optional.of("secondEvent"), server.waitForEvent().event.eventType);
    }

    @Test
    public void shouldWorkEvenIfQueueAddedTwice() throws Exception {
        doSetUp();
        Preconditions.checkState(null != server);

        gotoPage(QUEUE_ADDED_TWICE);

        assertEquals(Optional.of("pageView"), server.waitForEvent().event.eventType);
        assertEquals(Optional.of("firstEvent"), server.waitForEvent().event.eventType);
        assertEquals(Optional.of("secondEvent"), server.waitForEvent().event.eventType);
    }

    @Test
    public void shouldAcceptEventsAfterQueueFlushed() throws Exception {
        doSetUp();
        Preconditions.checkState(null != server);

        gotoPage(QUEUE_BEFORE_ASYNC);
        assertEquals(Optional.of("pageView"), server.waitForEvent().event.eventType);
        assertEquals(Optional.of("firstEvent"), server.waitForEvent().event.eventType);
        assertEquals(Optional.of("secondEvent"), server.waitForEvent().event.eventType);

        driver.findElement(By.id("custom")).click();
        final EventPayload payload = server.waitForEvent();
        final DivolteEvent eventData = payload.event;

        assertTrue(eventData.eventType.isPresent());
        assertEquals("custom", eventData.eventType.get());

        final Optional<String> customEventParameters =
            eventData.eventParametersProducer.get().map(Object::toString);
        assertTrue(customEventParameters.isPresent());
        assertEquals("{\"a\":{}," +
                "\"b\":\"c\"," +
                "\"d\":{\"a\":[],\"b\":\"g\"}," +
                "\"e\":[\"1\",\"2\"]," +
                "\"f\":42," +
                "\"g\":53.2," +
                "\"h\":-37," +
                "\"i\":-7.83E-9," +
                "\"j\":true," +
                "\"k\":false," +
                "\"l\":null," +
                "\"m\":\"2015-06-13T15:49:33.002Z\"," +
                "\"n\":{}," +
                "\"o\":[{},{\"a\":\"b\"},{\"c\":\"d\"}]," +
                "\"p\":[null,null,{\"a\":\"b\"},\"custom\",null,{}]," +
                "\"q\":{}}",
            customEventParameters.get());
    }
}
