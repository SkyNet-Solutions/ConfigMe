package ch.jalu.configme.beanmapper.leafproperties;

import ch.jalu.configme.beanmapper.BeanPropertyDescription;
import ch.jalu.configme.beanmapper.ConfigMeMapperException;
import ch.jalu.configme.properties.BeanProperty;
import ch.jalu.configme.properties.Property;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Generates {@link Property} objects for all "leaf" values of a bean for the export of beans by
 * the {@link ch.jalu.configme.resource.PropertyResource}.
 */
public class LeafPropertiesGenerator {

    /**
     * Generates a list of regular property objects for the given bean's data.
     *
     * @param beanProperty the bean property
     * @param value the value of the bean property
     * @param <B> the bean type
     * @return list of all properties necessary to export the bean
     */
    public <B> List<Property<?>> generate(BeanProperty<B> beanProperty, B value) {
        return new EntryBuilder(beanProperty)
            .collectPropertiesFromBean(value, beanProperty.getPath());
    }

    protected static final class EntryBuilder {
        private final BeanProperty<?> beanProperty;
        private final List<Property<?>> properties = new ArrayList<>();

        EntryBuilder(BeanProperty beanProperty) {
            this.beanProperty = beanProperty;
        }

        /**
         * Processes a bean class and handles all of its writable properties. Throws an exception
         * for non-beans or classes with no writable properties.
         *
         * @param bean the bean to process
         * @param path the path of the bean in the config structure
         * @return list of all properties necessary to export the bean
         */
        protected List<Property<?>> collectPropertiesFromBean(Object bean, String path) {
            Collection<BeanPropertyDescription> writableProperties =
                beanProperty.getWritableProperties(bean.getClass());
            if (writableProperties.isEmpty()) {
                throw new ConfigMeMapperException("Class '" + bean.getClass() + "' has no writable properties");
            }
            String prefix = path.isEmpty() ? "" : (path + ".");
            for (BeanPropertyDescription property : writableProperties) {
                collectPropertyEntries(property.getValue(bean), prefix + property.getName());
            }
            return properties;
        }

        /**
         * Creates property entries for the provided value, recursively for beans.
         *
         * @param value the value to process
         * @param path the path of the value in the config structure
         */
        protected void collectPropertyEntries(Object value, String path) {
            Property<?> property = createConstantProperty(value, path);
            if (property != null) {
                properties.add(property);
            } else if (value instanceof Collection<?>) {
                handleCollection((Collection<?>) value, path);
            } else if (value instanceof Map<?, ?>) {
                if (((Map) value).isEmpty()) {
                    properties.add(new ConstantValueProperty<>(path, Collections.emptyMap()));
                } else {
                    for (Map.Entry<String, ?> entry : ((Map<String, ?>) value).entrySet()) {
                        collectPropertyEntries(entry.getValue(), path + "." + entry.getKey());
                    }
                }
            } else {
                Objects.requireNonNull(value);
                // At this point it can only be a bean; the bean method checks that the class is a bean
                // and throws an exception otherwise, so we can just delegate the value to that method
                collectPropertiesFromBean(value, path);
            }
        }

        @Nullable
        protected ConstantValueProperty<?> createConstantProperty(Object value, String path) {
            if (value instanceof String || value instanceof Enum<?>
                || value instanceof Number || value instanceof Boolean) {
                return new ConstantValueProperty<>(path, value);
            }
            return null;
        }

        /**
         * Handles a value that is of {@link Collection} type.
         *
         * @param value the collection
         * @param path the path of the collection in the config structure
         */
        protected void handleCollection(Collection<?> value, String path) {
            properties.add(new ConstantValueProperty<>(path, value));
        }
    }
}
