@echo Pack the ZIP files for Win and Mac
@cd %cd%\dist\Mac
@7z u ..\Tonga.app.zip Tonga.app\Contents\Info.plist
@7z u ..\Tonga.app.zip Tonga.app\Contents\Resources\Tonga.main
@cd ..\Win
@7z a ..\Tonga.zip *
@cd ..\
@ren Tonga.app.zip Tonga.Mac.v%1%2.zip
@ren Tonga.zip Tonga.Win.v%1%2.zip