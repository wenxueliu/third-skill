module com.example.mavenextractor {
    // Logging
    requires org.slf4j;
    requires ch.qos.logback.classic;

    // Apache Commons Compress
    requires org.apache.commons.compress;

    // Picocli
    requires info.picocli;

    // XML processing (built-in)
    requires java.xml;

    // Export main package
    exports com.example.mavenextractor;

    // Export subpackages
    exports com.example.mavenextractor.config;
    exports com.example.mavenextractor.model;
    exports com.example.mavenextractor.detector;
    exports com.example.mavenextractor.parser;
    exports com.example.mavenextractor.locator;
    exports com.example.mavenextractor.decompiler;
    exports com.example.mavenextractor.extractor;
    exports com.example.mavenextractor.util;
}
