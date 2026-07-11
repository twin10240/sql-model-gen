@echo off
setlocal
set "APP_DIR=%~dp0"
java -cp "%APP_DIR%modelconvertor.jar;%APP_DIR%ojdbc8.jar" org.sqlmodel.Main %*
exit /b %ERRORLEVEL%
