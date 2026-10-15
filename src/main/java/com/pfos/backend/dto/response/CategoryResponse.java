package com.pfos.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CategoryResponse(
        @JsonProperty("_id") String id,
        String name,
        Integer sortOrder,
        Boolean isActive
) {
}