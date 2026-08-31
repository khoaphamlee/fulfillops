package com.fulfillops.sku.presentation;

import com.fulfillops.sku.application.SkuService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/skus")
public class SkuController {

    private final SkuService skuService;

    public SkuController(SkuService skuService) {
        this.skuService = skuService;
    }

    @PostMapping
    public ResponseEntity<SkuResponse> create(
            @PathVariable UUID tenantId,
            @Valid @RequestBody CreateSkuRequest request) {
        SkuResponse response = skuService.create(tenantId, request);
        URI location = URI.create("/api/v1/tenants/" + tenantId + "/skus/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{skuId}")
    public SkuResponse getById(@PathVariable UUID tenantId, @PathVariable UUID skuId) {
        return skuService.getById(tenantId, skuId);
    }
}
