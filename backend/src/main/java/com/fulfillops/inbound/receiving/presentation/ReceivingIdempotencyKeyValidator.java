package com.fulfillops.inbound.receiving.presentation;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ReceivingIdempotencyKeyValidator {
    private static final Pattern VALID_KEY = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    public String validate(String rawKey) {
        if (rawKey == null) throw new ReceivingIdempotencyKeyRequiredException();
        if (!VALID_KEY.matcher(rawKey).matches()) throw new ReceivingIdempotencyKeyInvalidException();
        return rawKey;
    }
}
