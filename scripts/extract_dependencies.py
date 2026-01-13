#!/usr/bin/env python3
"""
Maven 项目依赖提取工具
功能：
1. 检测 Maven 命令路径
2. 解析 pom.xml 获取依赖树
3. 下载源码包和二进制包
4. 解压源码包或反编译二进制包到指定目录
"""

import os
import sys
import json
import subprocess
import shutil
import zipfile
import tarfile
import re
from pathlib import Path
from typing import List, Dict, Optional, Tuple
from xml.etree import ElementTree as ET
from urllib.parse import urlparse


class Config:
    """配置类"""
    # 输出目录（可通过环境变量配置）
    THIRD_DIR = os.environ.get('THIRD_DIR', 'THIRD')
    # Maven 仓库路径
    MAVEN_REPO = os.path.expanduser('~/.m2/repository')
    # 本地缓存路径
    CACHE_DIR = '.maven_deps_cache'


class MavenDetector:
    """Maven 命令检测器"""

    @staticmethod
    def find_maven() -> Optional[str]:
        """
        查找 Maven 命令路径
        优先级：MAVEN_HOME 环境变量 > PATH 环境变量
        """
        # 1. 检查 MAVEN_HOME 环境变量
        maven_home = os.environ.get('MAVEN_HOME')
        if maven_home:
            maven_cmd = os.path.join(maven_home, 'bin', 'mvn')
            if os.name == 'nt':  # Windows
                maven_cmd += '.cmd'
            if MavenDetector._is_valid_maven(maven_cmd):
                print(f"✓ 找到 Maven (从 MAVEN_HOME): {maven_cmd}")
                return maven_cmd

        # 2. 检查 PATH 环境变量
        maven_cmd = 'mvn' if os.name != 'nt' else 'mvn.cmd'
        if MavenDetector._is_valid_maven(maven_cmd):
            result = subprocess.run(
                ['which', maven_cmd] if os.name != 'nt' else ['where', maven_cmd],
                capture_output=True,
                text=True
            )
            maven_path = result.stdout.strip().split('\n')[0]
            print(f"✓ 找到 Maven (从 PATH): {maven_path}")
            return maven_cmd

        print("✗ 未找到 Maven，请安装 Maven 或设置 MAVEN_HOME 环境变量")
        return None

    @staticmethod
    def _is_valid_maven(cmd: str) -> bool:
        """验证 Maven 命令是否可用"""
        try:
            result = subprocess.run(
                [cmd, '--version'],
                capture_output=True,
                text=True,
                timeout=10
            )
            return result.returncode == 0 and 'Apache Maven' in result.stdout
        except (subprocess.TimeoutExpired, FileNotFoundError):
            return False


class DependencyParser:
    """依赖解析器"""

    def __init__(self, maven_cmd: str, project_dir: str):
        self.maven_cmd = maven_cmd
        self.project_dir = project_dir
        self.pom_file = os.path.join(project_dir, 'pom.xml')

    def get_dependency_tree(self) -> List[Dict]:
        """
        获取依赖树
        使用 mvn dependency:tree 命令
        """
        if not os.path.exists(self.pom_file):
            raise FileNotFoundError(f"未找到 pom.xml: {self.pom_file}")

        print(f"\n解析依赖树: {self.pom_file}")

        # 执行 mvn dependency:tree
        cmd = [
            self.maven_cmd,
            'dependency:tree',
            '-DoutputFile=dependency-tree.txt',
            '-DappendOutput=false'
        ]

        result = subprocess.run(
            cmd,
            cwd=self.project_dir,
            capture_output=True,
            text=True
        )

        if result.returncode != 0:
            print(f"警告: Maven 命令执行失败: {result.stderr}")
            # 尝试备用方法：直接解析 pom.xml
            return self._parse_pom_directly()

        # 解析依赖树文件
        tree_file = os.path.join(self.project_dir, 'dependency-tree.txt')
        if os.path.exists(tree_file):
            dependencies = self._parse_dependency_tree(tree_file)
            os.remove(tree_file)
            return dependencies
        else:
            return self._parse_pom_directly()

    def _parse_dependency_tree(self, tree_file: str) -> List[Dict]:
        """解析 Maven 生成的依赖树文件"""
        dependencies = []
        seen = set()
        first_project_skipped = False

        with open(tree_file, 'r', encoding='utf-8') as f:
            for line in f:
                # 解析行: "INFO] com.example:artifact:jar:1.0.0:compile"
                match = re.search(r'([\w.-]+):([\w.-]+):jar:([\w.-]+)', line)
                if match:
                    group_id, artifact_id, version = match.groups()
                    key = f"{group_id}:{artifact_id}:{version}"

                    # 跳过第一行（项目本身）
                    if not first_project_skipped:
                        first_project_skipped = True
                        print(f"✓ 跳过项目本身: {key}")
                        continue

                    if key not in seen:
                        seen.add(key)
                        dependencies.append({
                            'groupId': group_id,
                            'artifactId': artifact_id,
                            'version': version,
                            'key': key
                        })

        print(f"✓ 找到 {len(dependencies)} 个依赖")
        return dependencies

    def _parse_pom_directly(self) -> List[Dict]:
        """直接解析 pom.xml 文件（备用方法）"""
        dependencies = []

        try:
            tree = ET.parse(self.pom_file)
            root = tree.getroot()

            # 处理命名空间
            ns = {'maven': 'http://maven.apache.org/POM/4.0.0'}
            if root.tag.startswith('{'):
                ns_uri = root.tag.split('{')[1].split('}')[0]
                ns = {'maven': ns_uri}

            # 查找 dependencies
            deps_elements = root.findall('.//maven:dependency', ns)

            for dep in deps_elements:
                group_id = dep.find('maven:groupId', ns)
                artifact_id = dep.find('maven:artifactId', ns)
                version = dep.find('maven:version', ns)
                scope = dep.find('maven:scope', ns)

                if group_id is not None and artifact_id is not None:
                    dep_info = {
                        'groupId': group_id.text,
                        'artifactId': artifact_id.text,
                        'version': version.text if version is not None else None,
                        'scope': scope.text if scope is not None else 'compile'
                    }
                    dependencies.append(dep_info)

            print(f"✓ 从 pom.xml 解析到 {len(dependencies)} 个直接依赖")
        except Exception as e:
            print(f"警告: 解析 pom.xml 失败: {e}")

        return dependencies

    def download_sources(self, dependencies: List[Dict]) -> None:
        """
        下载源码包
        使用 mvn dependency:sources 命令
        """
        print("\n下载源码包...")
        cmd = [self.maven_cmd, 'dependency:sources']

        result = subprocess.run(
            cmd,
            cwd=self.project_dir,
            capture_output=True,
            text=True
        )

        if result.returncode == 0:
            print("✓ 源码包下载完成")
        else:
            print(f"警告: 源码包下载失败: {result.stderr}")


class ArtifactLocator:
    """构件定位器"""

    def __init__(self):
        self.repo_path = Config.MAVEN_REPO

    def find_artifacts(self, dependency: Dict) -> Tuple[Optional[str], Optional[str]]:
        """
        查找构件的二进制包和源码包
        返回: (binary_path, source_path)
        """
        group_id = dependency['groupId']
        artifact_id = dependency['artifactId']
        version = dependency['version']

        # 转换 groupId 为路径
        group_path = group_id.replace('.', os.sep)

        # 二进制包路径
        binary_jar = os.path.join(
            self.repo_path,
            group_path,
            artifact_id,
            version,
            f"{artifact_id}-{version}.jar"
        )

        # 源码包路径
        source_jar = os.path.join(
            self.repo_path,
            group_path,
            artifact_id,
            version,
            f"{artifact_id}-{version}-sources.jar"
        )

        binary_path = binary_jar if os.path.exists(binary_jar) else None
        source_path = source_jar if os.path.exists(source_jar) else None

        return binary_path, source_path


class DecompilerWrapper:
    """反编译器包装类"""

    def __init__(self, decompiler_path: str):
        self.decompiler_path = decompiler_path

    def decompile(self, jar_path: str, output_dir: str) -> bool:
        """
        反编译 JAR 文件
        使用指定的 java-decompiler
        """
        try:
            # 确保输出目录存在
            os.makedirs(output_dir, exist_ok=True)

            # 构建命令
            cmd = [
                'java', '-jar',
                self.decompiler_path,
                '-hes=0',  # Hide empty super
                '-hdc=0',  # Hide default constructor
                jar_path,
                output_dir
            ]

            # 执行反编译
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=120
            )

            return result.returncode == 0
        except Exception as e:
            print(f"反编译失败 {jar_path}: {e}")
            return False


class Extractor:
    """文件提取器"""

    @staticmethod
    def extract_archive(archive_path: str, output_dir: str) -> bool:
        """
        解压归档文件（支持 zip, jar, tar.gz）
        """
        try:
            os.makedirs(output_dir, exist_ok=True)

            if archive_path.endswith(('.zip', '.jar')):
                with zipfile.ZipFile(archive_path, 'r') as zf:
                    zf.extractall(output_dir)
                return True
            elif archive_path.endswith('.tar.gz'):
                with tarfile.open(archive_path, 'r:gz') as tf:
                    tf.extractall(output_dir)
                return True
            else:
                print(f"不支持的文件格式: {archive_path}")
                return False
        except Exception as e:
            print(f"解压失败 {archive_path}: {e}")
            return False

    @staticmethod
    def copy_directory(src: str, dst: str) -> bool:
        """复制目录"""
        try:
            if os.path.exists(dst):
                shutil.rmtree(dst)
            shutil.copytree(src, dst)
            return True
        except Exception as e:
            print(f"复制目录失败 {src} -> {dst}: {e}")
            return False


class MavenDependencyExtractor:
    """主提取器"""

    def __init__(self, project_dir: str, decompiler_path: str = None):
        self.project_dir = os.path.abspath(project_dir)
        self.decompiler_path = decompiler_path
        self.output_dir = Config.THIRD_DIR

        # 检测 Maven
        self.maven_cmd = MavenDetector.find_maven()
        if not self.maven_cmd:
            raise RuntimeError("未找到 Maven")

        # 初始化组件
        self.parser = DependencyParser(self.maven_cmd, self.project_dir)
        self.locator = ArtifactLocator()

        # 反编译器（可选）
        self.decompiler = None
        if decompiler_path and os.path.exists(decompiler_path):
            self.decompiler = DecompilerWrapper(decompiler_path)

    def run(self) -> None:
        """执行提取流程"""
        print("=" * 60)
        print("Maven 依赖提取工具")
        print("=" * 60)
        print(f"项目目录: {self.project_dir}")
        print(f"输出目录: {self.output_dir}")
        print(f"Maven 命令: {self.maven_cmd}")
        print("=" * 60)

        # 1. 获取依赖树
        dependencies = self.parser.get_dependency_tree()
        if not dependencies:
            print("未找到任何依赖")
            return

        # 2. 下载源码
        self.parser.download_sources(dependencies)

        # 3. 处理每个依赖
        print("\n处理依赖...")
        os.makedirs(self.output_dir, exist_ok=True)

        stats = {
            'total': len(dependencies),
            'source_extracted': 0,
            'decompiled': 0,
            'skipped': 0,
            'failed': 0
        }

        for i, dep in enumerate(dependencies, 1):
            dep_key = dep.get('key', f"{dep['groupId']}:{dep['artifactId']}")
            print(f"\n[{i}/{stats['total']}] 处理: {dep_key}")

            # 查找构件
            binary_path, source_path = self.locator.find_artifacts(dep)

            if source_path:
                # 有源码包，直接解压
                print(f"  ✓ 找到源码包: {os.path.basename(source_path)}")
                artifact_dir = os.path.join(self.output_dir, dep['artifactId'])
                if Extractor.extract_archive(source_path, artifact_dir):
                    stats['source_extracted'] += 1
                    print(f"  ✓ 源码已解压到: {artifact_dir}")
                else:
                    stats['failed'] += 1
            elif binary_path:
                # 没有源码包，尝试反编译
                print(f"  ⚠ 未找到源码包，使用二进制包")

                if self.decompiler:
                    print(f"  → 反编译: {os.path.basename(binary_path)}")
                    artifact_dir = os.path.join(self.output_dir, dep['artifactId'])

                    # 创建临时目录用于反编译
                    temp_dir = f"{artifact_dir}_temp"
                    if self.decompiler.decompile(binary_path, temp_dir):
                        # 移动反编译结果到目标目录
                        if os.path.exists(temp_dir):
                            # 查找实际的输出目录（fern flower 可能会创建子目录）
                            contents = os.listdir(temp_dir)
                            if contents:
                                main_content = contents[0]
                                src_path = os.path.join(temp_dir, main_content)
                                Extractor.copy_directory(src_path, artifact_dir)
                                shutil.rmtree(temp_dir)
                                stats['decompiled'] += 1
                                print(f"  ✓ 反编译完成: {artifact_dir}")
                            else:
                                stats['failed'] += 1
                    else:
                        stats['failed'] += 1
                else:
                    print(f"  ⚠ 跳过（未配置反编译器）")
                    stats['skipped'] += 1
            else:
                print(f"  ✗ 未找到构件（二进制包和源码包都不存在）")
                stats['failed'] += 1

        # 4. 打印统计信息
        print("\n" + "=" * 60)
        print("提取完成！")
        print("=" * 60)
        print(f"总计依赖: {stats['total']}")
        print(f"源码已解压: {stats['source_extracted']}")
        print(f"反编译成功: {stats['decompiled']}")
        print(f"跳过: {stats['skipped']}")
        print(f"失败: {stats['failed']}")
        print(f"输出目录: {os.path.abspath(self.output_dir)}")
        print("=" * 60)


def main():
    """主函数"""
    import argparse

    parser = argparse.ArgumentParser(
        description='Maven 项目依赖提取工具',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  # 基本使用（提取源码，不反编译）
  python %(prog)s /path/to/project

  # 使用反编译器
  python %(prog)s /path/to/project -d /path/to/java-decompiler.jar

  # 指定输出目录
  python %(prog)s /path/to/project -o THIRD_LIBS

环境变量:
  MAVEN_HOME  Maven 安装目录
  THIRD_DIR   输出目录（默认: THIRD）
        """
    )

    parser.add_argument(
        'project_dir',
        help='Maven 项目目录（包含 pom.xml）'
    )

    parser.add_argument(
        '-d', '--decompiler',
        help='Java 反编译器路径（java-decompiler.jar）'
    )

    parser.add_argument(
        '-o', '--output',
        help='输出目录（默认: THIRD，可通过环境变量 THIRD_DIR 配置）'
    )

    args = parser.parse_args()

    # 配置输出目录
    if args.output:
        Config.THIRD_DIR = args.output

    try:
        # 创建提取器并运行
        extractor = MavenDependencyExtractor(
            project_dir=args.project_dir,
            decompiler_path=args.decompiler
        )
        extractor.run()

    except Exception as e:
        print(f"错误: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()
