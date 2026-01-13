# Troubleshooting Guide

## Maven Installation Issues

### Error: "未找到 Maven"

**Symptoms:**
- Script cannot locate Maven command
- Message: "✗ 未找到 Maven，请安装 Maven 或设置 MAVEN_HOME 环境变量"

**Solutions:**

1. **Install Maven:**
   - macOS: `brew install maven`
   - Ubuntu/Debian: `sudo apt-get install maven`
   - Windows: Download from https://maven.apache.org/download.cgi

2. **Set MAVEN_HOME environment variable:**
   ```bash
   # Linux/macOS - add to ~/.bashrc or ~/.zshrc
   export MAVEN_HOME=/path/to/maven
   export PATH=$MAVEN_HOME/bin:$PATH

   # Windows - set in Environment Variables
   MAVEN_HOME=C:\path\to\maven
   ```

3. **Verify installation:**
   ```bash
   mvn --version
   ```

### Maven Not Executable

**Symptoms:**
- Maven found but cannot execute
- Permission denied errors

**Solution:**
```bash
# Make Maven executable (macOS/Linux)
chmod +x $MAVEN_HOME/bin/mvn
```

## Java Installation Issues

### Error: "反编译失败" or "java: command not found"

**Symptoms:**
- Decompilation fails
- Java not found errors

**Solutions:**

1. **Install Java 8+:**
   - macOS: `brew install openjdk@11`
   - Ubuntu: `sudo apt-get install default-jdk`
   - Windows: Download from Oracle or AdoptOpenJDK

2. **Set JAVA_HOME:**
   ```bash
   # Linux/macOS
   export JAVA_HOME=/path/to/java
   export PATH=$JAVA_HOME/bin:$PATH

   # Windows
   JAVA_HOME=C:\Program Files\Java\jdk-11
   ```

3. **Verify installation:**
   ```bash
   java -version
   ```

## Network and Repository Issues

### Source Download Fails

**Symptoms:**
- "警告: 源码包下载失败"
- Timeout errors
- Connection refused

**Solutions:**

1. **Configure Maven mirror (China users):**
   ```xml
   <!-- In ~/.m2/settings.xml -->
   <mirrors>
     <mirror>
       <id>aliyun</id>
       <mirrorOf>central</mirrorOf>
       <name>Aliyun Maven</name>
       <url>https://maven.aliyun.com/repository/public</url>
     </mirror>
   </mirrors>
   ```

2. **Check network connection:**
   ```bash
   ping repo1.maven.org
   ```

3. **Manually download sources:**
   ```bash
   cd /path/to/project
   mvn dependency:sources -U
   ```

### SSL Certificate Errors

**Symptoms:**
- SSL handshake failures
- Certificate validation errors

**Solutions:**

1. **Import certificates to Java truststore:**
   ```bash
   # Export certificate from browser
   # Import to Java cacerts
   keytool -import -alias maven-repo -file cert.crt -keystore $JAVA_HOME/lib/security/cacerts
   ```

2. **Temporarily disable SSL verification (not recommended for production):**
   ```bash
   mvn -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true dependency:sources
   ```

## Project Configuration Issues

### Error: "未找到 pom.xml"

**Symptoms:**
- Script cannot find pom.xml
- "FileNotFoundError: 未找到 pom.xml"

**Solutions:**

1. **Verify project directory:**
   ```bash
   ls -la /path/to/project/pom.xml
   ```

2. **Check if you're in correct directory:**
   ```bash
   pwd  # Should show project root
   ```

3. **Ensure correct path is provided:**
   ```bash
   python scripts/extract_dependencies.py /correct/path/to/project
   ```

### Dependency Tree Parse Failure

**Symptoms:**
- "解析依赖树失败"
- Empty dependency list

**Solutions:**

1. **Validate pom.xml:**
   ```bash
   mvn validate
   ```

2. **Clean and rebuild:**
   ```bash
   mvn clean install
   ```

3. **Check for syntax errors in pom.xml:**
   - Validate XML structure
   - Check for unclosed tags
   - Verify dependency declarations

## Decompiler Issues

### Decompiler Path Invalid

**Symptoms:**
- Decompilation fails for all dependencies
- Path not found errors

**Solutions:**

1. **Verify decompiler file exists:**
   ```bash
   ls -la /path/to/java-decompiler.jar
   ```

2. **Use absolute path:**
   ```bash
   python scripts/extract_dependencies.py . -d /absolute/path/to/java-decompiler.jar
   ```

3. **Download java-decompiler:**
   - Extract from IntelliJ IDEA installation
   - Or use Fern Flower standalone

### Decompilation Produces Poor Results

**Symptoms:**
- Decompiled code has many errors
- Code structure looks incorrect

**Solutions:**

1. **Try to find original sources first:**
   ```bash
   mvn dependency:sources -U
   ```

2. **Use alternative decompiler:**
   - CFR: https://github.com/leibnitz27/cfr
   - Procyon: https://bitbucket.org/mstrobel/procyon/

3. **Accept limitations:**
   - Decompiled code may not perfectly match original
   - Some optimizations may obscure logic

## File System Issues

### Permission Denied

**Symptoms:**
- Cannot write to output directory
- Cannot read jar files

**Solutions:**

1. **Check directory permissions:**
   ```bash
   ls -ld THIRD/
   ```

2. **Fix permissions:**
   ```bash
   # Make directory writable
   chmod u+w THIRD/

   # For system Maven repository
   sudo chmod -R u+w ~/.m2/repository
   ```

3. **Use different output directory:**
   ```bash
   python scripts/extract_dependencies.py . -o /tmp/THIRD
   ```

### Disk Space Issues

**Symptoms:**
- No space left on device
- Extraction fails mid-process

**Solutions:**

1. **Check available space:**
   ```bash
   df -h
   ```

2. **Clean up if needed:**
   ```bash
   # Clean Maven cache
   rm -rf ~/.m2/repository

   # Clean output directory
   rm -rf THIRD/
   ```

3. **Use external drive:**
   ```bash
   python scripts/extract_dependencies.py . -o /external/drive/THIRD
   ```

## Python Environment Issues

### Python Version Incompatible

**Symptoms:**
- Syntax errors when running script
- Import errors

**Solutions:**

1. **Check Python version:**
   ```bash
   python --version  # Should be 3.6+
   ```

2. **Install Python 3:**
   - macOS: `brew install python3`
   - Ubuntu: `sudo apt-get install python3`

3. **Use python3 explicitly:**
   ```bash
   python3 scripts/extract_dependencies.py .
   ```

### Missing Python Dependencies

**Symptoms:**
- ModuleNotFoundError
- Import errors

**Solutions:**

The script uses only standard library modules, so no external dependencies should be needed. If you see import errors, ensure you're using a standard Python installation.

## Getting Help

If issues persist:

1. **Enable verbose output:**
   ```bash
   python scripts/extract_dependencies.py . --verbose
   ```

2. **Check Maven logs:**
   ```bash
   mvn dependency:tree -X
   ```

3. **Verify environment:**
   ```bash
   echo $MAVEN_HOME
   echo $JAVA_HOME
   mvn --version
   java -version
   python --version
   ```

4. **Consult documentation:**
   - Maven: https://maven.apache.org/guides/
   - Java Decompiler: https://github.com/fesh0r/fernflower
