package org.solid.testharness.config;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.rdf4j.model.IRI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solid.testharness.utils.TestHarnessInitializationException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.eclipse.rdf4j.model.util.Values.iri;

@ApplicationScoped
public class Config {
    private static final Logger logger = LoggerFactory.getLogger(Config.class);

    private IRI testSubject;
    private URL configUrl;
    private URL testSuiteDescriptionFile;
    private File credentialsDirectory;
    private File outputDir;

    // the settings are taken in the following order of preference:
    //   system property
    //   env variable
    //   .env file in cwd
    //   config/application.yaml (local)
    //   src/main/resources/application.yaml (project)
    // @seeAlso: https://quarkus.io/guides/config-reference#configuration_sources

    @ConfigProperty(name = "target")
    Optional<String> target;
    @ConfigProperty(name = "configFile")
    Optional<String> configFile;
    @ConfigProperty(name = "testSuiteDescription")
    Optional<String> testSuiteDescription;
    @ConfigProperty(name = "credentialsDir")
    Optional<String> credentialsPath;

    @Inject
    PathMappings pathMappings;

    public IRI getTestSubject() {
        if (testSubject == null && target.isPresent()) {
            testSubject = iri(target.get());
        }
        return testSubject;
    }

    public void setTestSubject(IRI testSubject) {
        this.testSubject = testSubject;
    }

    public URL getConfigUrl() {
        if (configUrl == null) {
            try {
                configUrl = Path.of(configFile.get()).toAbsolutePath().normalize().toUri().toURL();
            } catch (Exception e) {
                throw new TestHarnessInitializationException("configFile config is not a valid file or URL: %s", e.toString());
            }
        }
        return configUrl;
    }

    public void setConfigUrl(URL configUrl) {
        this.configUrl = configUrl;
    }

    public URL getTestSuiteDescription()  {
        if (testSuiteDescriptionFile == null) {
            try {
                testSuiteDescriptionFile = Path.of(testSuiteDescription.get()).toAbsolutePath().normalize().toUri().toURL();
            } catch (Exception e) {
                throw new TestHarnessInitializationException("testSuiteDescription config is not a valid file or URL: %s", e.toString());
            }
        }
        return testSuiteDescriptionFile;
    }

    public void setTestSuiteDescription(URL testSuiteDescriptionFile) {
        this.testSuiteDescriptionFile = testSuiteDescriptionFile;
    }

    public File getCredentialsDirectory() {
        if (credentialsDirectory == null) {
            try {
                credentialsDirectory = Path.of(credentialsPath.get()).toFile().getCanonicalFile();
            } catch (Exception e) {
                throw new TestHarnessInitializationException("credentialsDir config is not a valid file: %s", e.toString());
            }
        }
        return credentialsDirectory;
    }

    public File getOutputDirectory() {
        return outputDir;
    }

    public void setOutputDirectory(File outputDir) {
        this.outputDir = outputDir;
    }

    public List<PathMappings.Mapping> getPathMappings() {
        return pathMappings.getMappings();
    }

    public void logConfigSettings() {
        logger.info("Config url:       {}", getConfigUrl().toString());
        logger.info("Credentials path: {}", getCredentialsDirectory().getPath());
        logger.info("Test suite:       {}", getTestSuiteDescription().toString());
        logger.info("Path mappings:    {}", getPathMappings());
        logger.info("Target server:    {}", target.orElse("not defined"));
    }
}