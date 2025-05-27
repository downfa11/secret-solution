package com.ns.secrets.command.domain;

import lombok.*;

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
