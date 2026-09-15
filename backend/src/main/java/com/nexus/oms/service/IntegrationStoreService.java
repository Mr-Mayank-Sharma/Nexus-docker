package com.nexus.oms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.oms.dto.IntegrationStoreRequest;
import com.nexus.oms.dto.StoreSyncStatus;
import com.nexus.oms.entity.*;
import com.nexus.oms.exception.BadRequestException;
import com.nexus.oms.exception.ResourceNotFoundException;
import com.nexus.oms.integration.core.CredentialVault;
import com.nexus.oms.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IntegrationStoreService {

    private static final Logger log = LoggerFactory.getLogger(IntegrationStoreService.class);

    private final NxIntegrationStoreRepository storeRepository;
    private final NxIntegrationStoreSettingRepository settingRepository;
    private final NxIntegrationSyncConfigRepository syncConfigRepository;
    private final NxSyncLogRepository syncLogRepository;
    private final ObjectMapper objectMapper;
    private final CredentialVault credentialVault;

    public IntegrationStoreService(NxIntegrationStoreRepository storeRepository,
                                    NxIntegrationStoreSettingRepository settingRepository,
                                    NxIntegrationSyncConfigRepository syncConfigRepository,
                                    NxSyncLogRepository syncLogRepository,
                                    ObjectMapper objectMapper,
                                    CredentialVault credentialVault) {
        this.storeRepository = storeRepository;
        this.settingRepository = settingRepository;
        this.syncConfigRepository = syncConfigRepository;
        this.syncLogRepository = syncLogRepository;
        this.objectMapper = objectMapper;
        this.credentialVault = credentialVault;
    }

    @Cacheable(value = "storeSettings", key = "'stores:' + #tenantId + ':' + (#platform ?: 'all')")
    public List<NxIntegrationStore> getStores(UUID tenantId, String platform) {
        if (platform != null && !platform.isBlank()) {
            return storeRepository.findByTenantIdAndPlatform(tenantId, platform);
        }
        return storeRepository.findByTenantId(tenantId);
    }

    public NxIntegrationStore getStore(UUID id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IntegrationStore", id));
    }

    public Optional<NxIntegrationStore> findStoreByExternalDomain(String domain) {
        return storeRepository.findByExternalDomain(domain);
    }

    @Transactional
    @CacheEvict(value = "storeSettings", allEntries = true)
    public NxIntegrationStore createStore(UUID tenantId, IntegrationStoreRequest request) {
        if (storeRepository.findByTenantIdAndStoreCode(tenantId, request.getStoreCode()).isPresent()) {
            throw new BadRequestException("Store code already exists: " + request.getStoreCode());
        }

        NxIntegrationStore store = NxIntegrationStore.builder()
                .tenantId(tenantId)
                .storeCode(request.getStoreCode())
                .storeName(request.getStoreName())
                .platform(request.getPlatform().toUpperCase())
                .platformType(request.getPlatformType())
                .currency(request.getCurrency())
                .defaultLocale(request.getDefaultLocale())
                .timezone(request.getTimezone())
                .externalStoreId(request.getExternalStoreId())
                .externalDomain(request.getExternalDomain())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        store = storeRepository.save(store);

        if (request.getSettings() != null) {
            for (Map.Entry<String, String> entry : request.getSettings().entrySet()) {
                boolean encrypted = entry.getKey().contains("token") || entry.getKey().contains("secret");
                NxIntegrationStoreSetting setting = NxIntegrationStoreSetting.builder()
                        .storeId(store.getId())
                        .settingType(entry.getKey())
                        .settingValue(encryptIfRequired(entry.getValue(), encrypted))
                        .isEncrypted(encrypted)
                        .build();
                settingRepository.save(setting);
            }
        }

        String[] defaultSyncs = {"ORDER_IMPORT", "PRODUCT_SYNC", "INVENTORY_PUSH", "FULFILLMENT_PUSH", "REFUND_PUSH"};
        for (String syncType : defaultSyncs) {
            NxIntegrationSyncConfig syncConfig = NxIntegrationSyncConfig.builder()
                    .storeId(store.getId())
                    .syncType(syncType)
                    .enabled(true)
                    .intervalMinutes(15)
                    .build();
            syncConfigRepository.save(syncConfig);
        }

        return store;
    }

    @Transactional
    @CacheEvict(value = "storeSettings", allEntries = true)
    public NxIntegrationStore updateStore(UUID id, IntegrationStoreRequest request) {
        NxIntegrationStore store = getStore(id);
        if (request.getStoreName() != null) store.setStoreName(request.getStoreName());
        if (request.getCurrency() != null) store.setCurrency(request.getCurrency());
        if (request.getDefaultLocale() != null) store.setDefaultLocale(request.getDefaultLocale());
        if (request.getTimezone() != null) store.setTimezone(request.getTimezone());
        if (request.getExternalStoreId() != null) store.setExternalStoreId(request.getExternalStoreId());
        if (request.getExternalDomain() != null) store.setExternalDomain(request.getExternalDomain());
        if (request.getIsActive() != null) store.setIsActive(request.getIsActive());
        store = storeRepository.save(store);

        if (request.getSettings() != null) {
            for (Map.Entry<String, String> entry : request.getSettings().entrySet()) {
                boolean encrypted = entry.getKey().contains("token") || entry.getKey().contains("secret");
                NxIntegrationStoreSetting setting = settingRepository
                        .findByStoreIdAndSettingType(store.getId(), entry.getKey())
                        .orElse(NxIntegrationStoreSetting.builder()
                                .storeId(store.getId())
                                .settingType(entry.getKey())
                                .isEncrypted(encrypted)
                                .build());
                setting.setIsEncrypted(encrypted);
                setting.setSettingValue(encryptIfRequired(entry.getValue(), encrypted));
                settingRepository.save(setting);
            }
        }
        return store;
    }

    @Transactional
    @CacheEvict(value = "storeSettings", allEntries = true)
    public void deleteStore(UUID id) {
        NxIntegrationStore store = getStore(id);
        settingRepository.deleteByStoreId(id);
        storeRepository.delete(store);
    }

    @Cacheable(value = "storeSettings", key = "'settings:' + #storeId")
    public List<NxIntegrationStoreSetting> getSettings(UUID storeId) {
        // Return detached copies with decrypted values so we never mutate (and
        // risk dirty-flushing plaintext back onto) JPA-managed entities.
        return settingRepository.findByStoreId(storeId).stream()
                .map(s -> NxIntegrationStoreSetting.builder()
                        .id(s.getId())
                        .storeId(s.getStoreId())
                        .settingType(s.getSettingType())
                        .settingValue(decryptIfRequired(s.getSettingValue(), s.getIsEncrypted()))
                        .description(s.getDescription())
                        .isEncrypted(s.getIsEncrypted())
                        .createdAt(s.getCreatedAt())
                        .updatedAt(s.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Cacheable(value = "storeSettings", key = "'setting:' + #storeId + ':' + #settingType")
    public String getSetting(UUID storeId, String settingType) {
        return settingRepository.findByStoreIdAndSettingType(storeId, settingType)
                .map(s -> decryptIfRequired(s.getSettingValue(), s.getIsEncrypted()))
                .orElse(null);
    }

    @Cacheable(value = "storeSettings", key = "'settingsMap:' + #storeId")
    public Map<String, String> getSettingsMap(UUID storeId) {
        return settingRepository.findByStoreId(storeId).stream()
                .collect(Collectors.toMap(NxIntegrationStoreSetting::getSettingType,
                        s -> {
                            String value = decryptIfRequired(s.getSettingValue(), s.getIsEncrypted());
                            return value != null ? value : "";
                        }));
    }

    @Transactional
    @CacheEvict(value = "storeSettings", allEntries = true)
    public void updateSetting(UUID storeId, String settingType, String value) {
        NxIntegrationStoreSetting setting = settingRepository
                .findByStoreIdAndSettingType(storeId, settingType)
                .orElse(NxIntegrationStoreSetting.builder()
                        .storeId(storeId)
                        .settingType(settingType)
                        .isEncrypted(settingType.contains("token") || settingType.contains("secret"))
                        .build());
        setting.setIsEncrypted(settingType.contains("token") || settingType.contains("secret"));
        setting.setSettingValue(encryptIfRequired(value, setting.getIsEncrypted()));
        settingRepository.save(setting);
    }

    public StoreSyncStatus getSyncStatus(UUID storeId) {
        NxIntegrationStore store = getStore(storeId);
        List<NxIntegrationSyncConfig> syncConfigs = syncConfigRepository.findByStoreId(storeId);

        StoreSyncStatus status = StoreSyncStatus.builder()
                .storeId(store.getId())
                .storeCode(store.getStoreCode())
                .storeName(store.getStoreName())
                .platform(store.getPlatform())
                .connected(store.getIsActive())
                .syncTypes(syncConfigs.stream().map(sc -> StoreSyncStatus.SyncTypeStatus.builder()
                        .syncType(sc.getSyncType())
                        .enabled(sc.getEnabled())
                        .intervalMinutes(sc.getIntervalMinutes())
                        .lastSyncAt(sc.getLastSyncAt())
                        .lastSyncStatus(sc.getLastSyncStatus())
                        .lastSyncMessage(sc.getLastSyncMessage())
                        .build()).collect(Collectors.toList()))
                .build();
        return status;
    }

    // ── Encryption helpers ──────────────────────────────────────────────────

    private String encryptIfRequired(String value, Boolean isEncrypted) {
        if (Boolean.TRUE.equals(isEncrypted) && value != null && !value.isBlank()) {
            return credentialVault.encrypt(value);
        }
        return value;
    }

    private String decryptIfRequired(String value, Boolean isEncrypted) {
        if (Boolean.TRUE.equals(isEncrypted) && value != null && !value.isBlank()) {
            try {
                return credentialVault.decrypt(value);
            } catch (RuntimeException e) {
                // Legacy rows were flagged isEncrypted=true but stored as plaintext
                // (encryption was never applied). Return as-is; it self-heals on next write.
                log.warn("Failed to decrypt store setting; treating as legacy plaintext");
                return value;
            }
        }
        return value;
    }

    // ── Sync logs ───────────────────────────────────────────────────────────

    public List<NxSyncLog> getSyncLogs(UUID storeId, int limit) {
        NxIntegrationStore store = getStore(storeId);
        String integrationType = store.getPlatform().toUpperCase() + "_" + store.getStoreCode();
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return syncLogRepository
                .findByTenantIdAndIntegrationTypeOrderByCreatedAtDesc(
                        store.getTenantId(), integrationType, PageRequest.of(0, safeLimit))
                .getContent();
    }
}
