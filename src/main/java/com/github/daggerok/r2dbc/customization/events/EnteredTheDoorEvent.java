package com.github.daggerok.r2dbc.customization.events;

import com.github.daggerok.r2dbc.core.Event;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import lombok.*;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Data
@Getter
@Table("domain_events")
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class EnteredTheDoorEvent extends Event {

  public static EnteredTheDoorEvent from(JsonObject event) {
    var jsonData = Optional.ofNullable(event.getString("jsonData", null))
                           .orElseThrow(RuntimeException::new);
    var jsonObject = Event.jsonb.fromJson(jsonData, JsonObject.class);
    var aggregateId = Optional.ofNullable(jsonObject.getString("aggregateId", null))
                              .map(UUID::fromString)
                              .orElseThrow(RuntimeException::new);
    var json = Json.createObjectBuilder(jsonObject)
                   .add("updatedAt", LocalDateTime.now().toString())
                   .build()
                   .toString();
    return of(aggregateId, json);
  }

  public static EnteredTheDoorEvent of(UUID aggregateId, String json) {
    return new EnteredTheDoorEvent(aggregateId, json);
  }

  public EnteredTheDoorEvent(UUID aggregateId, String json) {
    super(null, aggregateId, json);
  }
}
