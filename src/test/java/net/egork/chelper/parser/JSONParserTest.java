package net.egork.chelper.parser;

import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.TestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JSONParserTest {
    private JSONParser parser;

    @BeforeEach
    void setUp() {
        parser = new JSONParser();
    }

    @Test
    void testParseValidPayload() {
        String payload = """
        {
            "name": "A. Beautiful Matrix",
            "group": "Codeforces - Codeforces Round #161 (Div. 2)",
            "languages": {
                "java": {
                    "mainClass": "Main",
                    "taskClass": "ABeautifulMatrix"
                }
            },
            "memoryLimit": 256,
            "testType": "single",
            "input": {
                "type": "stdin"
            },
            "output": {
                "type": "stdout"
            },
            "tests": [
                {
                    "input": "0 0 0 0 0\\n0 0 0 0 1\\n0 0 0 0 0\\n0 0 0 0 0\\n0 0 0 0 0\\n",
                    "output": "3\\n"
                }
            ]
        }
        """;

        Collection<Task> tasks = parser.parseTasks(payload);
        assertEquals(1, tasks.size());
        Task task = tasks.iterator().next();
        assertEquals("A. Beautiful Matrix", task.name);
        assertEquals("Codeforces - Codeforces Round #161 (Div. 2)", task.contestName);
        assertEquals("ABeautifulMatrix", task.taskClass);
        assertEquals("Main", task.mainClass);
        assertEquals("-Xmx256M", task.vmArgs);
        assertEquals(TestType.SINGLE, task.testType);
        assertEquals(StreamConfiguration.STANDARD, task.input);
        assertEquals(StreamConfiguration.STANDARD, task.output);
        assertEquals(1, task.tests.length);
        assertEquals(
                "0 0 0 0 0\n0 0 0 0 1\n0 0 0 0 0\n0 0 0 0 0\n0 0 0 0 0\n",
                task.tests[0].input
        );
        assertEquals("3\n", task.tests[0].output);
    }

    @Test
    void testParseInvalidJson() {
        String payload = "{ invalid json ]";
        Collection<Task> tasks = parser.parseTasks(payload);
        assertTrue(tasks.isEmpty());
    }

    @Test
    void testMissingFieldsHandledGracefully() {
        String payload = """
        {
            "name": "Test",
            "tests": []
        }
        """;
        Collection<Task> tasks = parser.parseTasks(payload);
        assertEquals(1, tasks.size());
        Task task = tasks.iterator().next();
        assertEquals("Test", task.name);
        assertEquals("", task.contestName); // missing string defaults to ""
        assertEquals(0, task.tests.length); // empty tests array
    }

    @Test
    void testFileStreamConfiguration() {
        String payload = """
        {
            "name": "File Task",
            "languages": {
                "java": {
                    "mainClass": "Main",
                    "taskClass": "Task"
                }
            },
            "testType": "multiNumber",
            "input": {
                "type": "file",
                "fileName": "input.txt"
            },
            "output": {
                "type": "file",
                "fileName": "output.txt"
            },
            "tests": []
        }
        """;

        Collection<Task> tasks = parser.parseTasks(payload);
        assertEquals(1, tasks.size());
        Task task = tasks.iterator().next();

        assertEquals(TestType.MULTI_NUMBER, task.testType);
        assertEquals(StreamConfiguration.StreamType.CUSTOM, task.input.type);
        assertEquals("input.txt", task.input.fileName);
        assertEquals(StreamConfiguration.StreamType.CUSTOM, task.output.type);
        assertEquals("output.txt", task.output.fileName);
    }
}
