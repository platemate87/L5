@echo off
title L1J-4Team Server Console
:start
echo Starting L1J-En Server.
echo.
REM -------------------------------------
REM Default parameters for a basic server.
java -server -Xms1024m -Xmx1024m -XX:+UseCodeCacheFlushing -XX:+OptimizeStringConcat -XX:+UseG1GC -XX:+TieredCompilation -XX:+UseCompressedOops -XX:SurvivorRatio=8 -XX:NewRatio=4 -cp "l1jen.jar;lib\*" l1j.server.Server
REM
REM If you have a big server and lots of memory, you could experiment for example with
REM java -server -Xmx1536m -Xms1024m -Xmn512m -XX:PermSize=256m -XX:SurvivorRatio=8 -Xnoclassgc -XX:+AggressiveOpts
REM -------------------------------------
if ERRORLEVEL 2 goto restart
if ERRORLEVEL 1 goto error
goto end
:restart
echo.
echo Admin Restart ...
echo.
goto start
:error
echo.
echo Server terminated abnormaly
echo.
:end
echo.
echo server terminated
echo.
pause
