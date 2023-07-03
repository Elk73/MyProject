package searchengine.controllers;

public record ControllerThread() {
    private static boolean isRun;
    public static boolean isIsRun() {
        return isRun;
    }

    public static void setIsRun(boolean isRun) {
        ControllerThread.isRun = isRun;
    }
}