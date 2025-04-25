package com.ns.secrets.controller;


import com.ns.secrets.service.SecretService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/secrets")
@RequiredArgsConstructor
public class SecretController {
    private final SecretService secretService;


}


