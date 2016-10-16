package com.github.authme.configme.samples;

import com.github.authme.configme.Comment;
import com.github.authme.configme.SectionComments;
import com.github.authme.configme.SettingsHolder;
import com.github.authme.configme.properties.Property;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.authme.configme.properties.PropertyInitializer.newListProperty;
import static com.github.authme.configme.properties.PropertyInitializer.newProperty;


/**
 * Sample properties for testing purposes.
 */
public final class TestConfiguration implements SettingsHolder {

    @Comment("Duration in seconds")
    public static final Property<Integer> DURATION_IN_SECONDS =
        newProperty("test.duration", 4);

    @Comment("The system name")
    public static final Property<String> SYSTEM_NAME =
        newProperty("test.systemName", "[TestDefaultValue]");

    // No comment
    public static final Property<TestEnum> RATIO_ORDER =
        newProperty(TestEnum.class, "sample.ratio.order", TestEnum.SECOND);

    // No comment
    public static final Property<List<String>> RATIO_FIELDS =
        newListProperty("sample.ratio.fields", "a", "b", "c");

    @Comment({
        "The version number",
        "This is just a random number" })
    public static final Property<Integer> VERSION_NUMBER =
        newProperty("version", 32046);

    @Comment("Skip boring features?")
    public static final Property<Boolean> SKIP_BORING_FEATURES =
        newProperty("features.boring.skip", false);

    @Comment("Add some boring colors here (gray, beige, ...)")
    public static final Property<List<String>> BORING_COLORS =
        newListProperty("features.boring.colors");

    // No comment
    public static final Property<Integer> DUST_LEVEL =
        newProperty("features.boring.dustLevel", -1);

    @Comment("Enable cool features?")
    public static final Property<Boolean> USE_COOL_FEATURES =
        newProperty("features.cool.enabled", false);

    @Comment("List of cool options to use")
    public static final Property<List<String>> COOL_OPTIONS =
        newListProperty("features.cool.options", "Sparks", "Sprinkles");


    private TestConfiguration() {
    }

    @SectionComments
    public static Map<String, String[]> getComments() {
        Map<String, String[]> comments = new HashMap<>();
        comments.put("sample", new String[]{"Sample section"});
        comments.put("features.cool", new String[]{"Cool features", "Contains cool settings"});
        comments.put("features.boring", new String[]{"Plain boring features"});
        comments.put("test", new String[]{"Test section"});
        return comments;
    }
}
