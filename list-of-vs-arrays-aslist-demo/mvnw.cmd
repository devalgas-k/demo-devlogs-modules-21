@REM Maven startup script for Windows
@echo off
set MAVEN_OPTS=-Xmx64m
java %MAVEN_OPTS% -jar "%~dp0mvnw\" org.apache.maven.wrapper.MavenWrapperMain %*
