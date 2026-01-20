# Maven Dependency Extractor - Java Implementation

A modern Java (JDK 21) implementation of the Maven dependency extractor tool. Extracts Maven project dependencies and their source code, with support for decompiling binary JARs when sources are unavailable.

## Features

- 🔍 Automatic Maven detection (MAVEN_HOME or PATH)
- 📦 Dependency tree parsing using `mvn dependency:tree`
- 📥 Automatic source JAR download via `mvn dependency:sources`
- 🔧 Binary JAR decompilation using Fernflower (java-decompiler.jar)
- 📊 Progress tracking and statistics
- 🚀 Modern JDK 21 features (Records, Pattern Matching, Switch Expressions, etc.)
- 📦 Standalone executable JAR with all dependencies included

## Requirements

- JDK 21 or higher
- Maven 3.6+ (for the target project)
- Optional: java-decompiler.jar (Fernflower) for decompilation

## Building

```bash
cd tools/mavenextractor
mvn clean package
```

This will create `target/maven-dependency-extractor-1.0.0.jar` - a fat JAR with all dependencies included.

## Usage

### Basic Usage (extract sources only)

```bash
java -jar target/maven-dependency-extractor-1.0.0.jar /path/to/maven/project
```

### Use Decompiler

```bash
java -jar target/maven-dependency-extractor-1.0.0.jar /path/to/maven/project -d java-decompiler.jar
```

### Specify Output Directory

```bash
java -jar target/maven-dependency-extractor-1.0.0.jar /path/to/maven/project -o THIRD_LIBS
```

### Command Line Options

```
Usage: maven-dependency-extractor [-d=<decompiler>] [-o=<dir>] PROJECT_DIR

      PROJECT_DIR              Maven project directory (containing pom.xml)

  -d, --decompiler=<DECOMPILER>
                              Path to Java decompiler (java-decompiler.jar)
  -o, --output=<DIR>          Output directory (default: THIRD)

Common Options:
  -h, --help                  Show this help message and exit
  -V, --version               Print version information and exit
```

### Environment Variables

- `MAVEN_HOME` - Maven installation directory
- `THIRD_DIR` - Output directory (default: THIRD)

## Project Structure

```
mavenextractor/
├── pom.xml                                    # Maven build configuration
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── module-info.java               # JPMS module descriptor
│   │   │   └── com/example/mavenextractor/
│   │   │       ├── Main.java                  # CLI entry point
│   │   │       ├── MavenDependencyExtractor.java  # Main orchestrator
│   │   │       ├── config/
│   │   │       │   └── Config.java            # Configuration
│   │   │       ├── model/
│   │   │       │   ├── Dependency.java        # Record for Maven dependency
│   │   │       │   ├── ArtifactLocation.java  # Record for JAR locations
│   │   │       │   └── ExtractionStats.java   # Record for statistics
│   │   │       ├── detector/
│   │   │       │   └── MavenDetector.java     # Maven detection
│   │   │       ├── parser/
│   │   │       │   └── DependencyParser.java  # Dependency parsing
│   │   │       ├── locator/
│   │   │       │   └── ArtifactLocator.java   # JAR file location
│   │   │       ├── extractor/
│   │   │       │   └── Extractor.java         # Archive extraction
│   │   │       ├── decompiler/
│   │   │       │   └── DecompilerWrapper.java # Decompiler wrapper
│   │   │       └── util/
│   │   │           └── ProcessExecutor.java   # Process execution utility
│   │   └── resources/
│   │       └── logback.xml                    # Logging configuration
```

## JDK 21 Features Used

- **Records** - Immutable data classes (Dependency, ArtifactLocation, ExtractionStats)
- **Pattern Matching for instanceof** - Simplified type checking
- **Switch Expressions** - Concise conditional logic
- **Text Blocks** - Multi-line strings for help messages
- **Enhanced Pseudo-Random Number Generators** - Not directly used but available
- **Sealed Classes** - Could be used for result types
- **Stream API Improvements** - Functional processing of dependencies
- **java.nio.file API** - Modern file operations

## Architecture

### Components

1. **Config** - Configuration management with environment variable support
2. **MavenDetector** - Detects Maven from MAVEN_HOME or PATH
3. **DependencyParser** - Parses dependency trees and pom.xml
4. **ArtifactLocator** - Locates JAR files in Maven repository
5. **Extractor** - Extracts ZIP/JAR/TAR.GZ archives
6. **DecompilerWrapper** - Wraps java-decompiler.jar (Fernflower)
7. **MavenDependencyExtractor** - Main orchestration class
8. **Main** - CLI entry point with picocli

### Process Flow

1. Detect Maven installation
2. Parse dependency tree (using `mvn dependency:tree` or pom.xml)
3. Download source JARs (using `mvn dependency:sources`)
4. For each dependency:
   - Locate binary and source JARs in local Maven repository
   - If source JAR exists: extract it
   - Else if binary JAR exists: decompile it (if decompiler available)
   - Else: skip
5. Output statistics

## Logging

The tool uses SLF4J with Logback for logging. Logs are written to:
- Console (stdout) with colored output
- File: `maven-dependency-extractor.log`

Log levels:
- INFO: Main progress and statistics
- DEBUG: Detailed processing information
- WARN: Warnings (e.g., missing decompiler)
- ERROR: Errors that prevent operation

## Comparison with Python Version

| Feature | Python Version | Java Version |
|---------|---------------|--------------|
| Type Safety | Runtime | Compile-time |
| Performance | Good | Better for large projects |
| Dependencies | Python stdlib | SLF4J, Logback, Commons Compress, picocli |
| Distribution | Single .py file | Fat JAR with all dependencies |
| Cross-platform | Yes | Yes (native Java) |
| Modularity | Classes | JPMS modules |
| Maintenance | Python 3.x | JDK 21+ |

## Troubleshooting

### Maven not found
Ensure Maven is installed and either:
- Set `MAVEN_HOME` environment variable to Maven installation directory
- Add Maven `bin/` directory to PATH

### Decompilation fails
Ensure `java-decompiler.jar` (Fernflower) is available and specify its path with `-d` option.

### OutOfMemoryError
Increase JVM heap size:
```bash
java -Xmx2g -jar maven-dependency-extractor-1.0.0.jar /path/to/project
```

### Permission denied
Ensure the JAR file has execute permissions or run with `java -jar`.

## License

This project maintains the same license as the parent project.

## Authors

Converted from Python original to modern Java JDK 21 implementation.
