package com.ns.secrets.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SecretRequest {
    private String key;
    private String value;
    // getters, setters
}
