@ECHO OFF
ECHO ===========================
ECHO Build Modes
ECHO ------
ECHO Java 8
ECHO  1. Full build with tests
ECHO  2. Fast build with no tests
ECHO ------
ECHO Java 11 or higher
ECHO  3. Full build with tests
ECHO  4. Fast build with no tests
ECHO ------
ECHO  5. Exit
ECHO ===========================
SET /P MODE=Mode:
IF %MODE%==1 GOTO FULL8
IF %MODE%==2 GOTO FAST8
IF %MODE%==3 GOTO FULL11
IF %MODE%==4 GOTO FAST11
IF %MODE%==5 GOTO END
:FULL8
call mvnw clean package
GOTO END
:FAST8
call mvnw clean package -Dmaven.test.skip -Dcheckstyle.skip
GOTO END
:FULL11
call mvnw clean package -P java11
GOTO END
:FAST11
call mvnw clean package -Dmaven.test.skip -Dcheckstyle.skip -P java11
GOTO END
:END