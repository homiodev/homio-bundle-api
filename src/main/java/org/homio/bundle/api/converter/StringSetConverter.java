package org.homio.bundle.api.converter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.apache.commons.lang3.StringUtils;

@Converter
public class StringSetConverter implements AttributeConverter<Set<String>, String> {
    @Override
    public String convertToDatabaseColumn(Set<String> set) {
        return set == null ? "" : set.stream().collect(Collectors.joining("~~~"));
    }

    @Override
    public Set<String> convertToEntityAttribute(String data) {
        return StringUtils.isEmpty(data) ? new HashSet<>() : new HashSet<>(Arrays.asList(data.split("~~~")));
    }
}
