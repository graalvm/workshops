package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;


@SpringBootApplication
@RestController
public class DemoApplication {

	public static void main(String[] args) {
	    SpringApplication.run(DemoApplication.class, args);
	}

    @RequestMapping(method = RequestMethod.GET, path = "/ping")
    ResponseEntity<Pong> ping() {
        Pong pong = new Pong("hi!");
	    return ResponseEntity.ok(pong);
    }
}
