# Mobility

Python3 script server communicating with Kotlin written app client via an HTTP server and JSON objects.

## Installation

### Server Side

- Get IDOPERATOR for **Operator** from database:


	CONNECT "192.168.50.100:D:\IBData\SMARTCASH.FDB" user sysdba password masterke;
	SELECT IDOPERATOR, NUME, PRENUME FROM OPERATORI;


- Install Python3 on Server on **Operator** account
- Copy ***MobilityServer*** folder to where Python3 is installed
- Create shortcut for ***C:\Program Files\Firebird\Firebird_2_5\bin\isql.exe*** in the Python3 directory (*Python311/isql.lnk*)
- Edit these files changing the directories:
  - MobilityServer.bat
  - MobilityServer.py (⚠️change **idoperator**)
  - MobilityServer.vbs
- Add ***MobilityServer.vbs*** to start at LogOn (**Operator**)
- Insert Terminal info and arbitrary ArticoleColectate (with INTSIZE id) in database (⚠️change \<IDOPERATOR>):


	CONNECT "192.168.50.100:D:\IBData\SMARTCASH.FDB" user sysdba password masterke;
	INSERT INTO MOB_LISTATERMINALE (NRTERMINAL, SERIETERMINAL, ISACTIV) VALUES (1, 867351038210142, 1);
	INSERT INTO ARTICOLECOLECTATE (ID, TYPEOF, ARTNR, COD, IDOPERATOR) VALUES (2147483647, 2, 1, '1', <IDOPERATOR>);

### Client Side

- Change Server IP Address in Settings

