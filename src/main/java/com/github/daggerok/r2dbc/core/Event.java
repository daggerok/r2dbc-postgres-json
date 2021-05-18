package com.github.daggerok.r2dbc.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@Table("domain_events")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED, onConstructor_ = @PersistenceConstructor)
public class Event {

  public static final Jsonb jsonb = JsonbBuilder.create();

  /* common event fields: */
  @Id
  @Setter(AccessLevel.PUBLIC)
  private Long sequenceNumber;

  private UUID aggregateId;

  private String jsonData;
}
