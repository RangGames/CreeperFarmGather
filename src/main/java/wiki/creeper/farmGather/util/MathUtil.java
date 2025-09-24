package wiki.creeper.farmGather.util;

public final class MathUtil {
    private MathUtil() {
    }

    public static float wrapDegrees(float value) {
        float wrapped = value % 360.0f;
        if (wrapped >= 180.0f) {
            wrapped -= 360.0f;
        }
        if (wrapped < -180.0f) {
            wrapped += 360.0f;
        }
        return wrapped;
    }

    public static float angleDifference(float a, float b) {
        return Math.abs(wrapDegrees(a - b));
    }
}
