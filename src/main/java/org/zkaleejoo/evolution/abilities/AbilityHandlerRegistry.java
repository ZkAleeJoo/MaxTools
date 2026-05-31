package org.zkaleejoo.evolution.abilities;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import org.zkaleejoo.evolution.AbilityType;

public class AbilityHandlerRegistry {

    @SuppressWarnings("null")
    private final Map<AbilityType, AbilityHandler> handlers = new EnumMap<>(AbilityType.class);

    public AbilityHandlerRegistry register(AbilityType abilityType, AbilityHandler handler) {
        handlers.put(abilityType, handler);
        return this;
    }

    public Optional<AbilityHandler> find(AbilityType abilityType) {
        return Optional.ofNullable(handlers.get(abilityType));
    }

    public Map<AbilityType, AbilityHandler> asMap() {
        return Map.copyOf(handlers);
    }
}
