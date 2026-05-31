package org.zkaleejoo.evolution;

import java.util.HashMap;
import java.util.Map;

class SelfRepairProgressTracker {

    private final Map<String, Double> distanceByKey = new HashMap<>();

    SelfRepairProgress.Result record(String key, double threshold, double movedDistance) {
        String safeKey = normalizeKey(key);
        double currentDistance = distanceByKey.getOrDefault(safeKey, 0.0D);
        SelfRepairProgress.Result result = new SelfRepairProgress(threshold).record(movedDistance, currentDistance);
        if (result.ignored()) {
            return result;
        }

        remember(safeKey, threshold, result.remainingDistance());
        return result;
    }

    void deferActivation(String key, double threshold, double remainingDistance) {
        remember(normalizeKey(key), threshold, remainingDistance + Math.max(1.0D, threshold));
    }

    void reset(String key) {
        distanceByKey.remove(normalizeKey(key));
    }

    void clearKeysStartingWith(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return;
        }
        distanceByKey.keySet().removeIf(key -> key.startsWith(prefix));
    }

    double currentDistance(String key) {
        return distanceByKey.getOrDefault(normalizeKey(key), 0.0D);
    }

    private void remember(String key, double threshold, double distance) {
        if (distance <= 0.0D) {
            distanceByKey.remove(key);
            return;
        }
        distanceByKey.put(key, Math.min(Math.max(1.0D, threshold), distance));
    }

    private String normalizeKey(String key) {
        return key == null || key.isBlank() ? "unknown" : key;
    }
}
