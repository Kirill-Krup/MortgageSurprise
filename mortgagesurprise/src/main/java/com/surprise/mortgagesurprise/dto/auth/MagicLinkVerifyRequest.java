package com.surprise.mortgagesurprise.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MagicLinkVerifyRequest(
        @NotBlank @Size(max = 128) String token
) {
}
