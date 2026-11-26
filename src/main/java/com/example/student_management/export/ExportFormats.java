package com.example.student_management.export;

import com.example.student_management.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Every {@link ExportFormatFactory} bean, looked up by its {@link ExportFormatFactory#extension()}. */
@Component
public class ExportFormats {
    private final Map<String, ExportFormatFactory> byExtension;

    public ExportFormats(List<ExportFormatFactory> factories) {
        this.byExtension = factories.stream()
                .collect(Collectors.toMap(ExportFormatFactory::extension, Function.identity(), (a, b) -> {
                    throw new IllegalStateException("Two export formats share the extension '" + a.extension() + "'");
                }, TreeMap::new));
    }

    /** @throws ResourceNotFoundException if no format has that extension; the message lists the ones that exist */
    public ExportFormatFactory get(String extension) {
        ExportFormatFactory factory = byExtension.get(extension);
        if (factory == null) {
            throw new ResourceNotFoundException("No export format '" + extension + "'. Available: " + String.join(", ", extensions()));
        }
        return factory;
    }

    /** Sorted. */
    public Set<String> extensions() {
        return byExtension.keySet();
    }
}
