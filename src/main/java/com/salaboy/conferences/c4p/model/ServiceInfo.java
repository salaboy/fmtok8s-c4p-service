package com.salaboy.conferences.c4p.model;

public record ServiceInfo(String name,
                          String version,
                          String source,
                          String podId,
                          String podNamepsace,
                          String podNodeName) {
}
