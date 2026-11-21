#!/bin/sh
set -e
APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
if [ ! -f "$CLASSPATH" ]; then
    if command -v gradle >/dev/null 2>&1; then
        exec gradle "$@"
    fi
    echo "gradle-wrapper.jar tidak ditemukan dan gradle global belum terpasang." >&2
    exit 1
fi
exec "${JAVA_HOME:+$JAVA_HOME/bin/}java" -Xmx64m -Xms64m -Dorg.gradle.appname=gradlew -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
