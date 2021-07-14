package com.camunda.training;

import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.camunda.bpm.extension.process_test_coverage.junit.rules.TestCoverageProcessEngineRuleBuilder;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;

public class ProcessJUnitTest {

  @Rule
  @ClassRule
  public static ProcessEngineRule rule = TestCoverageProcessEngineRuleBuilder.create().build();

  @Before
  public void setup() {
    Mocks.register("createTweetDelegate", new LoggerDelegate());
    init(rule.getProcessEngine());
  }

  @Test
  @Deployment(resources = "twitterqa.bpmn")
  public void testHappyPath() {
    // Create a HashMap to put in variables for the process instance
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("content", "Running from JUnit Test with Random Number: " + ThreadLocalRandom.current().nextInt());
    // Start process with Java API and variables
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TwitterQAProcess", variables);
    assertThat(processInstance).isStarted();
    // Make assertions on the process instance
    /* Map<String, Object> userTaskVariables = new HashMap<String, Object>();
    userTaskVariables.put("approved", true);
    List<Task> taskList = taskService().createTaskQuery().taskCandidateGroup("management").processInstanceId(processInstance.getId()).list();
    org.assertj.core.api.Assertions.assertThat(taskList).isNotNull();
    org.assertj.core.api.Assertions.assertThat(taskList).hasSize(1);
    Task task = taskList.get(0);
    taskService().complete(task.getId(), userTaskVariables); */

    //Fast Way to complete UserTask with mandatory checks
    assertThat(processInstance).isWaitingAt("ReviewTweet_UserTask");
    complete(task("ReviewTweet_UserTask"), withVariables("approved", true));

    //Asynch Behaviour detailled
    /*
    List<Job> jobList = managementService().createJobQuery().processInstanceId(processInstance.getId()).list();
    org.assertj.core.api.Assertions.assertThat(jobList).hasSize(1);
    Job job = jobList.get(0);
    execute(job);*/

    assertThat(processInstance).isWaitingAt("PublishTweet_ServiceTask");
    execute(job());

    assertThat(processInstance).isEnded().hasPassed("PublishTweet_ServiceTask");
  }

}
