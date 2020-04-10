@ECHO OFF
ECHO ===========================
ECHO Build Modes
ECHO 1. Full build with tests
ECHO 2. Fast build with no tests
ECHO ===========================
SET /P MODE=Mode:
IF %MODE%==1 GOTO FULL
IF %MODE%==2 GOTO FAST
:FULL
call mvnw clean package
GOTO END
:FAST
call mvnw clean package -Dmaven.test.skip -Dcheckstyle.skip
GOTO END
:END