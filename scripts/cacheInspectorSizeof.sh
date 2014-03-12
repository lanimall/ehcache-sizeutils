#!/bin/sh

#
# All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
#

INSTALL_DIR=`dirname "$0"`/..

if [ -f "${INSTALL_DIR}/bin/setenv.sh" ]; then
  . "${INSTALL_DIR}/bin/setenv.sh"
fi

JAVA_OPTS="-Xms128m -Xmx512m -XX:MaxDirectMemorySize=10G ${JAVA_OPTS}"
JAVA_OPTS="${JAVA_OPTS} -DuseThreading"
JAVA_OPTS="${JAVA_OPTS} -DserializedSize"

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
esac

if test \! -d "${JAVA_HOME}"; then
  echo "$0: the JAVA_HOME environment variable is not defined correctly"
  exit 2
fi

# For Cygwin, convert paths to Windows before invoking java
if $cygwin; then
  [ -n "$INSTALL_DIR" ] && INSTALL_DIR=`cygpath -d "$INSTALL_DIR"`
fi

exec "${JAVA_HOME}/bin/java" \
${JAVA_OPTS} -Dcom.tc.productkey.path=${TC_PRODUCTKEY} -Dehcache.config.path=${EHCACHE_PATH} -Dlog4j.configuration=file:${INSTALL_DIR}/config/log4j.properties \
-cp "${INSTALL_DIR}/lib/*:${CLIENT_CLASSPATH}" \
org.terracotta.utils.SizeIteratorLauncher "$@"