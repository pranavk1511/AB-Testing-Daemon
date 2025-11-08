package com.abdaemon.domain;

import java.util.List;

/** Optional targeting criteria such as country or minimum app version. */
public record Targets(List<String> countries, Integer minAppVersion) {
    public Targets {
        countries = (countries == null) ? List.of() : List.copyOf(countries);
    }

    public static Targets none() {
        return new Targets(List.of(), null);
    }
}
