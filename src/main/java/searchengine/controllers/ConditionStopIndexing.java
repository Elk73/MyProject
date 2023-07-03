package searchengine.controllers;

public record ConditionStopIndexing() {
    private static boolean isStop;
    private static boolean afterStop;
    public static boolean isIsStop() {
        return isStop;
    }
    public static void setIsStop(boolean isStop) {
        ConditionStopIndexing.isStop = isStop;
    }
    public static boolean isAfterStop() {
        return afterStop;
    }

    public static void setAfterStop(boolean afterStop) {
        ConditionStopIndexing.afterStop = afterStop;
    }
}

