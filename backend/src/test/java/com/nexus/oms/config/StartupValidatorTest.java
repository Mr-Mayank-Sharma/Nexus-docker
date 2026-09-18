package com.nexus.oms.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StartupValidatorTest {

    @Mock
    private ApplicationContext applicationContext;

    @Test
    void construction_succeedsWithValidConfig() {
        StartupValidator validator = new StartupValidator(applicationContext);
        assertNotNull(validator);
    }

    @Test
    void validate_withMissingJwt_marksFailed() {
        StartupValidator validator = new StartupValidator(applicationContext);

        validator.validate();

        verify(applicationContext, never()).getBean(anyString());
    }

    @Test
    void credentialKey_nullOrBlank_isInvalid() {
        assertFalse(StartupValidator.isCredentialKeyValid(null));
        assertFalse(StartupValidator.isCredentialKeyValid(""));
        assertFalse(StartupValidator.isCredentialKeyValid("   "));
    }

    @Test
    void credentialKey_shorterThan16_isInvalid() {
        assertFalse(StartupValidator.isCredentialKeyValid("short-key"));
        assertFalse(StartupValidator.isCredentialKeyValid("123456789012345"));
    }

    @Test
    void credentialKey_atLeast16_isValid() {
        assertTrue(StartupValidator.isCredentialKeyValid("1234567890123456"));
        assertTrue(StartupValidator.isCredentialKeyValid("a-strong-credential-encryption-key"));
    }
}
