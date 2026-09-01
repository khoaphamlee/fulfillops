package com.fulfillops.inbound.receiving.application;

import com.fulfillops.inbound.receiving.presentation.CreateReceivingReceiptLineRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ReceivingRequestFingerprint {
    public String calculate(UUID tenantId, UUID shipmentId, List<CreateReceivingReceiptLineRequest> lines) {
        StringBuilder canonical = new StringBuilder("receiving-receipt:v1\n")
                .append("tenant=").append(tenantId).append('\n')
                .append("shipment=").append(shipmentId).append('\n');
        lines.stream()
                .sorted(Comparator.comparing(line -> line.inboundShipmentLineId().toString()))
                .forEach(line -> canonical.append("line=").append(line.inboundShipmentLineId()).append(':')
                        .append(line.receivedQuantity()).append('\n'));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime.", exception);
        }
    }
}
