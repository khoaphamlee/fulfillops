package com.fulfillops.inbound.receiving.presentation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
public record CreateReceivingReceiptRequest(@NotEmpty List<@Valid CreateReceivingReceiptLineRequest> lines) {}
