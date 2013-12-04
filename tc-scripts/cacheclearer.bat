@echo off

rem All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.

setlocal
set TC_INSTALL_DIR=%~d0%~p0..
set TC_INSTALL_DIR="%TC_INSTALL_DIR:"=%"

if not defined JAVA_HOME (
  echo Environment variable JAVA_HOME needs to be set
  exit /b 1
)

set JAVA_HOME="%JAVA_HOME:"=%"
set JAVA_OPTS="%JAVA_OPTS% -Xms128m -Xmx512m -XX:MaxDirectMemorySize=10G -Dcom.tc.productkey.path=terracotta-license.key -Dehcache.config.path=classpath:ehcache.xml"

set CLASSPATH=%TC_INSTALL_DIR%\custom\lib\ehcache-sizeutils-1.0.0.jar;%TC_INSTALL_DIR%\ehcache\lib\*
%JAVA_HOME%\bin\java %JAVA_OPTS% -cp %CLASSPATH% org.terracotta.utils.CacheClearerLauncher %*
endlocal