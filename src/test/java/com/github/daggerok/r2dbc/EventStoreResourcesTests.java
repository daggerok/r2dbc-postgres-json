package com.github.daggerok.r2dbc;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("An event store REST API resources tests")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EventStoreResourcesTests {

  final WebTestClient client;

  @Test
  void should_stream_one_event() {
    var flux = client.mutate().responseTimeout(Duration.ofMinutes(1)).build()
                     .get()
                     .uri(uriBuilder -> uriBuilder.path("/event-stream").build())
                     .exchange()
                     .returnResult(Map.class)
                     .getResponseBody();
    StepVerifier.create(flux)
                .consumeNextWith(log::info)
                .thenCancel()
                .verify();
  }

  @Test
  void should_append_three_events_and_stream_them_back() {
    // given
    var aggregateId = UUID.randomUUID();
    var jsonData1 = API.Event.jsonb.toJson(new API.JsonData(null, null, aggregateId, "VisitorRegisteredEvent", "A test visitor", null, null));
    var jsonData2 = API.Event.jsonb.toJson(new API.JsonData(null, null, aggregateId, "PassCardDeliveredEvent", null, null, null));
    var jsonData3 = API.Event.jsonb.toJson(new API.JsonData(null, null, aggregateId, "EnteredTheDoorEvent", null, null, "IN-1"));
    // when
    var appended1stEvent = client.post()
                                 .uri(ub -> ub.path("/append-event").build())
                                 .bodyValue(Map.of("aggregateId", aggregateId,
                                                   "eventType", "VisitorRegisteredEvent",
                                                   "jsonData", jsonData1))
                                 .exchange()
                                 .expectStatus().isOk()
                                 .expectBody(new ParameterizedTypeReference<Map<String, String>>() {})
                                 .returnResult()
                                 .getResponseBody();
    // and
    var appended2ndEvent = client.post()
                                 .uri(ub -> ub.path("/append-event").build())
                                 .bodyValue(Map.of("aggregateId", aggregateId,
                                                   "eventType", "PassCardDeliveredEvent",
                                                   "jsonData", jsonData2))
                                 .exchange()
                                 .expectStatus().isOk()
                                 .expectBody(new ParameterizedTypeReference<Map<String, String>>() {})
                                 .returnResult()
                                 .getResponseBody();
    // and
    var appended3rdEvent = client.post()
                                 .uri(ub -> ub.path("/append-event").build())
                                 .bodyValue(Map.of("aggregateId", aggregateId,
                                                   "eventType", "EnteredTheDoorEvent",
                                                   "jsonData", jsonData3))
                                 .exchange()
                                 .expectStatus().isOk()
                                 .expectBody(new ParameterizedTypeReference<Map<String, String>>() {})
                                 .returnResult()
                                 .getResponseBody();
    // and
    var flux = client.get()
                     .uri(uriBuilder -> uriBuilder.path("/event-stream").build())
                     .exchange()
                     .returnResult(Map.class)
                     .getResponseBody()
                     .skip(1)
                     .doOnNext(log::info);
    // then
    assertThat(appended1stEvent).isNotNull();
    assertThat(appended2ndEvent).isNotNull();
    assertThat(appended3rdEvent).isNotNull();
    // and
    StepVerifier.create(flux)
                .consumeNextWith(appended1stEvent::equals)
                .consumeNextWith(appended2ndEvent::equals)
                .consumeNextWith(appended3rdEvent::equals)
                .thenCancel()
                .verify();
  }

  @Test
  void should_append_three_events_and_stream_them_back_using_aggregate_id() {
    // given
    var aggregateId = UUID.randomUUID();
    var jsonData1 = API.Event.jsonb.toJson(new API.JsonData(null, null, aggregateId, "VisitorRegisteredEvent", "A test visitor", null, null));
    var jsonData2 = API.Event.jsonb.toJson(new API.JsonData(null, null, aggregateId, "PassCardDeliveredEvent", null, null, null));
    var jsonData3 = API.Event.jsonb.toJson(new API.JsonData(null, null, aggregateId, "EnteredTheDoorEvent", null, null, "IN-1"));
    // when
    var appended1stEvent = client.post()
                                 .uri(ub -> ub.path("/append-event").build())
                                 .bodyValue(Map.of("aggregateId", aggregateId,
                                                   "eventType", "VisitorRegisteredEvent",
                                                   "jsonData", jsonData1))
                                 .exchange()
                                 .expectStatus().isOk()
                                 .expectBody(new ParameterizedTypeReference<Map<String, String>>() {})
                                 .returnResult()
                                 .getResponseBody();
    // and
    var appended2ndEvent = client.post()
                                 .uri(ub -> ub.path("/append-event").build())
                                 .bodyValue(Map.of("aggregateId", aggregateId,
                                                   "eventType", "PassCardDeliveredEvent",
                                                   "jsonData", jsonData2))
                                 .exchange()
                                 .expectStatus().isOk()
                                 .expectBody(new ParameterizedTypeReference<Map<String, String>>() {})
                                 .returnResult()
                                 .getResponseBody();
    // and
    var appended3rdEvent = client.post()
                                 .uri(ub -> ub.path("/append-event").build())
                                 .bodyValue(Map.of("aggregateId", aggregateId,
                                                   "eventType", "EnteredTheDoorEvent",
                                                   "jsonData", jsonData3))
                                 .exchange()
                                 .expectStatus().isOk()
                                 .expectBody(new ParameterizedTypeReference<Map<String, String>>() {})
                                 .returnResult()
                                 .getResponseBody();
    // and
    var flux = client.get()
                     .uri(uriBuilder -> uriBuilder.path("/event-stream/{aggregateId}").build(aggregateId))
                     .exchange()
                     .returnResult(Map.class)
                     .getResponseBody()
                     .doOnNext(log::info);
    // then
    assertThat(appended1stEvent).isNotNull();
    assertThat(appended2ndEvent).isNotNull();
    assertThat(appended3rdEvent).isNotNull();
    // and
    StepVerifier.create(flux)
                .consumeNextWith(appended1stEvent::equals)
                .consumeNextWith(appended2ndEvent::equals)
                .consumeNextWith(appended3rdEvent::equals)
                .thenCancel()
                .verify();
  }
}
