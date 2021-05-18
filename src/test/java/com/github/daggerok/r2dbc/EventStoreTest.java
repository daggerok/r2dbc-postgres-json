package com.github.daggerok.r2dbc;

import com.github.daggerok.r2dbc.core.EventStore;
import com.github.daggerok.r2dbc.customization.events.PassCardDeliveredEvent;
import com.github.daggerok.r2dbc.customization.events.VisitorRegisteredEvent;
import jakarta.json.Json;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Log4j2
@SpringBootTest
@DisplayName("An event store tests")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EventStoreTest {

  @Autowired
  EventStore eventStore;

  @BeforeEach
  void setUp() {
    eventStore.deleteAll().subscribe(log::info);
  }

  @Test
  void should_save_event() {
    // given
    var aggregateId = UUID.randomUUID().toString();
    var expireAt = LocalDateTime.now()
                                .plus(1, ChronoUnit.DAYS)
                                .toString();
    var jsonObject = Json.createObjectBuilder()
                         .add("aggregateId", aggregateId)
                         .add("expireAt", expireAt)
                         .add("name", "A test")
                         .build()
                         .toString();
    var eventObject = Json.createObjectBuilder(Map.of("aggregateId", aggregateId,
                                                      "eventType", VisitorRegisteredEvent.class.getSimpleName(),
                                                      "jsonData", jsonObject))
                          .build();
    // then
    StepVerifier.create(eventStore.save(VisitorRegisteredEvent.from(eventObject)))
                .consumeNextWith(log::info)
                .verifyComplete();
  }

  @Test
  void should_find_all() {
    // given
    var aggregateId = UUID.randomUUID().toString();
    var jsonObject = Json.createObjectBuilder()
                         .add("aggregateId", aggregateId)
                         .build();
    var eventObject = Json.createObjectBuilder(Map.of("aggregateId", aggregateId,
                                                      "eventType", PassCardDeliveredEvent.class.getSimpleName(),
                                                      "jsonData", jsonObject.toString()))
                          .build();
    // and
    StepVerifier.create(eventStore.save(PassCardDeliveredEvent.from(eventObject)))
                .consumeNextWith(log::info) // consume: one
                .verifyComplete();
    // then
    StepVerifier.create(eventStore.findAll())
                .consumeNextWith(log::info) // consume: 1
                .verifyComplete();
  }

  @Test
  void should_not_find_by_unknown_aggregate_id() {
    StepVerifier.create(eventStore.findByAggregateIdOrderBySequenceNumberAsc(UUID.fromString("0-0-0-0-1")))
                .verifyComplete(); // nothing to consume...
  }
}
