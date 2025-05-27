package com.ns.secrets.command.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SecretRequest {
    private String key;
    private String value;
    // getters, setters
}
