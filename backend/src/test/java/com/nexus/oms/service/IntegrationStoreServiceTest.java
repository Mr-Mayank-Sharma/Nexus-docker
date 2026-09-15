package com.nexus.oms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.oms.dto.IntegrationStoreRequest;
import com.nexus.oms.entity.NxIntegrationStore;
import com.nexus.oms.entity.NxIntegrationStoreSetting;
import com.nexus.oms.integration.core.CredentialVault;
import com.nexus.oms.repository.NxIntegrationStoreRepository;
import com.nexus.oms.repository.NxIntegrationStoreSettingRepository;
import com.nexus.oms.repository.NxIntegrationSyncConfigRepository;
import com.nexus.oms.repository.NxSyncLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntegrationStoreServiceTest {

    @Mock private NxIntegrationStoreRepository storeRepository;
    @Mock private NxIntegrationStoreSettingRepository settingRepository;
    @Mock private NxIntegrationSyncConfigRepository syncConfigRepository;
    @Mock private NxSyncLogRepository syncLogRepository;

    private IntegrationStoreService service;
    private CredentialVault credentialVault;

    @BeforeEach
    void setUp() {
        // Real vault (fallback key) so we exercise the actual AES/GCM round-trip.
        credentialVault = new CredentialVault();
        service = new IntegrationStoreService(storeRepository, settingRepository,
                syncConfigRepository, syncLogRepository, new ObjectMapper(), credentialVault);
    }

    @Test
    void createStore_encryptsTokenAndSecret_butLeavesNonSensitivePlain() {
        UUID tenantId = UUID.randomUUID();
        when(storeRepository.findByTenantIdAndStoreCode(any(), any())).thenReturn(Optional.empty());
        when(storeRepository.save(any())).thenAnswer(inv -> {
            NxIntegrationStore s = inv.getArgument(0);
            if (s.getId() == null) s.setId(UUID.randomUUID());
            return s;
        });
        when(settingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IntegrationStoreRequest req = new IntegrationStoreRequest();
        req.setStoreCode("shop1");
        req.setStoreName("Shop 1");
        req.setPlatform("shopify");
        req.setSettings(Map.of(
                "shop_domain", "shop1.myshopify.com",
                "access_token", "shpat_secret_token",
                "client_secret", "super-secret"));

        service.createStore(tenantId, req);

        ArgumentCaptor<NxIntegrationStoreSetting> captor =
                ArgumentCaptor.forClass(NxIntegrationStoreSetting.class);
        verify(settingRepository, times(3)).save(captor.capture());
        Map<String, NxIntegrationStoreSetting> saved = captor.getAllValues().stream()
                .collect(java.util.stream.Collectors.toMap(
                        NxIntegrationStoreSetting::getSettingType, s -> s));

        // Non-sensitive value stored as-is
        assertEquals("shop1.myshopify.com", saved.get("shop_domain").getSettingValue());
        assertEquals(false, saved.get("shop_domain").getIsEncrypted());

        // Sensitive values encrypted (not equal to plaintext) and flagged
        NxIntegrationStoreSetting token = saved.get("access_token");
        assertEquals(true, token.getIsEncrypted());
        assertNotEquals("shpat_secret_token", token.getSettingValue());
        assertEquals("shpat_secret_token", credentialVault.decrypt(token.getSettingValue()));

        NxIntegrationStoreSetting secret = saved.get("client_secret");
        assertEquals(true, secret.getIsEncrypted());
        assertNotEquals("super-secret", secret.getSettingValue());
        assertEquals("super-secret", credentialVault.decrypt(secret.getSettingValue()));
    }

    @Test
    void getSetting_decryptsEncryptedValue() {
        UUID storeId = UUID.randomUUID();
        String ciphertext = credentialVault.encrypt("shpat_abc");
        NxIntegrationStoreSetting setting = NxIntegrationStoreSetting.builder()
                .storeId(storeId)
                .settingType("access_token")
                .settingValue(ciphertext)
                .isEncrypted(true)
                .build();
        when(settingRepository.findByStoreIdAndSettingType(storeId, "access_token"))
                .thenReturn(Optional.of(setting));

        assertEquals("shpat_abc", service.getSetting(storeId, "access_token"));
    }

    @Test
    void getSetting_legacyPlaintextWithEncryptedFlag_fallsBackGracefully() {
        UUID storeId = UUID.randomUUID();
        NxIntegrationStoreSetting legacy = NxIntegrationStoreSetting.builder()
                .storeId(storeId)
                .settingType("access_token")
                .settingValue("plain-legacy-token")   // never actually encrypted
                .isEncrypted(true)
                .build();
        when(settingRepository.findByStoreIdAndSettingType(storeId, "access_token"))
                .thenReturn(Optional.of(legacy));

        // Must not throw — returns the raw legacy value
        assertEquals("plain-legacy-token", service.getSetting(storeId, "access_token"));
    }

    @Test
    void getSettings_decryptsValues_andDoesNotMutateSourceEntities() {
        UUID storeId = UUID.randomUUID();
        String ciphertext = credentialVault.encrypt("tok");
        NxIntegrationStoreSetting token = NxIntegrationStoreSetting.builder()
                .storeId(storeId).settingType("access_token")
                .settingValue(ciphertext).isEncrypted(true).build();
        when(settingRepository.findByStoreId(storeId)).thenReturn(List.of(token));

        List<NxIntegrationStoreSetting> result = service.getSettings(storeId);

        assertEquals("tok", result.get(0).getSettingValue());
        // Source entity must remain encrypted (no dirty-flush of plaintext)
        assertEquals(ciphertext, token.getSettingValue());
    }

    @Test
    void getSettingsMap_decryptsEncryptedAndKeepsPlain() {
        UUID storeId = UUID.randomUUID();
        NxIntegrationStoreSetting domain = NxIntegrationStoreSetting.builder()
                .storeId(storeId).settingType("shop_domain")
                .settingValue("shop.myshopify.com").isEncrypted(false).build();
        NxIntegrationStoreSetting token = NxIntegrationStoreSetting.builder()
                .storeId(storeId).settingType("access_token")
                .settingValue(credentialVault.encrypt("tok")).isEncrypted(true).build();
        when(settingRepository.findByStoreId(storeId)).thenReturn(List.of(domain, token));

        Map<String, String> map = service.getSettingsMap(storeId);

        assertEquals("shop.myshopify.com", map.get("shop_domain"));
        assertEquals("tok", map.get("access_token"));
    }

    @Test
    void updateSetting_encryptsSensitiveValue() {
        UUID storeId = UUID.randomUUID();
        when(settingRepository.findByStoreIdAndSettingType(storeId, "access_token"))
                .thenReturn(Optional.empty());
        when(settingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateSetting(storeId, "access_token", "new-token");

        ArgumentCaptor<NxIntegrationStoreSetting> captor =
                ArgumentCaptor.forClass(NxIntegrationStoreSetting.class);
        verify(settingRepository).save(captor.capture());
        NxIntegrationStoreSetting saved = captor.getValue();
        assertEquals(true, saved.getIsEncrypted());
        assertNotEquals("new-token", saved.getSettingValue());
        assertEquals("new-token", credentialVault.decrypt(saved.getSettingValue()));
    }
}
