package net.egork.chelper.parser;

import net.egork.chelper.checkers.TokenChecker;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.Test;
import net.egork.chelper.task.TestType;
import net.egork.chelper.util.TaskUtilities;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collection;
import java.util.Collections;

public class JSONParser {
    public Collection<Task> parseTasks(String payload) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode obj = mapper.readTree(payload);

            String taskName = obj.path("name").asText();
            String contestName = obj.path("group").asText();

            JsonNode languages = obj.path("languages");
            JsonNode java = languages.path("java");

            String mainClass = java.path("mainClass").asText();
            String taskClass = TaskUtilities.replaceCyrillics(java.path("taskClass").asText());

            int memoryLimit = obj.path("memoryLimit").asInt();

            TestType type = stringToTestType(obj.path("testType").asText());
            StreamConfiguration inputConfig = parseStreamConfiguration(obj.path("input"));
            StreamConfiguration outputConfig = parseStreamConfiguration(obj.path("output"));

            JsonNode testsArr = obj.path("tests");
            Test[] tests = new Test[testsArr.size()];

            for (int i = 0, iMax = testsArr.size(); i < iMax; i++) {
                JsonNode testObj = testsArr.get(i);

                String testInput = testObj.path("input").asText();
                String testOutput = testObj.path("output").asText();

                tests[i] = new Test(testInput, testOutput);
            }

            Task task = new Task(taskName, type, inputConfig, outputConfig, tests,
                    null, "-Xmx" + memoryLimit + "M", mainClass, taskClass,
                    TokenChecker.class.getCanonicalName(), "",
                    new String[0], null, contestName, true,
                    null, null, false, false);

            return Collections.singleton(task);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private StreamConfiguration parseStreamConfiguration(JsonNode obj) {
        if (obj == null || obj.isMissingNode() || obj.isNull()) return null;
        String type = obj.path("type").asText();

        switch (type) {
            case "stdin":
            case "stdout":
                return StreamConfiguration.STANDARD;
            case "file":
                String fileName = obj.path("fileName").asText();
                return new StreamConfiguration(StreamConfiguration.StreamType.CUSTOM, fileName);
            case "regex":
                String regex = obj.path("pattern").asText();
                return new StreamConfiguration(StreamConfiguration.StreamType.LOCAL_REGEXP, regex);
        }

        return null;
    }

    private TestType stringToTestType(String str) {
        switch (str) {
            case "single":
                return TestType.SINGLE;
            case "multiNumber":
                return TestType.MULTI_NUMBER;
            case "multiEOF":
                return TestType.MULTI_EOF;
        }

        return TestType.SINGLE;
    }
}
