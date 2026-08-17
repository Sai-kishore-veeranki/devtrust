package com.vsk.devtrust.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/slow")
public class SlowAPI {
    @GetMapping()
    public String slowOne(){
        return "sai kishore veeranki";
    }
}
