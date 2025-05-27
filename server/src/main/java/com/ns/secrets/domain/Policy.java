package com.ns.secrets.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Policy {
    private String id;
    private String version;
    private List<Statement> statement;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class Statement {
        private String effect;
        private List<String> actions;
        private List<String> resources;
    }
}
