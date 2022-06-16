package com.salaboy.conferences.c4p.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ServiceInfo(String name,
                          String version,
                          String source,
                          String podId,
                          String podNamepsace,
                          String podNodeName) {
}
