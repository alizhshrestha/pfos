package com.pfos.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DeleteTransactionResponse(
        @JsonProperty("_id") String id,
        boolean deleted
) {
}