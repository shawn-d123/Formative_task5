package integration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Small wrapper class that exposes one clear entry point per task.
 * The integrated menu calls only these methods.
 */
public final class TaskLaunchers {

    private TaskLaunchers() {
    }

    public static void launchMasterMind() throws Exception {
        invokeTaskMain("src_MasterMind_Emanuel.Main");
    }

    public static void launchZigzag() throws Exception {
        invokeTaskMain("src_ZigZag_Shamez.ZigZag");
    }

    public static void launchSnakesAndLadders() throws Exception {
        invokeTaskMain("src_SnakeAndLadder_Minhaj.SnakeandLadder");
    }

    public static void launchTrafficLight() throws Exception {
        invokeTaskMain("src_TrafficLights_shawn.TrafficLightMainController");
    }

    public static void launchSpyBot() throws Exception {
        invokeTaskMain("src_SpyBot_alwyn.SpyBot");
    }

    public static void launchDrawShape() throws Exception {
        invokeTaskMain("src_DrawShape_Miguel.DrawShape");
    }

    public static void launchNoughtsAndCrosses() throws Exception {
        invokeTaskMain("src_NoughtsAndCrosses_Prahlad.NoughtsAndCrosses");
    }

    public static void launchSearchForLight() throws Exception {
        invokeTaskMain("src_SearchForLight_Abid.SearchForLight");
    }

    public static void launchDance() throws Exception {
        invokeTaskMain("src_Dance_Shumail.SwiftBotDance");
    }

    public static void launchDetectObject() throws Exception {
        invokeTaskMain("src_DetectObject_chris.Detect_Object");
    }

    private static void invokeTaskMain(String className) throws Exception {
        try {
            Class<?> taskClass = Class.forName(className);
            Method mainMethod = taskClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) new String[0]);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException("Task failed with unknown cause.", cause);
        }
    }
}
