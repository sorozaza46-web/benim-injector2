#!/bin/sh

# Global gradle aramak yerine setup-gradle'ın indirdiği jar dosyasını tetikler
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

# GitHub Actions üzerindeki yerel gradle yapısını çalıştırır
exec "$JAVACMD" -Xmx64m "-Dorg.gradle.appname=gradlew" -classpath "$HOME/.gradle/wrapper/dists" org.gradle.launcher.GradleMain "$@"
