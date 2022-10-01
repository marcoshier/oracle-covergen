PATH=%PATH%;C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2022.1\jbr\bin

:loop
call ./gradlew.bat run
GOTO loop
