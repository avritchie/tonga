@echo Set the EXE version string to %1%2
@rcedit %cd%\dist\Win\Tonga.exe --set-file-version %1%2
@rcedit %cd%\dist\Win\Tonga.exe --set-product-version %1%2