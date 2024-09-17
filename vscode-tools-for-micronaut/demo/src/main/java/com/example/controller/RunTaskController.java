package com.example.controller;

import com.example.jobs.Job;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import jakarta.inject.Inject;

@Controller("/runtask")
public class RunTaskController {

    // The scheduled task singleton
    @Inject
    Job job;

    @Get(produces = "text/plain")
    public String activateTask() {
        job.unpause();
        return "Task activated.";
    }

}
