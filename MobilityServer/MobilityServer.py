# Version 1.3

import fdb
import time
import datetime
import json
from http.server import HTTPServer, BaseHTTPRequestHandler
from http import HTTPStatus
from threading import Thread

from config import *

countReceptiiEmise = 0
countProduseEtichetare = 0
countProduseVerificare = 0

def writeToLog(logMessage):
    now = datetime.datetime.now()
    with open(folderProgram + "MobilityServer.txt", "a") as logFile:
        logFile.write(f"[{now}]: {logMessage}\n")

def checkTimeSaveLog():
    global countReceptiiEmise
    global countProduseEtichetare
    global countProduseVerificare

    while True:
        now = datetime.datetime.now()
        currentTime = int(now.strftime("%H%M"))
        if 2150 - currentTime <= 30:
            writeToLog(f"[{datetime.datetime.now()}] : Nr Receptii = {countReceptiiEmise} ; Nr Produse Etichetare = {countProduseEtichetare} ; Nr Produse Verificare = {countProduseVerificare}\n")
            import sys
            sys.exit() # exit Thread
        time.sleep(30 * 60)

def fdbSqlOperation(operationType, fetchOneAll, fdbSQLCommand):
    try:
        fdbConnection = fdb.connect(dsn = dbdir, user = "sysdba", password = "masterkey") # Firebird
        fdbCursor1 = fdbConnection.cursor()
        fdbCursor1.execute(fdbSQLCommand)

        returnString = ""
        if operationType == "READ":
            if fetchOneAll == "ONE":
                returnString = fdbCursor1.fetchone()
            else:
                returnString = fdbCursor1.fetchall()
        else:
            fdbConnection.commit()

        fdbConnection.close()
        return returnString
    
    except Exception as e:
        print(e)

def getListafurnizor():
    jsonList = []
    listaCursor1 = fdbSqlOperation("READ", "ALL", f"SELECT idfurn, nume FROM furnizori WHERE inactiv = 0 ORDER BY nume")
    
    for idFurnizor, numeFurnizor in listaCursor1:
        jsonList.append({"id" : idFurnizor, "nume" : numeFurnizor})
    return jsonList

def getCantitateReceptie(idRec):
    listaCursor1 = fdbSqlOperation("READ", "ONE", f"SELECT COUNT(cantitate), SUM(cantitate) FROM mob_receptie WHERE idrec = {idRec}")
    
    if listaCursor1 is None or listaCursor1[0] == 0:
        return 0, 0.0
    return int(listaCursor1[0]), float(listaCursor1[1])

def getprodus(codEAN):
    listaCursor1 = fdbSqlOperation("READ", "ONE", f"SELECT PRET * ((SELECT tva FROM tva WHERE tva.idtva = catalog.idtva) / 100 + 1), descriere, artnr FROM catalog WHERE ARTNR = (SELECT artnr FROM coduri WHERE codwithcrc = '{codEAN}')")

    if listaCursor1 is None:
        print(f"Cod EAN Invalid {codEAN}")
        return None, None, None

    pret, descriere, artnr = listaCursor1
    pret = round(float(pret), 2)
    
    print(f"Get Produs: {artnr},{descriere},{pret}")
    return descriere, pret, artnr

# Get STOC of one product and return it
def getprodusstoc(artnr):
    if artnr is None:
        return None

    listaCursor1 = fdbSqlOperation("READ", "ONE", f"SELECT cantitate - rezervare FROM stoc WHERE artnr = {artnr}")

    if listaCursor1 is None:
        print("Get Stoc: NULL")
        return 0

    return float(listaCursor1[0])

def getReceptiiInLucru(idfurn):
    jsonList = [{"doc" : 0, "cantitatetotala" : 0}]

    listaCursor1 = fdbSqlOperation("READ", "ALL", f"SELECT doc, cantitate_totala FROM mob_receptieheader WHERE stare = 0 AND idfurn = {idfurn}")

    for docnr, cantitateTotala in listaCursor1:
        jsonList.append({"doc" : docnr, "cantitatetotala" : float(cantitateTotala)})

    return jsonList

def sendMobReceptieHeader(docnr, idfurn, boolNewReceptie):
    # GET CURRENT DATE IN FIREBIRD FORMAT
    now = datetime.datetime.now()
    datequery = now.strftime('%Y-%m-%d')
    
    fdbSQLCommand = f"UPDATE OR INSERT INTO mob_receptieheader (DATAREC, DOC, DATADOC, IDFURN, IDSTORE, IDAPLICATIE, IDTERMINAL, STARE, IDOPERATOR) "
    fdbSQLCommand += f"VALUES {(datequery, str(docnr), datequery, idfurn, -1, 4, idTerminal, 0, idoperator)} MATCHING(DOC, IDFURN)"
    fdbSqlOperation("CREATE", None, fdbSQLCommand)

    listaCursor1 = fdbSqlOperation("READ", "ONE", f"SELECT IDREC FROM MOB_RECEPTIEHEADER WHERE DOC = '{docnr}' AND IDFURN = {idfurn}")

    if listaCursor1 is None:
        print('MobReceptieHeader IDREC None???')
        return None # This should never happen...
    return listaCursor1[0]

def sendMobReceptie(idRec, artnr, cantitate):
    lastupdate = str(datetime.datetime.now())[:-2]
    fdbSqlOperation("CREATE", None, f"INSERT INTO mob_receptie (IDREC, ARTNR, CANTITATE, LASTUPDATE) VALUES {(idRec, artnr, cantitate, lastupdate)}")

def sendEmiteReceptie(idRec):
    listaCursor1 = fdbSqlOperation("READ", "ONE", f"SELECT SUM(cantitate) FROM mob_receptie WHERE idrec = {idRec}")

    if listaCursor1 is None:
        print('EmiteReceptie CANTITATE None???')
        return None # This should never happen...
    
    cantitateTotala = listaCursor1[0]
    
    fdbSqlOperation("CREATE", None, f"UPDATE mob_receptieheader SET stare = 1, cantitate_totala = {cantitateTotala} WHERE idrec = {idRec}")

    global countReceptiiEmise
    countReceptiiEmise += 1

def sendAddEtichetare(artnr, codprodus):
    dataeventadd = str(datetime.datetime.now())[:-2]
    
    fdbSQLCommand = f"INSERT INTO ARTICOLECOLECTATE (TYPEOF, ARTNR, COD, CANTITATE, IDOPERATOR, DATAEVENT_ADD, DISCOUNT, STATUS, IDLABELFORMAT) "
    fdbSQLCommand += f"VALUES (5, {artnr}, '{codprodus}', 1, {idoperator}, '{dataeventadd}', 0, 0, 1)"
    fdbSqlOperation("CREATE", None, fdbSQLCommand)

    global countProduseEtichetare
    countProduseEtichetare += 1

threadcheckTimeSaveLog = Thread(target = checkTimeSaveLog)
threadcheckTimeSaveLog.start()

# Sample blog post data similar to
# https://ordina-jworks.github.io/frontend/2019/03/04/vue-with-typescript.html#4-how-to-write-your-first-component
class _RequestHandler(BaseHTTPRequestHandler):
    # Borrowing from https://gist.github.com/nitaku/10d0662536f37a087e1b
    def _set_headers(self):
        self.send_response(HTTPStatus.OK.value)
        self.send_header('Content-type', 'application/json')
        # Allow requests from any origin, so CORS policies don't
        # prevent local development.
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()

    # In case of GET requests
    def do_GET(self):
        self._set_headers()
        # Lista Furnizori
        if self.path == '/furnizori':
            listafurnizori = getListafurnizor()
            self.wfile.write(json.dumps(listafurnizori).encode('utf-8'))
            # Write to log current date and mark stuff
            writeToLog("GET Lista Furnizori")

    # In case of POST requests
    def do_POST(self):
        length = int(self.headers.get('content-length'))
        message = json.loads(self.rfile.read(length))
        self._set_headers()
        
        # Receptii in lucru from ONE furnizor
        if self.path == '/receptiiinlucru':
            # {"idfurn": "096"}
            idfurn = message['idfurn']
            jsonList = getReceptiiInLucru(idfurn)
            print(jsonList)
            self.wfile.write(json.dumps(jsonList).encode('utf-8'))
            # Write to log current date and mark stuff
            writeToLog("POST Receptii In Lucru")
            
        # Insert into MOB_RECEPTIEHEADER either NEW or EXISTING receptie based on boolean boolNewReceptie
        if self.path == '/mobreceptieheader':
            # {"boolNewReceptie": "True", "doc": "123456", "idfurn": "096"}
            print(message)
            boolNewReceptie = message['boolNewReceptie']
            docnr = message['doc']
            idfurn = message['idfurn']
            
            idRec = fdbSqlOperation("READ", "ONE", f"SELECT IDREC FROM MOB_RECEPTIEHEADER WHERE DOC = '{docnr}' AND IDFURN = {idfurn}")

            # ?????????? else
            if idRec is not None and boolNewReceptie == True:
                idRec = idRec[0]
                self.wfile.write(json.dumps([{'success': False, 'idrec': idRec}]).encode('utf-8'))
                print ("DOCNR Already Exists!!")
            else:
                # Call subprogram to insert in DB
                idRec = sendMobReceptieHeader(docnr, idfurn, boolNewReceptie)
                # Get the idRec of entry with specific DOCNR
                self.wfile.write(json.dumps([{'success': True, 'idrec': idRec}]).encode('utf-8'))
            # Write to log current date and mark stuff
            writeToLog("POST MOB_RECEPTIEHEADER")

        # Get produs NUME, PRET, ARTNR
        if self.path == '/produscurentpret':
            # {"codprodus": "5942325000233"}
            codprodus = message['codprodus']
            print(codprodus)
            descriere, pretfinal, artnr = getprodus(codprodus)

            self.wfile.write(json.dumps([{'numeprodus': descriere, 'pretprodus': pretfinal, 'artnr': artnr}]).encode('utf-8'))
            # Write to log current date and mark stuff
            writeToLog("POST Produs Curent Pret")
        
        # Get produs NUME, PRET, ARTNR, STOC
        if self.path == '/produscurentpretstoc':
            # {"codprodus": "5942325000233"}
            codprodus = message['codprodus']
            print(codprodus)
            descriere, pretfinal, artnr = getprodus(codprodus)
            stoc = getprodusstoc(artnr)

            self.wfile.write(json.dumps([{'numeprodus': descriere,'pretprodus': pretfinal, 'artnr': artnr, 'stoc': stoc}]).encode('utf-8'))

            writeToLog("POST Produs Curent Stoc")

            global countProduseVerificare
            countProduseVerificare += 1

        # Get IDREC, ARTNR, DOCNR, CANTITATE for the current PRODUCT to add to MOB_RECEPTIE
        if self.path == '/produscurent':
            # {"idRec": "49", "artnr": "12007", "docnr": "123456", "cantitate": "15"}
            print(message)
            idRec = message['idrec']
            artnr = message['artnr']
            docnr = message['docnr']
            cantitate = message['cantitate']
            sendMobReceptie(idRec, artnr, cantitate)

            self.wfile.write(json.dumps([{'success': True}]).encode('utf-8'))
            # Write to log current date and mark stuff
            writeToLog("POST Produs Curent")

        if self.path == '/cantitatereceptie':
            idRec = message['idrec']
            countReceptie, cantitateReceptie = getCantitateReceptie(idRec)
            print(countReceptie, cantitateReceptie)
            
            self.wfile.write(json.dumps([{'countreceptie': countReceptie, 'cantitatereceptie': cantitateReceptie}]).encode('utf-8'))
            # Write to log current date and mark stuff
            writeToLog("POST Cantitate Receptie Curenta")

        # Change boolean STARE to 1 in MOB_RECEPTIEHEADER when receptie is FINISHED
        if self.path == '/emitereceptie':
            # {"idRec": "49"}
            idRec = message['idrec']
            sendEmiteReceptie(idRec)

            self.wfile.write(json.dumps([{'success': True}]).encode('utf-8'))
            # Write to log current date and mark stuff
            writeToLog("POST Emite Receptie")
        
        # Change boolean STARE to 0 in ARTICOLECOLECTATE when articol is added to the list
        if self.path == '/addetichetare':
            # {"idRec": "49"}
            
            artnr = message['artnr']
            codprodus = message['codprodus']
            sendAddEtichetare(artnr, codprodus)

            self.wfile.write(json.dumps([{'success': True}]).encode('utf-8'))
            # Write to log current date and mark stuff
            writeToLog("POST Add Etichetare")

    def do_OPTIONS(self):
        # Send allow-origin header for preflight POST XHRs.
        self.send_response(HTTPStatus.NO_CONTENT.value)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST')
        self.send_header('Access-Control-Allow-Headers', 'content-type')
        self.end_headers()

def run_server():
    server_address = ('', 8001)
    httpd = HTTPServer(server_address, _RequestHandler)
    print('serving at %s:%d' % server_address)
    httpd.serve_forever()

# Write to log current date and mark start of program
writeToLog("====================================Start program")

if __name__ == '__main__':
    run_server()
    
#JSON
#_g_furnizori = [
#    {
#      "id": 0,
##      "nume": "Furnizor"
#    }
#]

#_g_mobreceptieheader = [
#    {
#      "doc": 0,
#      "idfurn": 0
#    }
#]

#_g_produscurentpret = [
#    {
#        'codprodus': 1234
#    }
#]

#_g_produscurent = [
#    {
#        'idrec': 0,
#        'docnr': 0,
#        'codprodus': 1234,
#        'cantitate': 4321
#    }
#]
