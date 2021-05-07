package org.solid.testharness2.config;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;
import org.solid.testharness.config.Config;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
@TestProfile(ConfigTestBlankProfile.class)
public class ConfigMissingTest {
    @Inject
    Config config;

    @Test
    void getTestSubject() {
        assertNull(config.getTestSubject());
    }

    @Test
    void getConfigUrl() {
        assertThrows(TestHarnessInitializationException.class, () -> config.getConfigUrl());
    }

    @Test
    void getTestSuiteDescription() {
        assertThrows(TestHarnessInitializationException.class, () -> config.getTestSuiteDescription());
    }

    @Test
    void getCredentialsDirectory() {
        assertThrows(TestHarnessInitializationException.class, () -> config.getCredentialsDirectory());
    }

    @Test
    void logConfigSettings() {
        assertThrows(TestHarnessInitializationException.class, () -> config.logConfigSettings());
    }
}