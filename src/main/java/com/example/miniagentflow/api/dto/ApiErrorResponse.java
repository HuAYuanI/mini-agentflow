package com.example.miniagentflow.api.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiErrorResponse {

    private String code;
    private String message;
    private Instant timestamp;
}
