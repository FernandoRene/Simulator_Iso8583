package com.iso8583.simulator.simulator;

import com.iso8583.simulator.core.enums.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class ScenarioRunner {

    private static final Logger logger = LoggerFactory.getLogger(ScenarioRunner.class);

    @Autowired
    private MessageSimulator messageSimulator;

    @Autowired
    private CSVDataLoader csvDataLoader;

    public ScenarioResult runScenario(ScenarioConfig config) {
        logger.info("Starting scenario: {}", config.getName());

        ScenarioResult result = new ScenarioResult(config.getName());
        result.setStartTime(System.currentTimeMillis());

        try {
            // Load test data if specified
            List<Map<String, String>> testData = loadTestData(config);

            // Execute scenario steps
            for (ScenarioStep step : config.getSteps()) {
                executeStep(step, testData, result);
            }

        } catch (Exception e) {
            logger.error("Error executing scenario: {}", e.getMessage(), e);
            result.addError("Scenario execution failed: " + e.getMessage());
        } finally {
            result.setEndTime(System.currentTimeMillis());
        }

        logger.info("Scenario completed: {} - Success: {}", config.getName(), result.isSuccess());
        return result;
    }

    private List<Map<String, String>> loadTestData(ScenarioConfig config) {
        if (config.getTestDataFile() != null) {
            try {
                return csvDataLoader.loadFromClasspath(config.getTestDataFile());
            } catch (Exception e) {
                logger.warn("Failed to load test data from {}: {}", config.getTestDataFile(), e.getMessage());
            }
        }

        // Return sample data if no file specified or loading failed
        List<Map<String, String>> sampleData = new ArrayList<>();
        sampleData.add(csvDataLoader.createSampleData());
        return sampleData;
    }

    private void executeStep(ScenarioStep step, List<Map<String, String>> testData, ScenarioResult result) {
        logger.debug("Executing step: {}", step.getName());

        try {
            Map<String, String> data = testData.get(0); // Use first record for now

            MessageSimulator.SimulationResult simResult = messageSimulator.sendSingleMessage(
                    step.getSwitchName(),
                    step.getMessageType(),
                    data
            );

            result.addStepResult(step.getName(), simResult);

        } catch (Exception e) {
            logger.error("Error executing step {}: {}", step.getName(), e.getMessage());
            result.addError("Step '" + step.getName() + "' failed: " + e.getMessage());
        }
    }

    // Configuration classes
    public static class ScenarioConfig {
        private String name;
        private String description;
        private String testDataFile;
        private List<ScenarioStep> steps = new ArrayList<>();

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getTestDataFile() { return testDataFile; }
        public void setTestDataFile(String testDataFile) { this.testDataFile = testDataFile; }

        public List<ScenarioStep> getSteps() { return steps; }
        public void setSteps(List<ScenarioStep> steps) { this.steps = steps; }
    }

    public static class ScenarioStep {
        private String name;
        private String switchName;
        private MessageType messageType;
        private Map<String, String> expectedResponse;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getSwitchName() { return switchName; }
        public void setSwitchName(String switchName) { this.switchName = switchName; }

        public MessageType getMessageType() { return messageType; }
        public void setMessageType(MessageType messageType) { this.messageType = messageType; }

        public Map<String, String> getExpectedResponse() { return expectedResponse; }
        public void setExpectedResponse(Map<String, String> expectedResponse) { this.expectedResponse = expectedResponse; }
    }

    public static class ScenarioResult {
        private String scenarioName;
        private long startTime;
        private long endTime;
        private boolean success = true;
        private List<String> errors = new ArrayList<>();
        private List<StepResult> stepResults = new ArrayList<>();

        public ScenarioResult(String scenarioName) {
            this.scenarioName = scenarioName;
        }

        public void addStepResult(String stepName, MessageSimulator.SimulationResult simResult) {
            stepResults.add(new StepResult(stepName, simResult));
            if (!simResult.isSuccess()) {
                success = false;
            }
        }

        public void addError(String error) {
            errors.add(error);
            success = false;
        }

        // Getters and setters
        public String getScenarioName() { return scenarioName; }
        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }
        public long getEndTime() { return endTime; }
        public void setEndTime(long endTime) { this.endTime = endTime; }
        public boolean isSuccess() { return success; }
        public List<String> getErrors() { return new ArrayList<>(errors); }
        public List<StepResult> getStepResults() { return new ArrayList<>(stepResults); }
        public long getDuration() { return endTime - startTime; }

        public static class StepResult {
            private String stepName;
            private MessageSimulator.SimulationResult simulationResult;

            public StepResult(String stepName, MessageSimulator.SimulationResult simulationResult) {
                this.stepName = stepName;
                this.simulationResult = simulationResult;
            }

            public String getStepName() { return stepName; }
            public MessageSimulator.SimulationResult getSimulationResult() { return simulationResult; }
        }
    }
}