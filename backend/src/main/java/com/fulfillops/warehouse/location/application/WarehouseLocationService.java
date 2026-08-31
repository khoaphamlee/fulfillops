package com.fulfillops.warehouse.location.application;

import com.fulfillops.warehouse.application.WarehouseService;
import com.fulfillops.warehouse.location.domain.WarehouseAisle;
import com.fulfillops.warehouse.location.domain.WarehouseBin;
import com.fulfillops.warehouse.location.domain.WarehouseRack;
import com.fulfillops.warehouse.location.domain.WarehouseZone;
import com.fulfillops.warehouse.location.infrastructure.WarehouseAisleRepository;
import com.fulfillops.warehouse.location.infrastructure.WarehouseBinRepository;
import com.fulfillops.warehouse.location.infrastructure.WarehouseRackRepository;
import com.fulfillops.warehouse.location.infrastructure.WarehouseZoneRepository;
import com.fulfillops.warehouse.location.presentation.CreateWarehouseAisleRequest;
import com.fulfillops.warehouse.location.presentation.CreateWarehouseBinRequest;
import com.fulfillops.warehouse.location.presentation.CreateWarehouseRackRequest;
import com.fulfillops.warehouse.location.presentation.CreateWarehouseZoneRequest;
import com.fulfillops.warehouse.location.presentation.WarehouseAisleResponse;
import com.fulfillops.warehouse.location.presentation.WarehouseBinResponse;
import com.fulfillops.warehouse.location.presentation.WarehouseRackResponse;
import com.fulfillops.warehouse.location.presentation.WarehouseZoneResponse;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WarehouseLocationService {

    private static final String ZONE_UNIQUE = "uk_warehouse_zones_warehouse_code";
    private static final String AISLE_UNIQUE = "uk_warehouse_aisles_zone_code";
    private static final String RACK_UNIQUE = "uk_warehouse_racks_aisle_code";
    private static final String BIN_UNIQUE = "uk_warehouse_bins_rack_code";

    private final WarehouseService warehouseService;
    private final WarehouseZoneRepository zoneRepository;
    private final WarehouseAisleRepository aisleRepository;
    private final WarehouseRackRepository rackRepository;
    private final WarehouseBinRepository binRepository;

    public WarehouseLocationService(
            WarehouseService warehouseService,
            WarehouseZoneRepository zoneRepository,
            WarehouseAisleRepository aisleRepository,
            WarehouseRackRepository rackRepository,
            WarehouseBinRepository binRepository) {
        this.warehouseService = warehouseService;
        this.zoneRepository = zoneRepository;
        this.aisleRepository = aisleRepository;
        this.rackRepository = rackRepository;
        this.binRepository = binRepository;
    }

    @Transactional
    public WarehouseZoneResponse createZone(UUID tenantId, UUID warehouseId, CreateWarehouseZoneRequest request) {
        warehouseService.requireExistingWarehouse(tenantId, warehouseId);
        WarehouseZone zone = WarehouseZone.create(warehouseId, request.code());
        try {
            return toResponse(zoneRepository.saveAndFlush(zone));
        } catch (DataIntegrityViolationException exception) {
            if (isUniqueViolation(exception, ZONE_UNIQUE)) throw new WarehouseZoneCodeAlreadyExistsException();
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public WarehouseZoneResponse getZone(UUID tenantId, UUID warehouseId, UUID zoneId) {
        warehouseService.requireExistingWarehouse(tenantId, warehouseId);
        return toResponse(findZoneInWarehouse(warehouseId, zoneId));
    }

    @Transactional
    public WarehouseAisleResponse createAisle(UUID tenantId, UUID warehouseId, UUID zoneId, CreateWarehouseAisleRequest request) {
        warehouseService.requireExistingWarehouse(tenantId, warehouseId);
        findZoneInWarehouse(warehouseId, zoneId);
        WarehouseAisle aisle = WarehouseAisle.create(zoneId, request.code());
        try {
            return toResponse(aisleRepository.saveAndFlush(aisle));
        } catch (DataIntegrityViolationException exception) {
            if (isUniqueViolation(exception, AISLE_UNIQUE)) throw new WarehouseAisleCodeAlreadyExistsException();
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public WarehouseAisleResponse getAisle(UUID tenantId, UUID warehouseId, UUID zoneId, UUID aisleId) {
        warehouseService.requireExistingWarehouse(tenantId, warehouseId);
        findZoneInWarehouse(warehouseId, zoneId);
        return toResponse(aisleRepository.findByIdAndZoneId(aisleId, zoneId)
                .orElseThrow(WarehouseAisleNotFoundException::new));
    }

    @Transactional
    public WarehouseRackResponse createRack(UUID tenantId, UUID warehouseId, UUID aisleId, CreateWarehouseRackRequest request) {
        warehouseService.requireExistingWarehouse(tenantId, warehouseId);
        findAisleInWarehouse(warehouseId, aisleId);
        WarehouseRack rack = WarehouseRack.create(aisleId, request.code());
        try {
            return toResponse(rackRepository.saveAndFlush(rack));
        } catch (DataIntegrityViolationException exception) {
            if (isUniqueViolation(exception, RACK_UNIQUE)) throw new WarehouseRackCodeAlreadyExistsException();
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public WarehouseRackResponse getRack(UUID tenantId, UUID warehouseId, UUID aisleId, UUID rackId) {
        warehouseService.requireExistingWarehouse(tenantId, warehouseId);
        WarehouseRack rack = rackRepository.findByIdAndAisleId(rackId, aisleId)
                .orElseThrow(WarehouseRackNotFoundException::new);
        if (!aisleBelongsToWarehouse(warehouseId, aisleId)) throw new WarehouseRackNotFoundException();
        return toResponse(rack);
    }

    @Transactional
    public WarehouseBinResponse createBin(UUID tenantId, UUID warehouseId, UUID rackId, CreateWarehouseBinRequest request) {
        warehouseService.requireExistingWarehouse(tenantId, warehouseId);
        findRackInWarehouse(warehouseId, rackId);
        WarehouseBin bin = WarehouseBin.create(rackId, request.code());
        try {
            return toResponse(binRepository.saveAndFlush(bin));
        } catch (DataIntegrityViolationException exception) {
            if (isUniqueViolation(exception, BIN_UNIQUE)) throw new WarehouseBinCodeAlreadyExistsException();
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public WarehouseBinResponse getBin(UUID tenantId, UUID warehouseId, UUID rackId, UUID binId) {
        warehouseService.requireExistingWarehouse(tenantId, warehouseId);
        WarehouseBin bin = binRepository.findByIdAndRackId(binId, rackId)
                .orElseThrow(WarehouseBinNotFoundException::new);
        if (!rackBelongsToWarehouse(warehouseId, rackId)) throw new WarehouseBinNotFoundException();
        return toResponse(bin);
    }

    private WarehouseZone findZoneInWarehouse(UUID warehouseId, UUID zoneId) {
        return zoneRepository.findByIdAndWarehouseId(zoneId, warehouseId)
                .orElseThrow(WarehouseZoneNotFoundException::new);
    }

    private WarehouseAisle findAisleInWarehouse(UUID warehouseId, UUID aisleId) {
        WarehouseAisle aisle = aisleRepository.findById(aisleId).orElseThrow(WarehouseAisleNotFoundException::new);
        if (!zoneRepository.findByIdAndWarehouseId(aisle.getZoneId(), warehouseId).isPresent()) {
            throw new WarehouseAisleNotFoundException();
        }
        return aisle;
    }

    private WarehouseRack findRackInWarehouse(UUID warehouseId, UUID rackId) {
        WarehouseRack rack = rackRepository.findById(rackId).orElseThrow(WarehouseRackNotFoundException::new);
        if (!aisleBelongsToWarehouse(warehouseId, rack.getAisleId())) throw new WarehouseRackNotFoundException();
        return rack;
    }

    private boolean aisleBelongsToWarehouse(UUID warehouseId, UUID aisleId) {
        return aisleRepository.findById(aisleId)
                .map(aisle -> zoneRepository.findByIdAndWarehouseId(aisle.getZoneId(), warehouseId).isPresent())
                .orElse(false);
    }

    private boolean rackBelongsToWarehouse(UUID warehouseId, UUID rackId) {
        return rackRepository.findById(rackId)
                .map(rack -> aisleBelongsToWarehouse(warehouseId, rack.getAisleId()))
                .orElse(false);
    }

    private boolean isUniqueViolation(DataIntegrityViolationException exception, String constraintName) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
                    && violation.getKind() == ConstraintKind.UNIQUE
                    && constraintName.equals(violation.getConstraintName())) return true;
            cause = cause.getCause();
        }
        return false;
    }

    private WarehouseZoneResponse toResponse(WarehouseZone zone) { return new WarehouseZoneResponse(zone.getId(), zone.getWarehouseId(), zone.getCode(), zone.getCreatedAt()); }
    private WarehouseAisleResponse toResponse(WarehouseAisle aisle) { return new WarehouseAisleResponse(aisle.getId(), aisle.getZoneId(), aisle.getCode(), aisle.getCreatedAt()); }
    private WarehouseRackResponse toResponse(WarehouseRack rack) { return new WarehouseRackResponse(rack.getId(), rack.getAisleId(), rack.getCode(), rack.getCreatedAt()); }
    private WarehouseBinResponse toResponse(WarehouseBin bin) { return new WarehouseBinResponse(bin.getId(), bin.getRackId(), bin.getCode(), bin.getCreatedAt()); }
}
