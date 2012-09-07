@echo off
echo *********************************************************
echo *                                                       *
echo *    IAP WATCH SERVICE with auto-updating client (R2)   *
echo *                                                       *
echo *        Developed by Dr Christian Klukas, RG IA        *
echo *                                                       *
echo *********************************************************
echo Config file 'watch.txt' will be created and saved as
echo %APPDATA%\IAP\watch\watch.txt
cd /d %~dp0
echo Current path:
cd
:download
echo Current client will be downloaded from http://ba-13.ipk-gatersleben.de/iap.jar
java -cp iap.jar iap.Download http://ba-13.ipk-gatersleben.de/iap.jar iap2.jar
IF %ERRORLEVEL% NEQ 0 GOTO downloaderror 
echo Move downloaded jar into place ...
move /Y iap2.jar iap.jar
IF %ERRORLEVEL% NEQ 0 GOTO downloaderror 
echo JAR Release Info:
dir iap.jar
echo One moment, start watch service execution ...
:start
java -Xmx200m -jar iap.jar watch-cmd
echo.
echo Watch service client closed.
echo Waiting 1 minute for restart.
echo.
echo INFO: Config file 'watch.txt' will be created and saved as
echo %APPDATA%\IAP\watch\watch.txt
PING 1.1.1.1 -n 1 -w 60000 >NUL
GOTO download
:downloaderror
echo Return value indicates that the current release could not be downloaded. Waiting 10 minutes for next try.
PING 1.1.1.1 -n 10 -w 60000 >NUL
GOTO download
