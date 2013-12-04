#!/bin/sh

#
# All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
#

CLIENT_CLASSPATH="${HOME}/MyDev/MyTools/EhCacheUtils-Client/target/EhCacheUtils-Client-1.0.0.jar"
CLIENT_EHCACHE_PATH="classpath:ehcache3x.xml"
TC_LICENSE_PATH="${HOME}/terracotta-license.key"

JAVA_OPTS="${JAVA_OPTS} -Xms128m -Xmx512m -XX:MaxDirectMemorySize=10G"

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
esac

if test \! -d "${JAVA_HOME}"; then
  echo "$0: the JAVA_HOME environment variable is not defined correctly"
  exit 2
fi

TC_INSTALL_DIR=`dirname "$0"`/..

# For Cygwin, convert paths to Windows before invoking java
if $cygwin; then
  [ -n "$TC_INSTALL_DIR" ] && TC_INSTALL_DIR=`cygpath -d "$TC_INSTALL_DIR"`
fi

exec "${JAVA_HOME}/bin/java" \
${JAVA_OPTS} -Dcom.tc.productkey.path=${TC_LICENSE_PATH} -Dehcache.config.path=${CLIENT_EHCACHE_PATH} \
-cp "${TC_INSTALL_DIR}/custom/lib/ehcache-sizeutils-1.0.0.jar:${TC_INSTALL_DIR}/ehcache/lib/*:${TC_INSTALL_DIR}/common/*:${CLIENT_CLASSPATH}" \
org.terracotta.utils.CacheClearerLauncher "$@"