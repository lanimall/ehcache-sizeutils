#!/bin/sh

#
# All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
#

JAVA_OPTS="${JAVA_OPTS} -Xms128m -Xmx512m -XX:MaxDirectMemorySize=10G -Dcom.tc.productkey.path=terracotta-license.key -Dehcache.config.path=classpath:ehcache.xml"

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
${JAVA_OPTS} \
-cp "${TC_INSTALL_DIR}/custom/lib/EhCacheUtils-1.0.0.jar:${TC_INSTALL_DIR}/ehcache/lib/*" \
org.terracotta.utils.SizeIteratorLauncher "$@"