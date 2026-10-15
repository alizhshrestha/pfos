package com.pfos.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentMethodResponse(
        @JsonProperty("_id") String id,
        String name,
        Boolean isActive
) {
}