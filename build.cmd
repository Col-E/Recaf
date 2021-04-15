@ECHO OFF
ECHO ===========================
ECHO Build Modes
ECHO ------
ECHO  1. Build with tests (Slow)
ECHO  2. Build with no tests (Fast)
ECHO ------
ECHO  3. Exit
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