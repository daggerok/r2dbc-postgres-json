package com.github.daggerok.r2dbc.core;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Infrastructure {

  public static final Function<Throwable, Map<String, String>> wrap =
      throwable -> Map.of("error", throwable.getMessage());

  public static final Function<String, Supplier<RuntimeException>> error =
      message -> () -> new RuntimeException(message);
}
