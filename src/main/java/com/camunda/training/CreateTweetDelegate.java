package com.camunda.training;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CreateTweetDelegate implements JavaDelegate {

    TwitterService twitterService;

    @Autowired
    public CreateTweetDelegate(TwitterService twitterService){
        this.twitterService = twitterService;
    }

    private final Logger LOGGER = LoggerFactory.getLogger(CreateTweetDelegate.class.getName());

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String content = (String) execution.getVariable("content");
        LOGGER.info("Publishing Tweet: " + content);
        long id = twitterService.tweet(content);
        execution.setVariable("id", id);
    }
}
