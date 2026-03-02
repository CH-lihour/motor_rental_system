package com.example.motor_retal.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class DashboardController {
    @GetMapping("/")
    public String index() {
        return "index";
    }
    
}
