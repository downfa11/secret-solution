package com.ns.secrets.command.domain.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SecretResponse {
    private String key;
    private String value;
    // getters, setters
}