package dronesimulation.model.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EventBusTest {

  @Test
  void deliversToAllListenersInOrderWithClockTime() {
    EventBus bus = new EventBus();
    bus.setClock(() -> 12.5);
    List<String> received = new ArrayList<>();
    bus.subscribe(e -> received.add("a:" + e.type()));
    bus.subscribe(e -> received.add("b:" + e.time()));

    bus.publish(SimEventType.CRASH, "d1", "hit the ground at 9.0 m/s");
    bus.flush();

    assertEquals(List.of("a:CRASH", "b:12.5"), received);
  }

  @Test
  void eventsWaitForFlushAndArriveExactlyOnceInOrder() {
    EventBus bus = new EventBus();
    List<SimEventType> received = new ArrayList<>();
    bus.subscribe(e -> received.add(e.type()));

    bus.publish(SimEventType.TOOK_OFF, "d1", "up");
    bus.publish(SimEventType.LANDED, "d1", "down");
    assertTrue(received.isEmpty(), "nothing before the end of the step");
    assertEquals(2, bus.pendingCount());

    bus.flush();
    bus.flush();

    assertEquals(List.of(SimEventType.TOOK_OFF, SimEventType.LANDED), received);
    assertEquals(0, bus.pendingCount());
  }

  @Test
  void eventsPublishedByListenersAreDeliveredInTheSameFlush() {
    EventBus bus = new EventBus();
    List<SimEventType> received = new ArrayList<>();
    bus.subscribe(e -> {
      received.add(e.type());
      if (e.type() == SimEventType.LOW_BATTERY) {
        bus.publish(SimEventType.RETURN_HOME, e.droneId(), "going home");
      }
    });

    bus.publish(SimEventType.LOW_BATTERY, "d1", "20%");
    bus.flush();

    assertEquals(List.of(SimEventType.LOW_BATTERY, SimEventType.RETURN_HOME), received);
  }

  @Test
  void failingListenerIsReportedAndOthersStillReceive() {
    EventBus bus = new EventBus();
    List<String> errors = new ArrayList<>();
    List<SimEvent> received = new ArrayList<>();
    bus.setErrorHandler((listener, e) -> errors.add(e.getMessage()));
    bus.subscribe(e -> {
      throw new IllegalStateException("boom");
    });
    bus.subscribe(received::add);

    bus.publish(SimEventType.LANDED, "d1", "landed");
    bus.flush();

    assertEquals(List.of("boom"), errors);
    assertEquals(1, received.size());
  }

  @Test
  void unsubscribedListenerGetsNothing() {
    EventBus bus = new EventBus();
    List<SimEvent> received = new ArrayList<>();
    SimulationListener listener = received::add;
    bus.subscribe(listener);
    bus.unsubscribe(listener);

    bus.publish(SimEventType.LANDED, "d1", "landed");
    bus.flush();

    assertTrue(received.isEmpty());
  }

  @Test
  void eventRejectsNulls() {
    assertThrows(NullPointerException.class, () -> new SimEvent(null, "d1", 0, "x"));
    assertThrows(NullPointerException.class, () -> new EventBus().subscribe(null));
    assertThrows(NullPointerException.class, () -> new EventBus().setErrorHandler(null));
  }
}
