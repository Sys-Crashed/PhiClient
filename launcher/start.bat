@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

:: Debug Mode - true: run with a console window (java.exe) and pause on exit
::              false: run headless (javaw.exe) and close this window at once
set DEBUG=false

set MC_USERNAME=PhiClient User
set ACCESS_TOKEN=0
set UUID=0
set VERSION=1.8.9
set MEMORY=2

if not exist "game" mkdir "game"

if "%DEBUG%"=="true" (
    set JAVA_EXEC=java.exe
    @echo on
) else (
    set JAVA_EXEC=javaw.exe
    @echo off
)

:: 1) Prefer the bundled Java 8 runtime shipped next to this script (java\bin\javaw.exe).
:: 2) Otherwise look for a Java 8 install on this machine.
:: Minecraft 1.8.9 and LaunchWrapper 1.11 only run on Java 8: on Java 9+ the system
:: class loader is no longer a URLClassLoader, so launchwrapper dies with
:: "AppClassLoader cannot be cast to java.net.URLClassLoader".
set "JAVA_PATH=%CD%\java\bin\%JAVA_EXEC%"
if not exist "%JAVA_PATH%" call :findJava

if not defined JAVA_PATH (
    echo.
    echo [PhiClient] No Java 8 installation found.
    echo Minecraft 1.8.9 and LaunchWrapper 1.11 require Java 8.
    echo Install Java 8, or unzip a Java 8 runtime into the "java" folder.
    echo.
    pause
    exit /b 1
)

:: Java launch parameters
set LAUNCH_ARGS=
set LAUNCH_ARGS=%LAUNCH_ARGS% -Xmx%MEMORY%G
set LAUNCH_ARGS=%LAUNCH_ARGS% -Xms%MEMORY%G
set LAUNCH_ARGS=%LAUNCH_ARGS% -XX:+DisableExplicitGC
set LAUNCH_ARGS=%LAUNCH_ARGS% -XX:+UseConcMarkSweepGC
set LAUNCH_ARGS=%LAUNCH_ARGS% -XX:+UseParNewGC
set LAUNCH_ARGS=%LAUNCH_ARGS% -XX:+UseNUMA
set LAUNCH_ARGS=%LAUNCH_ARGS% -XX:+CMSParallelRemarkEnabled
set LAUNCH_ARGS=%LAUNCH_ARGS% -XX:MaxTenuringThreshold=15
set LAUNCH_ARGS=%LAUNCH_ARGS% -XX:MaxGCPauseMillis=30
set LAUNCH_ARGS=%LAUNCH_ARGS% -XX:GCPauseIntervalMillis=150
set LAUNCH_ARGS=%LAUNCH_ARGS% -XX:-UseGCOverheadLimit
set LAUNCH_ARGS=%LAUNCH_ARGS% -XX:+UseBiasedLocking
set LAUNCH_ARGS=%LAUNCH_ARGS% -XX:SurvivorRatio=8
set LAUNCH_ARGS=%LAUNCH_ARGS% -XX:TargetSurvivorRatio=90
set LAUNCH_ARGS=%LAUNCH_ARGS% -XX:+UseFastAccessorMethods
set LAUNCH_ARGS=%LAUNCH_ARGS% -XX:+UseCompressedOops
set LAUNCH_ARGS=%LAUNCH_ARGS% -XX:+OptimizeStringConcat
set LAUNCH_ARGS=%LAUNCH_ARGS% -XX:+AggressiveOpts
set LAUNCH_ARGS=%LAUNCH_ARGS% -XX:ReservedCodeCacheSize=2048m
set LAUNCH_ARGS=%LAUNCH_ARGS% -XX:+UseCodeCacheFlushing
set LAUNCH_ARGS=%LAUNCH_ARGS% -XX:SoftRefLRUPolicyMSPerMB=10000
set LAUNCH_ARGS=%LAUNCH_ARGS% -Dfml.ignorePatchDiscrepancies=true
set LAUNCH_ARGS=%LAUNCH_ARGS% -Dfml.ignoreInvalidMinecraftCertificates=true
set LAUNCH_ARGS=%LAUNCH_ARGS% -Djava.library.path="%CD%\natives"

:: Build classpath
set CP=
for %%f in ("%CD%\libraries\*.jar") do set CP=!CP!%%f;
set CP=%CP%%CD%\minecraft\1.8.9.jar;%CD%\minecraft\PhiClient.jar

:: Launch Minecraft
if "%DEBUG%"=="true" (
    echo Debug Mode: ON
    echo Java Path: %JAVA_PATH%
    echo Launch Arguments: %LAUNCH_ARGS%
    echo.

    "%JAVA_PATH%" %LAUNCH_ARGS% -cp "%CP%" net.minecraft.launchwrapper.Launch ^
        --accessToken %ACCESS_TOKEN% ^
        --version %VERSION% ^
        --username "%MC_USERNAME%" ^
        --uuid %UUID% ^
        --gameDir "%CD%\game" ^
        --assetsDir "%CD%\assets" ^
        --assetIndex 1.8 ^
        --tweakClass org.dpdns.syscrashed.phiclient.injection.mixin.PhiTweaker
    pause
) else (
    start "" /B "%JAVA_PATH%" %LAUNCH_ARGS% -cp "%CP%" net.minecraft.launchwrapper.Launch ^
        --accessToken %ACCESS_TOKEN% ^
        --version %VERSION% ^
        --username "%MC_USERNAME%" ^
        --uuid %UUID% ^
        --gameDir "%CD%\game" ^
        --assetsDir "%CD%\assets" ^
        --assetIndex 1.8 ^
        --tweakClass org.dpdns.syscrashed.phiclient.injection.mixin.PhiTweaker
    exit /b 0
)

exit /b 0

:: Look for a Java 8 install: JAVA_HOME, PATH, registry, then common install dirs.
:: Candidate is accepted only when its version string contains 1.8
:findJava
set "JAVA_PATH="
set "PC_JV=%TEMP%\phiclient_java_version.txt"

if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" call :checkDir "%JAVA_HOME%\bin"

:: java.exe on PATH (javaw.exe prints nothing, so always probe with java.exe)
if not defined JAVA_PATH (
    java.exe -version >"%PC_JV%" 2>&1
    findstr /c:"1.8" "%PC_JV%" >nul
    if not errorlevel 1 set "JAVA_PATH=%JAVA_EXEC%"
)

if not defined JAVA_PATH (
    for %%k in ("HKLM\SOFTWARE\JavaSoft\JRE\1.8" "HKLM\SOFTWARE\JavaSoft\JDK\1.8" "HKLM\SOFTWARE\WOW6432Node\JavaSoft\JRE\1.8" "HKLM\SOFTWARE\WOW6432Node\JavaSoft\JDK\1.8") do (
        if not defined JAVA_PATH (
            for /f "tokens=2,*" %%a in ('reg query %%~k /v JavaHome 2^>nul') do (
                if exist "%%b\bin\java.exe" call :checkDir "%%b\bin"
            )
        )
    )
)

if not defined JAVA_PATH (
    for /d %%d in ("%ProgramFiles%\Java\*" "%ProgramFiles%\Eclipse Adoptium\*" "%ProgramFiles%\Amazon Corretto\*" "%ProgramFiles%\Zulu\*" "%ProgramFiles%\BellSoft\*" "%USERPROFILE%\.jdks\*" "%USERPROFILE%\.gradle\jdks\*") do (
        if not defined JAVA_PATH (
            if exist "%%~d\bin\java.exe" call :checkDir "%%~d\bin"
        )
    )
)

if defined JAVA_PATH echo [PhiClient] Java: %JAVA_PATH%
goto :eof

:: Accept a bin directory only when its java.exe reports 1.8
:checkDir
set "CAND=%~1\java.exe"
"%CAND%" -version >"%PC_JV%" 2>&1
findstr /c:"1.8" "%PC_JV%" >nul
if not errorlevel 1 set "JAVA_PATH=%~1\%JAVA_EXEC%"
goto :eof
