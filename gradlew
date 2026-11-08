#!/bin/sh
#
# Gradle start up script for UN*X
#
# This script allows you to run gradle with no installation.
# It has been adapted from gradle's own gradle wrapper.

APP_NAME="Gradle"
APP_BASE_NAME=${0##*/}

PRG="$0"
while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`"/$link"
  fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -n "$JAVA_HOME" ] ; then
  JAVA="$JAVA_HOME/bin/java"
else
  JAVA="java"
fi

# Use array for JVM opts to avoid quoting issues
DEFAULT_JVM_OPTS=("-Xmx64m" "-Xms64m")

exec "$JAVA" "${DEFAULT_JVM_OPTS[@]}" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"