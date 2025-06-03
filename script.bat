@echo off
setlocal EnableDelayedExpansion

echo Searching for .java files...

REM Loop through all .java files
for /R %%f in (*.java) do (
    echo Checking file: %%f

    REM Check if the file contains "jakarta.xml.bind"
    findstr /C:"jakarta.xml.bind" "%%f" >nul
    if !errorlevel! == 0 (
        echo Found jakarta.xml.bind in %%f - replacing...
        powershell -Command "(Get-Content -Raw '%%f') -replace 'jakarta\.xml\.bind', 'javax.xml.bind' | Set-Content '%%f'"
    ) else (
        echo No jakarta.xml.bind in %%f
    )
)

echo Done!
pause
