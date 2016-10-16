package com.github.authme.configme.knownproperties;

import com.github.authme.configme.properties.Property;
import com.github.authme.configme.properties.StringProperty;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.github.authme.configme.TestUtils.containsAll;
import static com.github.authme.configme.TestUtils.verifyException;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link ConfigurationData}.
 */
public class ConfigurationDataTest {

    @Test
    public void shouldAcceptListWithTypedProperty() {
        // given
        List<Property<String>> properties = Arrays.asList(
            new StringProperty("test", "Test"),
            new StringProperty("taste", "Taste"),
            new StringProperty("toast", "Toaster"));

        // when
        ConfigurationData configData = new ConfigurationData(properties);

        // then
        assertThat(configData.getProperties(), containsAll((List) properties));
    }

    @Test
    public void shouldHaveImmutablePropertyList() {
        // given
        List<Property<?>> properties = Collections.singletonList(new StringProperty("test", ""));
        ConfigurationData configData = new ConfigurationData(properties);

        // when / then
        verifyException(() -> configData.getProperties().remove(0), UnsupportedOperationException.class);
    }
}
