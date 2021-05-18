package com.github.daggerok.r2dbc.customization.events;

import com.github.daggerok.r2dbc.core.Event;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import lombok.*;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Data
@Getter
@Table("domain_events")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class VisitorRegisteredEvent extends Event {

  public static VisitorRegisteredEvent from(JsonObject event) {
    var jsonData = Optional.ofNullable(event.getString("jsonData", null))
                           .orElseThrow(RuntimeException::new);
    var jsonObject = Event.jsonb.fromJson(jsonData, JsonObject.class);
    var aggregateId = Optional.ofNullable(jsonObject.getString("aggregateId", null))
                              .map(UUID::fromString)
                              .orElseGet(UUID::randomUUID);
    var expireAt = Optional.ofNullable(jsonObject.getString("expireAt", null))
                           .map(LocalDateTime::parse)
                           .orElseGet(() -> LocalDateTime.now().plus(1, ChronoUnit.DAYS));
    var json = Json.createObjectBuilder(jsonObject)
                   .add("expireAt", expireAt.toString())
                   .add("updatedAt", LocalDateTime.now().toString())
                   .build()
                   .toString();

    return of(aggregateId, json);
  }

  public static VisitorRegisteredEvent of(UUID aggregateId, String json) {
    return new VisitorRegisteredEvent(aggregateId, json);
  }

  public VisitorRegisteredEvent(UUID aggregateId, String json) {
    super(null, aggregateId, json);
  }
}
