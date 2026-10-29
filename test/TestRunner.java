import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight standalone CLI test runner for JUnit 5 test classes.
 * Executes all @Test methods with @BeforeEach and @AfterEach lifecycle support
 * without requiring Gradle, Maven, or IDE-bundled test runners.
 */
public class TestRunner {
    public static void main(String[] args) throws Exception {
        Class<?>[] testClasses = new Class<?>[] {
            Class.forName("hk.edu.polyu.comp.comp2021.crobs.model.CROBSTest"),
            Class.forName("hk.edu.polyu.comp.comp2021.crobs.gui.CrobsGuiTest"),
            Class.forName("hk.edu.polyu.comp.comp2021.crobs.gui.CrobsGuiEndToEndTest")
        };

        int passed = 0;
        int failed = 0;
        List<String> failureMessages = new ArrayList<>();

        for (Class<?> clazz : testClasses) {
            System.out.println("Running " + clazz.getSimpleName() + "...");
            List<Method> beforeMethods = new ArrayList<>();
            List<Method> afterMethods = new ArrayList<>();
            List<Method> testMethods = new ArrayList<>();

            for (Method m : clazz.getDeclaredMethods()) {
                if (m.isAnnotationPresent(BeforeEach.class)) {
                    m.setAccessible(true);
                    beforeMethods.add(m);
                } else if (m.isAnnotationPresent(AfterEach.class)) {
                    m.setAccessible(true);
                    afterMethods.add(m);
                } else if (m.isAnnotationPresent(Test.class)) {
                    m.setAccessible(true);
                    testMethods.add(m);
                }
            }

            for (Method testMethod : testMethods) {
                try {
                    Constructor<?> constructor = clazz.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    Object instance = constructor.newInstance();

                    for (Method b : beforeMethods) {
                        b.invoke(instance);
                    }
                    testMethod.invoke(instance);
                    for (Method a : afterMethods) {
                        a.invoke(instance);
                    }
                    passed++;
                    System.out.println("  PASS: " + testMethod.getName());
                } catch (Exception e) {
                    failed++;
                    Throwable cause = (e.getCause() != null) ? e.getCause() : e;
                    System.out.println("  FAIL: " + testMethod.getName() + " -> " + cause);
                    cause.printStackTrace(System.out);
                    failureMessages.add(clazz.getSimpleName() + "." + testMethod.getName() + ": " + cause.getMessage());
                }
            }
        }

        System.out.println("\n-------------------------------------------");
        System.out.println("RESULTS: Total=" + (passed + failed) + ", Passed=" + passed + ", Failed=" + failed);
        if (failed > 0) {
            System.exit(1);
        } else {
            System.exit(0);
        }
    }
}
