package com.camunda.training;

import org.camunda.bpm.dmn.engine.DmnDecisionTableResult;
import org.camunda.bpm.engine.DecisionService;
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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import twitter4j.TwitterException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;
import static org.mockito.ArgumentMatchers.anyString;

public class ProcessJUnitTest {

  @Mock
  TwitterService twitterService;

  @Rule
  @ClassRule
  public static ProcessEngineRule rule = TestCoverageProcessEngineRuleBuilder.create().build();

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    Mocks.register("createTweetDelegate", new CreateTweetDelegate(twitterService));
    init(rule.getProcessEngine());
  }

  @Test
  @Deployment(resources = "twitterqa.bpmn")
  public void testHappyPath() throws TwitterException {
    Mockito.when(twitterService.tweet(anyString())).thenReturn(10L);


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

    assertThat(processInstance).isEnded().hasPassed("PublishTweet_ServiceTask").variables().containsEntry("id",10L);
  }

  @Test
  @Deployment(resources = "twitterqa.bpmn")
  public void tweetRejected(){
    ProcessInstance processInstance = runtimeService()
            .createProcessInstanceByKey("TwitterQAProcess")
            .setVariables(withVariables("content", "Very mean things about life!", "approved", false))
            .startAfterActivity("ReviewTweet_UserTask")
            .execute();
    assertThat(processInstance).isStarted();

    assertThat(processInstance).isWaitingAt("RejectTweet_ExternalServiceTask");
    assertThat(externalTask()).hasTopicName("notification");
    complete(externalTask());

    assertThat(processInstance).isEnded().hasPassed("TweetRejected_EndEvent");
  }

  @Test
  @Deployment(resources = "twitterqa.bpmn")
  public void tweetSuperUser() {
    ProcessInstance processInstance = runtimeService()
            .createMessageCorrelation("superuserTweet")
            .setVariable("content", "Exercise 11 by Norman: " + ThreadLocalRandom.current().nextInt())
            .correlateWithResult()
            .getProcessInstance();
    assertThat(processInstance).isStarted();
    assertThat(processInstance).isWaitingAt("PublishTweet_ServiceTask");
    execute(job());

    assertThat(processInstance).isEnded();
  }

  @Test
  @Deployment(resources = "twitterqa.bpmn")
  public void tweetWithdrawn(){
    ProcessInstance processInstance = runtimeService()
            .startProcessInstanceByKey("TwitterQAProcess", "MyBusinessKey_001", withVariables("content", "Test tweetWithdrawn message"));
    assertThat(processInstance).isStarted();

    assertThat(processInstance).isWaitingAt("ReviewTweet_UserTask");
    runtimeService().createMessageCorrelation("tweetWithdrawn")
            .processInstanceBusinessKey("MyBusinessKey_001")
            .correlateWithResult();
    assertThat(processInstance).isEnded();
  }

  @Test
  @Deployment(resources = "tweetApproval.dmn")
  public void testTweetFromNorman() {
    DmnDecisionTableResult result = processEngine()
            .getDecisionService()
            .evaluateDecisionTableByKey("tweetApproval", withVariables("email","norman.luering234@camunda.com","content", "camunda rocks"));
    org.assertj.core.api.Assertions.assertThat(result.getFirstResult()).containsEntry("approved", true);
  }
}
