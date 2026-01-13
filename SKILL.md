---
name: java-dependency-downloader
description: Automatically download and extract Java/Maven project dependency source code. Use this skill when a user wants to extract third-party dependency sources from a Maven project for analysis, debugging, or code review. This skill handles Maven detection, dependency tree parsing, source jar downloads, extraction to a target directory, and optional decompilation of binary jars. Trigger phrases include "extract dependency sources", "download dependency sources", "get third-party library sources", "extract maven dependencies", "decompile dependencies", or similar requests for Java/Maven project dependency extraction.
---

# Java Dependency Downloader

Extract and download source code for all Maven project dependencies.

## Quick Start

**Basic usage (extract source jars only):**
```
Extract dependency sources from /path/to/project
```

**With decompilation (for dependencies without sources):**
```
Extract dependency sources from /path/to/project using java-decompiler.jar
```

**Custom output directory:**
```
Extract dependency sources to LIBS folder
```

## Required Parameters

The skill requires these inputs, which should be gathered from the user:

1. **Project directory** - Path to Maven project (must contain pom.xml)
2. **Java-decompiler path** (optional) - Path to java-decompiler.jar for decompiling binaries
3. **Output directory** (optional, default: "THIRD") - Target folder name for extracted sources

## Workflow

### Step 1: Gather Required Information

If the user doesn't provide the project path, ask:
```
What is the path to the Maven project? (The directory should contain pom.xml)
```

If decompilation is needed and no decompiler path is provided, ask:
```
What is the path to java-decompiler.jar? (Leave empty to skip decompilation)
```

For custom output directory:
```
What directory should sources be extracted to? (Default: THIRD)
```

### Step 2: Execute the Script

Run the extraction script with gathered parameters:

```bash
python scripts/extract_dependencies.py <project_dir> [-d decompiler_path] [-o output_dir]
```

**Parameters:**
- `project_dir` - Required: Path to Maven project
- `-d, --decompiler` - Optional: Path to java-decompiler.jar
- `-o, --output` - Optional: Output directory name (default: THIRD)

**Environment variables:**
- `MAVEN_HOME` - Maven installation directory (auto-detected if not set)
- `THIRD_DIR` - Output directory (overrides -o flag)

### Step 3: Interpret Results

The script will output:
- Total number of dependencies found
- Count of source jars extracted
- Count of decompiled jars (if decompiler provided)
- Count of skipped/failed dependencies
- Full path to output directory

**Example output:**
```
============================================================
Maven 依赖提取工具
============================================================
项目目录: /path/to/project
输出目录: THIRD
Maven 命令: mvn
============================================================

解析依赖树: /path/to/project/pom.xml
✓ 跳过项目本身: com.example:my-project:1.0.0
✓ 找到 24 个依赖

下载源码包...
✓ 源码包下载完成

处理依赖...

[1/24] 处理: org.springframework:spring-core:5.3.20
  ✓ 找到源码包: spring-core-5.3.20-sources.jar
  ✓ 源码已解压到: THIRD/spring-core

...

============================================================
提取完成！
============================================================
总计依赖: 24
源码已解压: 20
反编译成功: 3
跳过: 0
失败: 1
输出目录: /path/to/project/THIRD
============================================================
```

## Error Handling

### Common Errors and Solutions

**Error: "未找到 pom.xml"**
- Cause: Invalid project directory
- Solution: Verify the path contains pom.xml and try again

**Error: "未找到 Maven"**
- Cause: Maven not installed or not in PATH
- Solution: See [TROUBLESHOOTING.md](references/TROUBLESHOOTING.md)

**Error: "解析依赖树失败"**
- Cause: Maven build failures or corrupted pom.xml
- Solution: Run `mvn clean` first, check pom.xml syntax

**Error: "反编译失败"**
- Cause: Missing Java or invalid decompiler path
- Solution: Verify Java 8+ is installed and decompiler path is correct

For detailed troubleshooting, see [TROUBLESHOOTING.md](references/TROUBLESHOOTING.md).

## How It Works

1. **Maven Detection** - Finds Maven command from MAVEN_HOME or PATH
2. **Dependency Parsing** - Runs `mvn dependency:tree` to get all dependencies
3. **Source Download** - Runs `mvn dependency:sources` to download source jars
4. **Artifact Location** - Finds jars in local Maven repository (~/.m2/repository)
5. **Smart Extraction** - For each dependency:
   - If source jar exists → Extract to output directory
   - If only binary jar exists → Decompile (if decompiler provided) → Extract
   - Otherwise → Skip with warning
6. **Statistics** - Reports summary of extraction results

## Output Directory Structure

```
THIRD/ (or custom name)
├── spring-core/
│   └── org/springframework/core/...
├── spring-boot/
│   └── org/springframework/boot/...
└── mysql-connector-j/
    └── com/mysql/cj/jdbc/...
```

Each dependency gets its own folder named after the artifact ID.

## Important Notes

- The script automatically skips the project's own artifact (first line of dependency tree)
- Only third-party dependencies are extracted, not project source code
- Decompiled code may have minor differences from original source
- Some dependencies may not have published source code
- Network connection required for downloading sources from Maven repositories

## Resources

### scripts/extract_dependencies.py

Main Python script that performs the dependency extraction. Includes:

- **MavenDetector** - Finds Maven installation
- **DependencyParser** - Parses dependency tree and downloads sources
- **ArtifactLocator** - Locates jars in Maven repository
- **DecompilerWrapper** - Decompiles binary jars
- **Extractor** - Extracts jar files
- **MavenDependencyExtractor** - Main orchestrator

The script is executed directly by Claude and handles all extraction logic.

### references/TROUBLESHOOTING.md

Detailed troubleshooting guide for common issues including:
- Maven installation and configuration
- Java version requirements
- Network and repository issues
- Decompiler setup
- Permission problems

Load this reference when encountering errors or when users need detailed setup instructions.
