package com.github.daggerok.r2dbc.customization;

import com.github.daggerok.r2dbc.core.Event;
import com.github.daggerok.r2dbc.customization.events.EnteredTheDoorEvent;
import com.github.daggerok.r2dbc.customization.events.PassCardDeliveredEvent;
import com.github.daggerok.r2dbc.customization.events.VisitorRegisteredEvent;
import jakarta.json.JsonObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.function.Function;

@Configuration
public class JsonObjectEventMappingConfig {

  @Bean
  Function<JsonObject, Mono<Event>> eventMapper() {
    return event -> {
      var type = Optional.ofNullable(event.getString("eventType", null))
                         .orElseThrow(RuntimeException::new);
      if (VisitorRegisteredEvent.class.getSimpleName().equals(type))
        return Mono.just(VisitorRegisteredEvent.from(event));
      if (PassCardDeliveredEvent.class.getSimpleName().equals(type))
        return Mono.just(PassCardDeliveredEvent.from(event));
      if (EnteredTheDoorEvent.class.getSimpleName().equals(type)) return Mono.just(EnteredTheDoorEvent.from(event));
      return Mono.empty();
    };
  }
}
