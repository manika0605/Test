package com.gap.platform.cucumber_utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.lang3.StringEscapeUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gap.platform.common.GAPReportGenerator;
import com.gap.platform.common.PostServiceCall;
import com.gap.platform.configuration.AppTestModel;
import com.gap.platform.configuration.Config;
import com.gap.platform.configuration.JiraResponse;
import com.gap.platform.configuration.TestConfig;
import com.gap.platform.jira_utils.JiraClient;
import com.gap.platform.junitutils.JunitListener;
import com.gap.platform.runners.CucumberRunner;
import com.gap.platform.runners.ExecutionSetup;

public class CucumberReporter {

	public static String testCaseName = "";

	public static String lineSeparator = System.getProperty("line.separator");
	private static final String SEPARATOR = System.getProperty("file.separator");
	
	public static int totalFeatures=0;
	public static int passedFeatures=0;
	public static StringBuffer jiraDesc = new StringBuffer();
	
	public static String getCurrentDateTime() {
		final DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		return sdf.format(date);
	}
	
	public static long getExecutionTime() {
		long startTime = JunitListener.startTime;
		return System.currentTimeMillis()-startTime;
	}

	public static void generateCucumberReport() throws Exception {
		AppTestModel model = new AppTestModel();
		boolean needToCreatejira = false;
		try {

			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
			objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
			String userDir = System.getProperty("user.dir");

			CucumberResults[] results = objectMapper.readValue(new File(userDir+SEPARATOR+"target"+SEPARATOR+"Destination"+SEPARATOR+"cucumber.json"),
					CucumberResults[].class);
			int featureCount = 0;
			for (CucumberResults result : results) {
				StringBuilder failedFeatureBuilder = new StringBuilder();

				if (result.getKeyword().equalsIgnoreCase("feature")) {
					if (CucumberRunner.retryCount == 0)
						totalFeatures++;
					featureCount++;
					StringBuilder builder = new StringBuilder();
					String featureId = result.getId();
					String featureName = result.getName();
					String featureDesc = result.getDescription();
					String featureUri = result.getUri();
					boolean featureStatus = true;

					failedFeatureBuilder.append("@failed" + lineSeparator);
					failedFeatureBuilder.append("Feature: " + featureName + lineSeparator);

					Elements[] elements = result.getElements();
					int scenarionCount = 0;
					int passedScenarioCount=0;
					for (Elements element : elements) {

						if (element.getType().equalsIgnoreCase("scenario")) {
							scenarionCount++;
							boolean testStatus = true;
							StringBuilder failedScenarioBuilder = new StringBuilder();
							String scenarioName = element.getName();
							String scenarioDesc = element.getDescription();
							String scenarioID = element.getId();
							String scenarionKeyword = element.getKeyword();
							int ScenarioLine = element.getLine();
							After[] after = element.getAfter();
							String[] output = after[0].getOutput();

							failedScenarioBuilder.append("Scenario" + ": " + scenarioName + lineSeparator);

							Steps[] steps = element.getSteps();
							testCaseName = scenarioName;

							ArrayList<String> scenarioLogs = new ArrayList();
							int stepCount = 0;
							boolean skipNextSteps = false;
							
							for (Steps step : steps) {
								stepCount++;
								String stepLine = step.getLine();

								String stepName = step.getName();
								String stepKeyword = step.getKeyword();
								String stepResult = step.getResult().getStatus();
								String stepDuration = step.getResult().getDuration();
								String matchLocation = step.getMatch().getLocation();
								Arguments[] matchArguments = step.getMatch().getArguments();

								failedScenarioBuilder.append(stepKeyword + " " + stepName + lineSeparator);

								// for (Arguments arg:matchArguments
								// ) {
								// String matchVal=arg.getVal();
								// String offset=arg.getOffset();
								// }

								ArrayList<String> stepLogs = new ArrayList<>();
								if (output != null) {
									for (String log : output) {
										if (log.startsWith(stepCount + "\\*#")) {
											if (log.split("\\*#").length > 1) {
												stepLogs.add(log.split("\\*#")[1]);

											}
										}
									}
								}
								int count = Integer.valueOf(scenarionCount + "" + stepCount);
								String name = stepKeyword + " " + stepName;

								if (stepResult.equalsIgnoreCase("passed")) {
									testStatus = true;

								} else if (stepResult.equalsIgnoreCase("failed") && !skipNextSteps) {

									testStatus = false;
									featureStatus = false;
									String error = step.getResult().getError_message();
									stepLogs.add(error);
								}

								String logs = String.join("", stepLogs);
								if (testStatus) {
									scenarioLogs.add(GAPReportGenerator.appendStepPass(logs, name, count).toString());
								} else if(skipNextSteps){
									scenarioLogs.add(GAPReportGenerator.appendStepSkip(logs, name, count).toString());
								}else {
									scenarioLogs.add(GAPReportGenerator.appendStepFail(logs, name, count, scenarioName).toString());
									skipNextSteps = true;
								}
							}
							
							String logs = String.join("", scenarioLogs);
							if (CucumberRunner.retryCount != 0) {
								testCaseName = testCaseName + " [ Retry : " + CucumberRunner.retryCount + " ]";
							}
							if (testStatus) {
								passedScenarioCount++;
								builder.append(GAPReportGenerator.appendScenarioPass(logs, testCaseName));
							} else {
								builder.append(GAPReportGenerator.appendScenarioFail(logs, testCaseName));
								failedFeatureBuilder.append(failedScenarioBuilder + lineSeparator);

								if (CucumberRunner.retryCount == ExecutionSetup.retryCount && Config.getInstance().getTestConfig().isJiraIssueLogging()) {
									String strRegEx = "<[^>]*>";
									String desc = logs.replaceAll(strRegEx, "");
									
									desc=StringEscapeUtils.escapeJava(desc);
									jiraDesc.append("Feature-" + featureName + "\\nFailed Scenario-" + testCaseName +"\\n\\n");
									//JiraClient.createJiraIssue("Feature-"+featureName +" :: Scenario-"+testCaseName,desc);

								}
							}
						}
					}
					if (CucumberRunner.retryCount != 0) {
						featureName = featureName + " [ Retry : " + CucumberRunner.retryCount + " ]";
					}
					if (featureStatus)
						passedFeatures++;
					GAPReportGenerator.appendFeature(featureName, builder, scenarionCount, passedScenarioCount);								
					if (!featureStatus) {
						int folderRetryName = CucumberRunner.retryCount+1;
						File folder = new File(CucumberRunner.retryFolder + System.getProperty("file.separator")+"Retry-"+ folderRetryName + System.getProperty("file.separator"));
						folder.mkdir();
						String fileName = CucumberRunner.retryFolder + System.getProperty("file.separator")+"Retry-"+ folderRetryName + System.getProperty("file.separator") + "feature_" + featureCount + "_retry_"
								+ CucumberRunner.retryCount + ".feature";
						Files.deleteIfExists(Paths.get(fileName));
						Files.createFile(Paths.get(fileName));
						Files.write(Paths.get(fileName), failedFeatureBuilder.toString().getBytes(),
								StandardOpenOption.APPEND);
					}
					
					
					  model.setAppName(TestConfig.getProjectName());
					  model.setFailedTestCases(scenarionCount-passedScenarioCount);
					  model.setPassTestCases(passedScenarioCount);
					  model.setTotalTestCases(scenarionCount);
					  model.setStatus(model.getFailedTestCases() > 0 ? "failed" : "success");
					  if(model.getFailedTestCases() > 0) {
						  needToCreatejira = true;
					  }
					  model.setIssueDescription(model.getFailedTestCases() > 0 ?
					  TestConfig.getProjectName()+ " automation test cases failed" : "");
					  model.setJiraAssignee("Psurya");
					  model.setIssueSummary(model.getFailedTestCases() > 0 ?
					  TestConfig.getProjectName()+" build failed" : "");
					  model.setAppOwner("Shoman Nath"); model.setTotalExecutionTime(getExecutionTime());
					  model.setExecutionDate(getCurrentDateTime());
					  //model.setJiraUrl("https://jira.gapinc.com/browse/APT-1201");
				}
			
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			
			if(needToCreatejira) {
				JiraResponse resp = JiraClient.createJiraIssue(Config.getInstance().getTestConfig().getProjectName()+" Validation failed", jiraDesc.toString());
				if(resp!= null && resp.getKey() != null) {
					model.setJiraUrl("https://jira.gapinc.com/browse/"+resp.getKey());
				}
			}
			PostServiceCall.invokePostCall(model);
			PostServiceCall.invokePostServiceWithAttachment(TestConfig.getProjectName());
		}

	}
}