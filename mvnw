#!/bin/sh
# Maven Wrapper script — do not modify
MAVEN_WRAPPER_JAR=".mvn/wrapper/maven-wrapper.jar"
MAVEN_WRAPPER_PROPERTIES=".mvn/wrapper/maven-wrapper.properties"
if [ -r "$MAVEN_WRAPPER_PROPERTIES" ]; then
  while IFS="=" read key value; do
    case "$key" in (wrapperUrl) WRAPPER_JAR_URL="$value"; break;; esac
  done < "$MAVEN_WRAPPER_PROPERTIES"
fi
if [ -r "$MAVEN_WRAPPER_JAR" ]; then :
else
  if command -v wget > /dev/null; then
    wget "$WRAPPER_JAR_URL" -O "$MAVEN_WRAPPER_JAR" 2>/dev/null
  elif command -v curl > /dev/null; then
    curl -o "$MAVEN_WRAPPER_JAR" "$WRAPPER_JAR_URL" 2>/dev/null
  fi
fi
exec java -jar "$MAVEN_WRAPPER_JAR" "$@"
