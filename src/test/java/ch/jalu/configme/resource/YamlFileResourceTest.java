package ch.jalu.configme.resource;

import ch.jalu.configme.SettingsManager;
import ch.jalu.configme.TestUtils;
import ch.jalu.configme.beanmapper.command.CommandConfig;
import ch.jalu.configme.beanmapper.worldgroup.GameMode;
import ch.jalu.configme.beanmapper.worldgroup.Group;
import ch.jalu.configme.beanmapper.worldgroup.WorldGroupConfig;
import ch.jalu.configme.configurationdata.ConfigurationData;
import ch.jalu.configme.configurationdata.ConfigurationDataBuilder;
import ch.jalu.configme.exception.ConfigMeException;
import ch.jalu.configme.migration.PlainMigrationService;
import ch.jalu.configme.properties.BeanProperty;
import ch.jalu.configme.properties.OptionalProperty;
import ch.jalu.configme.properties.Property;
import ch.jalu.configme.samples.TestConfiguration;
import ch.jalu.configme.samples.TestEnum;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ch.jalu.configme.TestUtils.getJarPath;
import static ch.jalu.configme.TestUtils.verifyException;
import static ch.jalu.configme.properties.PropertyInitializer.newProperty;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Test for {@link YamlFileResource} and {@link YamlFileReader}.
 */
public class YamlFileResourceTest {

    private static final String COMPLETE_FILE = "/config-sample.yml";
    private static final String INCOMPLETE_FILE = "/config-incomplete-sample.yml";
    private static final String DIFFICULT_FILE = "/config-difficult-values.yml";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldThrowForAbsentYamlMap() throws IOException {
        // given
        File file = temporaryFolder.newFile();
        Files.write(file.toPath(), "123".getBytes());

        // when / then
        verifyException(() -> new YamlFileResource(file),
            ConfigMeException.class, "Top-level is not a map");
    }

    @Test
    public void shouldWrapIOException() throws IOException {
        // given
        File folder = temporaryFolder.newFolder();
        File file = new File(folder, "test");

        // when / then
        verifyException(() -> new YamlFileResource(file),
            ConfigMeException.class, "Could not read file");
    }

    @Test
    public void shouldReadAllProperties() {
        // given
        File config = copyFileFromResources(COMPLETE_FILE);

        // when
        PropertyResource resource = new YamlFileResource(config);

        // then
        Map<Property<?>, Object> expected = new HashMap<>();
        expected.put(TestConfiguration.DURATION_IN_SECONDS, 22);
        expected.put(TestConfiguration.SYSTEM_NAME, "Custom sys name");
        expected.put(TestConfiguration.RATIO_ORDER, TestEnum.FIRST);
        expected.put(TestConfiguration.RATIO_FIELDS, Arrays.asList("Australia", "Burundi", "Colombia"));
        expected.put(TestConfiguration.VERSION_NUMBER, 2492);
        expected.put(TestConfiguration.SKIP_BORING_FEATURES, false);
        expected.put(TestConfiguration.BORING_COLORS, Arrays.asList("beige", "gray"));
        expected.put(TestConfiguration.DUST_LEVEL, 2);
        expected.put(TestConfiguration.USE_COOL_FEATURES, true);
        expected.put(TestConfiguration.COOL_OPTIONS, Arrays.asList("Dinosaurs", "Explosions", "Big trucks"));

        for (Map.Entry<Property<?>, Object> entry : expected.entrySet()) {
            assertThat("Property '" + entry.getKey().getPath() + "' has expected value",
                entry.getKey().getValue(resource), equalTo(entry.getValue()));
        }
    }

    @Test
    public void shouldWriteMissingProperties() {
        // given
        File file = copyFileFromResources(INCOMPLETE_FILE);
        YamlFileResource resource = new YamlFileResource(file);
        ConfigurationData configurationData = ConfigurationDataBuilder.collectData(TestConfiguration.class);

        // when
        resource.exportProperties(configurationData);

        // then
        // Load file again to make sure what we wrote can be read again
        resource = new YamlFileResource(file);
        Map<Property<?>, Object> expected = new HashMap<>();
        expected.put(TestConfiguration.DURATION_IN_SECONDS, 22);
        expected.put(TestConfiguration.SYSTEM_NAME, "[TestDefaultValue]");
        expected.put(TestConfiguration.RATIO_ORDER, "SECOND");
        expected.put(TestConfiguration.RATIO_FIELDS, Arrays.asList("Australia", "Burundi", "Colombia"));
        expected.put(TestConfiguration.VERSION_NUMBER, 32046);
        expected.put(TestConfiguration.SKIP_BORING_FEATURES, false);
        expected.put(TestConfiguration.BORING_COLORS, Collections.EMPTY_LIST);
        expected.put(TestConfiguration.DUST_LEVEL, -1);
        expected.put(TestConfiguration.USE_COOL_FEATURES, false);
        expected.put(TestConfiguration.COOL_OPTIONS, Arrays.asList("Dinosaurs", "Explosions", "Big trucks"));
        for (Map.Entry<Property<?>, Object> entry : expected.entrySet()) {
            // Check with resource#getObject to make sure the values were persisted to the file
            // If we go through Property objects they may fall back to their default values
            String propertyPath = entry.getKey().getPath();
            assertThat("Property '" + propertyPath + "' has expected value",
                resource.getObject(propertyPath), equalTo(entry.getValue()));
        }
    }

    /** Verifies that "difficult cases" such as apostrophes in strings etc. are handled properly. */
    @Test
    public void shouldProperlyExportAnyValues() {
        // given
        File file = copyFileFromResources(DIFFICULT_FILE);
        YamlFileResource resource = new YamlFileResource(file);

        // Properties
        List<Property<?>> properties = new ArrayList<>(Arrays.asList(
            newProperty("more.string1", "it's a text with some \\'apostrophes'"),
            newProperty("more.string2", "\tthis one\nhas some\nnew '' lines-test")));
        properties.addAll(ConfigurationDataBuilder.collectData(TestConfiguration.class).getProperties());
        ConfigurationData configData = new ConfigurationData(properties);

        // when
        new SettingsManager(resource, new PlainMigrationService(), configData);
        // Save and load again
        resource.exportProperties(configData);
        resource.reload();

        // then
        assertThat(resource.getObject(TestConfiguration.DUST_LEVEL.getPath()), not(nullValue()));

        Map<Property<?>, Object> expected = new HashMap<>();
        expected.put(TestConfiguration.DURATION_IN_SECONDS, 20);
        expected.put(TestConfiguration.SYSTEM_NAME, "A 'test' name");
        expected.put(TestConfiguration.RATIO_ORDER, "FOURTH");
        expected.put(TestConfiguration.RATIO_FIELDS, Arrays.asList("Australia\\", "\tBurundi'", "Colombia?\n''"));
        expected.put(TestConfiguration.VERSION_NUMBER, -1337);
        expected.put(TestConfiguration.SKIP_BORING_FEATURES, false);
        expected.put(TestConfiguration.BORING_COLORS, Arrays.asList("it's a difficult string!", "gray\nwith new lines\n"));
        expected.put(TestConfiguration.DUST_LEVEL, -1);
        expected.put(TestConfiguration.USE_COOL_FEATURES, true);
        expected.put(TestConfiguration.COOL_OPTIONS, Collections.EMPTY_LIST);
        expected.put(properties.get(0), properties.get(0).getDefaultValue());
        expected.put(properties.get(1), properties.get(1).getDefaultValue());

        for (Map.Entry<Property<?>, Object> entry : expected.entrySet()) {
            assertThat("Property '" + entry.getKey().getPath() + "' has expected value",
                resource.getObject(entry.getKey().getPath()), equalTo(entry.getValue()));
        }
    }

    @Test
    public void shouldReloadValues() throws IOException {
        // given
        File file = temporaryFolder.newFile();
        YamlFileResource resource = new YamlFileResource(file);
        Files.copy(getJarPath(COMPLETE_FILE), file.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // when
        assertThat(TestConfiguration.RATIO_ORDER.getValue(resource), equalTo(TestEnum.SECOND)); // default value
        resource.reload();

        // then
        assertThat(TestConfiguration.RATIO_ORDER.getValue(resource), equalTo(TestEnum.FIRST));
    }

    @Test
    public void shouldRetrieveTypedValues() {
        // given
        File file = copyFileFromResources(COMPLETE_FILE);
        YamlFileResource resource = new YamlFileResource(file);

        // when / then
        assertThat(resource.getBoolean(TestConfiguration.DURATION_IN_SECONDS.getPath()), nullValue());
        assertThat(resource.getString(TestConfiguration.DURATION_IN_SECONDS.getPath()), nullValue());
        assertThat(resource.getDouble(TestConfiguration.DURATION_IN_SECONDS.getPath()), equalTo(22.0));
        assertThat(resource.getDouble(TestConfiguration.SKIP_BORING_FEATURES.getPath()), nullValue());
    }

    @Test
    public void shouldSetValuesButNotPersist() {
        // given
        File file = copyFileFromResources(INCOMPLETE_FILE);
        YamlFileResource resource = new YamlFileResource(file);

        // when
        assertThat(TestConfiguration.RATIO_ORDER.getValue(resource), equalTo(TestEnum.SECOND)); // default value
        resource.setValue(TestConfiguration.RATIO_ORDER.getPath(), TestEnum.THIRD);
        resource.setValue(TestConfiguration.SKIP_BORING_FEATURES.getPath(), true);

        // then
        assertThat(TestConfiguration.RATIO_ORDER.getValue(resource), equalTo(TestEnum.THIRD));
        assertThat(TestConfiguration.SKIP_BORING_FEATURES.getValue(resource), equalTo(true));

        // when (2) - reload without saving, so will fallback to default again
        resource.reload();

        // then
        assertThat(TestConfiguration.RATIO_ORDER.getValue(resource), equalTo(TestEnum.SECOND));
        assertThat(TestConfiguration.SKIP_BORING_FEATURES.getValue(resource), equalTo(false));
    }

    @Test
    public void shouldReturnIfResourceContainsValue() {
        // given
        File file = copyFileFromResources(INCOMPLETE_FILE);
        PropertyResource resource = new YamlFileResource(file);

        // when
        boolean presentPropertyResult = resource.contains(TestConfiguration.DURATION_IN_SECONDS.getPath());
        boolean absentPropertyResult = resource.contains(TestConfiguration.SKIP_BORING_FEATURES.getPath());

        // then
        assertThat(presentPropertyResult, equalTo(true));
        assertThat(absentPropertyResult, equalTo(false));
    }

    @Test
    public void shouldWrapIoExceptionInConfigMeException() throws IOException {
        // given
        File file = copyFileFromResources(INCOMPLETE_FILE);
        PropertyResource resource = new YamlFileResource(file);
        file.delete();
        // Hacky: the only way we can easily provoke an IOException is by deleting the file and creating a folder
        // with the same name...
        temporaryFolder.newFolder(file.getName());

        // when / then
        try {
            resource.exportProperties(ConfigurationDataBuilder.collectData(TestConfiguration.class));
            fail("Expected ConfigMeException to be thrown");
        } catch (ConfigMeException e) {
            assertThat(e.getCause(), instanceOf(IOException.class));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldReturnRootForEmptyString() throws IOException {
        // given
        File file = copyFileFromResources(COMPLETE_FILE);
        PropertyResource resource = new YamlFileResource(file);

        // when
        Object result = resource.getObject("");

        // then
        assertThat(result, instanceOf(Map.class));
        assertThat(((Map<String, ?>) result).keySet(), containsInAnyOrder("test", "sample", "version", "features"));
    }

    @Test
    public void shouldExportConfigurationWithExpectedComments() throws IOException {
        // given
        File file = copyFileFromResources(COMPLETE_FILE);
        PropertyResource resource = new YamlFileResource(file);
        ConfigurationData configurationData = ConfigurationDataBuilder.collectData(TestConfiguration.class);

        // when
        resource.exportProperties(configurationData);

        // then
        // The IDE likes manipulating the whitespace in the expected file. As long as it's handled outside of an IDE
        // this test should be fine.
        assertThat(Files.readAllLines(file.toPath()),
            equalTo(Files.readAllLines(getJarPath("/config-export-expected.yml"))));
    }

    @Test
    public void shouldSkipAbsentOptionalProperty() throws IOException {
        // given
        ConfigurationData configurationData = new ConfigurationData(Arrays.asList(
            new OptionalProperty<>(TestConfiguration.DURATION_IN_SECONDS),
            new OptionalProperty<>(TestConfiguration.RATIO_ORDER)));
        File file = copyFileFromResources(INCOMPLETE_FILE);
        PropertyResource resource = new YamlFileResource(file);

        // when
        resource.exportProperties(configurationData);

        // then
        List<String> exportedLines = Files.readAllLines(file.toPath());
        assertThat(exportedLines, contains(
            "",
            "test:",
            "    duration: 22"
        ));
    }

    @Test
    public void shouldExportAllPresentOptionalProperties() throws IOException {
        // given
        ConfigurationData configurationData = new ConfigurationData(Arrays.asList(
            new OptionalProperty<>(TestConfiguration.DURATION_IN_SECONDS),
            new OptionalProperty<>(TestConfiguration.RATIO_ORDER)));
        File file = copyFileFromResources(COMPLETE_FILE);
        PropertyResource resource = new YamlFileResource(file);

        // when
        resource.exportProperties(configurationData);

        // then
        List<String> exportedLines = Files.readAllLines(file.toPath());
        assertThat(exportedLines, contains(
            "",
            "test:",
            "    duration: 22",
            "sample:",
            "    ratio:",
            "        order: 'FIRST'"
        ));
    }

    @Test
    public void shouldSetValueAfterLoadingEmptyFile() {
        // given
        String durationPath = "duration";
        int duration = 13;
        String headerPath = "text.sample.titles.header";
        String header = "Test header";

        File file = copyFileFromResources("/empty_file.yml");
        PropertyResource resource = new YamlFileResource(file);

        // when
        resource.setValue(durationPath, duration);
        resource.setValue(headerPath, header);

        // then
        assertThat(resource.getObject(durationPath), equalTo(duration));
        assertThat(resource.getObject(headerPath), equalTo(header));
    }

    @Test
    public void shouldSetBeanPropertyValueAtRoot() {
        // given
        // Custom WorldGroupConfig
        Group easyGroup = new Group();
        easyGroup.setDefaultGamemode(GameMode.CREATIVE);
        easyGroup.setWorlds(Arrays.asList("easy1", "easy2"));
        Group hardGroup = new Group();
        hardGroup.setDefaultGamemode(GameMode.SURVIVAL);
        hardGroup.setWorlds(Arrays.asList("hard1", "hard2"));

        Map<String, Group> groups = new HashMap<>();
        groups.put("easy", easyGroup);
        groups.put("hard", hardGroup);
        WorldGroupConfig worldGroupConfig = new WorldGroupConfig();
        worldGroupConfig.setGroups(groups);

        // Load resource with empty file
        File file = copyFileFromResources("/beanmapper/worlds.yml");
        PropertyResource resource = new YamlFileResource(file);

        // when
        resource.setValue("", worldGroupConfig);

        // then
        assertThat(resource.getObject(""), equalTo(worldGroupConfig));
    }

    @Test
    public void shouldThrowExceptionWhenSettingSubpathOfRootBean() {
        // given
        PropertyResource resource = new YamlFileResource(copyFileFromResources("/empty_file.yml"));
        resource.setValue("", new WorldGroupConfig());

        // when / then
        verifyException(
            () -> resource.setValue("some.path", 14),
            ConfigMeException.class,
            "The root path is a bean property");
    }

    @Test
    public void shouldClearOtherValuesWhenBeanAtRootIsSet() {
        // given
        PropertyResource resource = new YamlFileResource(copyFileFromResources("/beanmapper/commands_root_path.yml"));
        // assumption
        assertThat((Map<?, ?>) resource.getObject("commands.save"), aMapWithSize(2));

        CommandConfig newConfig = new CommandConfig();
        newConfig.setDuration(14);
        newConfig.setCommands(Collections.emptyMap());

        // when
        resource.setValue("", newConfig);

        // then
        assertThat(resource.getObject(""), equalTo(newConfig));
        assertThat(resource.getObject("commands.save"), nullValue());
    }

    @Test
    public void shouldReturnNullForUnknownPath() {
        // given
        File file = copyFileFromResources(COMPLETE_FILE);
        YamlFileResource resource = new YamlFileResource(file);

        // when / then
        assertThat(resource.getObject("sample.ratio.wrong.dunno"), nullValue());
        assertThat(resource.getObject(TestConfiguration.RATIO_ORDER.getPath() + ".child"), nullValue());
    }

    @Test
    public void shouldExportEmptyMap() throws IOException {
        // given
        CommandConfig config = new CommandConfig();
        config.setDuration(3);
        config.setCommands(Collections.emptyMap());

        File file = copyFileFromResources("/beanmapper/commands.yml");
        YamlFileResource resource = new YamlFileResource(file);
        resource.setValue("config", config);

        Property<CommandConfig> commandConfigProperty =
            new BeanProperty<>(CommandConfig.class, "config", new CommandConfig());

        // when
        resource.exportProperties(new ConfigurationData(Collections.singletonList(commandConfigProperty)));

        // then
        List<String> exportedLines = Files.readAllLines(file.toPath());
        assertThat(exportedLines, contains(
            "",
            "config:",
            "    commands: {}",
            "    duration: 3"
        ));
    }

    @Test
    public void shouldClearIntermediateValuesForNull() {
        // given
        File file = copyFileFromResources("/empty_file.yml");
        YamlFileResource resource = new YamlFileResource(file);
        resource.setValue("abc.def", 25);
        resource.setValue("abc.xyz", "Hi Peter");

        // when
        resource.setValue("abc.def.ghi.jjj", null);

        // then
        assertThat((Map<?, ?>) resource.getObject("abc.def"), anEmptyMap());
    }

    private File copyFileFromResources(String path) {
        return TestUtils.copyFileFromResources(path, temporaryFolder);
    }
}
