@echo off
call .\gradlew :build
move /y .\build\libs\anti-vpn-service.jar .\
rd /s /q .\build