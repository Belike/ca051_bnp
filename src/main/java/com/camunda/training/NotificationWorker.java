package com.camunda.training;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.topic.TopicSubscriptionBuilder;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class NotificationWorker {
    public static void main(String [] args){
        ExternalTaskClient client = ExternalTaskClient.create()
                .baseUrl("http://localhost:8080/engine-rest")
                .lockDuration(60000)
                .maxTasks(1)
                .build();

        TopicSubscriptionBuilder subscriptionBuilder = client.subscribe("notification");
        subscriptionBuilder.handler(((externalTask, externalTaskService) -> {

            String content = (String) externalTask.getVariable("content");
            System.out.println("Sorry this has been rejected: " + content);
            Map<String, Object> variables = new HashMap<String, Object>();
            variables.put("notificationTimestamp", new Date());
            externalTaskService.complete(externalTask, variables);
        })).open();
    }
}
