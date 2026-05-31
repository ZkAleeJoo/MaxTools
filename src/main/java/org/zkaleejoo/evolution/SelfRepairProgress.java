package org.zkaleejoo.evolution;

public class SelfRepairProgress {

    private static final double MAX_MOVEMENT_PER_EVENT = 4.0D;

    private final double threshold;

    public SelfRepairProgress(double threshold) {
        this.threshold = Math.max(1.0D, threshold);
    }

    public Result record(double movedDistance, double currentDistance) {
        double safeCurrent = Math.max(0.0D, currentDistance);
        if (movedDistance <= 0.0D || movedDistance > MAX_MOVEMENT_PER_EVENT) {
            return new Result(safeCurrent, 0, true);
        }

        double updated = safeCurrent + movedDistance;
        int activations = (int) Math.floor(updated / threshold);
        double remaining = updated - (activations * threshold);
        return new Result(remaining, activations, false);
    }

    public double threshold() {
        return threshold;
    }

    public record Result(double remainingDistance, int activations, boolean ignored) {
    }
}
