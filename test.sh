#!/usr/bin/env bash
set -euo pipefail
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"
BS_ANDROID_TOOLCHAIN="${BS_ANDROID_TOOLCHAIN:-$PROJECT_DIR/toolchain}"
if [[ -n "${BS_ANDROID_TOOLCHAIN:-}" ]]; then
  ecj() { java -Xmx384m -cp "$BS_ANDROID_TOOLCHAIN/usr/share/java/eclipse-jdt-core.jar" org.eclipse.jdt.internal.compiler.batch.Main "$@"; }
fi
mkdir -p build/tests
mapfile -t SOURCES < core-sources.txt
SOURCE_FILES=()
for name in "${SOURCES[@]}"; do SOURCE_FILES+=("app/src/main/java/com/danil/blackswordsman/$name.java"); done
ecj -proc:none -source 1.7 -target 1.7 -encoding UTF-8 -d build/tests "${SOURCE_FILES[@]}" tests/*.java
DEFAULT_TESTS="V8AnimationSmoke V8NatureSmoke V8MigrationSmoke LegacyMigrationSmoke V7WaterSmoke V7CreaturesSmoke V6RegressionSmoke LogicSmoke ControlSmoke MotionFilterSmoke StateButtonSmoke SkeletonSmoke PhysicsSmoke FullCampaignSmoke WorldStreamingSmoke PopulationSmoke SurrealSmoke AnomalySmoke ExplorationSmoke"
for test in ${*:-$DEFAULT_TESTS}; do
  java -Xmx256m -cp build/tests com.danil.blackswordsman.TestLauncher "$test"
done
