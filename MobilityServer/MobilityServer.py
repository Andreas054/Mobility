import subprocess
import re
import os
import socket
import time
import datetime
from datetime import date, timedelta
from http.server import HTTPServer, BaseHTTPRequestHandler
from http import HTTPStatus
import json


dbdir= '"D:\IBData\SMARTCASH.FDB"'
#dbdir = '"172.16.0.174:D:\IBData\SMARTCASH.FDB"'
programdir = "C:/Users/Operator/AppData/Local/Programs/Python/Python37/"

# ID User (1002 = Operator)
# 1003 = Stoica
idoperator = 1003

artnr = "0"

def isqlquery(condition, selection, table, column, data):
    tmp=os.system('echo CONNECT {}; > inputfile'.format(dbdir))
    if condition == 1:
        tmp=os.system('echo SELECT {} FROM {} WHERE {}={}; >> inputfile'.format(selection, table, column, data))
    elif condition == 2:
        tmp=os.system('echo SELECT {} FROM {} WHERE INACTIV=0 ORDER BY {}; >> inputfile'.format(selection, table, column))
    elif condition == 3:
        tmp=os.system('echo SELECT {} FROM {}; >> inputfile'.format(selection, table))
    return str(subprocess.check_output('isql.exe -u SYSDBA -p masterke -i "{}inputfile"'.format(programdir),shell=True))
    
def isqlinsert(condition, table, values, matching, valueswhere):
    tmp=os.system('echo CONNECT {}; > inputfile'.format(dbdir))
    if condition == 1:
        tmp=os.system('echo UPDATE OR INSERT INTO {} VALUES {} ; >> inputfile'.format(table, values))
    if condition == 2:
        tmp=os.system('echo UPDATE OR INSERT INTO {} {} VALUES {} MATCHING({}); >> inputfile'.format(table, valueswhere, values, matching))
    tmp=os.system('echo COMMIT; >> inputfile')
    return str(subprocess.check_output('isql.exe -u SYSDBA -p masterke -i "{}inputfile"'.format(programdir),shell=True))
    
def isqlupdate(condition, table, column, value, columnwhere, valuewhere):
    tmp=os.system('echo CONNECT {}; > inputfile'.format(dbdir))
    if condition == 1:
        tmp=os.system('echo UPDATE {} SET {} = {} WHERE {} = {}; >> inputfile'.format(table, column, value, columnwhere, valuewhere))
    tmp=os.system('echo COMMIT; >> inputfile')
    return str(subprocess.check_output('isql.exe -u SYSDBA -p masterke -i "{}inputfile"'.format(programdir),shell=True))

def getOneItemFromDB(isqloutput):
    # Try in case of empty TABLE
    try:
        # Find first number in string
        # Keep the string starting with first number
        # Position of first whitespace
        # Leave only OneITEM in variable
        oneitem = re.search(r"\d", isqloutput)
        oneitem = isqloutput[oneitem.start():]
        removerest = re.search(r"\s", oneitem)
        oneitem = oneitem[:removerest.start()]
    # On exception return error message to Android
    except Exception as e:
        print("errmessageRECEPTIEHEADER " + str(e))
        return 0
    return oneitem

# Get lista furnizori from DB and return
def getListafurnizor():
    jsonList = []
    idfurnizor = 0
    numefurnizor = "Err"
    isqloutput = isqlquery(2, "IDFURN,NUME", "FURNIZORI", "NUME", 0)
    isqloutput = isqloutput.split("\\n")
    for curline in isqloutput:
        try:
            idfurnizor = re.search(r"\d", curline)
            idfurnizor = curline[idfurnizor.start():]
            removerest = re.search(r"\s", idfurnizor)
            curline = idfurnizor
            idfurnizor = idfurnizor[:removerest.start()]
            
            numefurnizor = re.search(r"\s", curline)
            numefurnizor = curline[numefurnizor.start()+1:]
            removerest = re.search(r"\\r", numefurnizor)
            numefurnizor = numefurnizor[:removerest.start()]
            # Remove trailing whitespaces
            numefurnizor = ' '.join(numefurnizor.split())
            # add to jsonlist
            jsonList.append({"id" : idfurnizor, "nume" : numefurnizor})
        except:
            continue
    return jsonList

# Get Name, Price and ARTNR of one product and return them
def getprodus(data):
    # Cod EAN
    data = "'" + str(data) + "'"
    isqloutput = isqlquery(1, "ARTNR", "CODURI", "COD",  data)
    # Try in case of non existent COD
    try:
        # Find first number in string
        # Keep the string starting with first number
        # Position of first whitespace
        # Leave only PRET in variable
        artnr = re.search(r"\d", isqloutput)
        artnr = isqloutput[artnr.start():]
        removerest = re.search(r"\s", artnr)
        artnr = artnr[:removerest.start()]
    # On exception return error message to Android
    except Exception as e:
        print("errmessage")
        return "eroare", 0, 0
    # Create inputfile for PRET and IDTVA
    isqloutput = isqlquery(1, "PRET,IDTVA", "CATALOG", "ARTNR", artnr)
    # Find first number (PRET) in string
    # Keep the string starting with first number
    # Position of first whitespace (after PRET)
    pret = re.search(r"\d", isqloutput)
    pret = isqloutput[pret.start():]
    removerestpret = re.search(r"\s", pret)
    # Keep string after PRET
    # Find first number (IDTVA) in PRET string
    # Keep the string starting with first number
    # Position of first whitespace (after IDTVA)
    # Leave only IDTVA in variable
    tva = pret[removerestpret.start():]
    removeresttva = re.search(r"\d", tva)
    tva = tva[removeresttva.start():]
    removeresttva = re.search(r"\s", tva)
    tva = tva[:removeresttva.start()]
    # Leave only PRET in variable
    pret = pret[:removerestpret.start()]
    # Convert IDTVA into actual TVA
    tva=float(tva)
    if tva==1:
        tva=1.19
    if tva==2:
        tva=1.09
    if tva==3:
        tva=1.05
    pretfinal = float(pret) * tva
    pretfinal = round(pretfinal, 2)
    # Create inputfile for DESCRIERE
    isqloutput = isqlquery(1, "DESCRIERE", "CATALOG", "ARTNR", artnr)
    # Find first "== " and keep that in isqloutput string
    descriere = re.search("== ", isqloutput)
    isqloutput = isqloutput[descriere.start():]
    # Find first linebreak and keep that +2 characters in descriere string
    descriere = re.search(r"\\n", isqloutput)
    descriere = isqloutput[descriere.start()+2:]
    # Find 3 whitespaces in a row (indicating end of DESCRIERE) and keep that in descriere string
    removerestdescriere = re.search("   ", descriere)
    descriere = descriere[:removerestdescriere.start()]
    print(descriere)
    print(pretfinal) 
    return descriere, pretfinal, artnr

# Get receptii in lucru and return DOCNR and CANTITATE_TOTALA
def getReceptiiInLucru(idfurn):
    jsonList = [{"doc" : 0, "cantitatetotala" : 0}]
    isqloutput = isqlquery(1, "DOC, CANTITATE_TOTALA", "MOB_RECEPTIEHEADER", "IDFURN",  str(idfurn) + " AND STARE = 0")
    isqloutput = isqloutput.split("\\n")
    for curline in isqloutput:
        # Try in case of empty TABLE
        try:
            # Find first number in string
            # Keep the string starting with first number
            # Position of first whitespace
            docnr = re.search(r"\d", curline)
            docnr = curline[docnr.start():]
            removerest = re.search(r"\s", docnr)
            # Keep string after DOCNR
            # Find first number (CANTITATETOTALA) in DOCNR string
            # Keep the string starting with first number
            # Position of first whitespace (after CANTITATETOTALA)
            # Leave only CANTITATETOTALA in variable
            cantitatetotala = docnr[removerest.start():]
            removerestct = re.search(r"\d", cantitatetotala)
            cantitatetotala = cantitatetotala[removerestct.start():]
            removerestct = re.search(r"\s", cantitatetotala)
            cantitatetotala = cantitatetotala[:removerestct.start()]
            # Leave only DOCNR in variable
            docnr = docnr[:removerest.start()]
            docnr = int(docnr)
            jsonList.append({"doc" : docnr, "cantitatetotala" : cantitatetotala})
        # On exception return error message to Android
        except Exception as e:
            continue
    return jsonList

# Insert into MOB_RECEPTIEHEADER either NEW or EXISTING receptie based on boolean boolNewReceptie
def sendMobReceptieHeader(docnr, idfurn, boolNewReceptie):
    idrec = 0
    # GET CURRENT DATE IN FIREBIRD FORMAT
    now = datetime.datetime.now()
    now = str(now)
    datequery = now[:10]
    datequery = "'" + datequery + "'"
    print(datequery)
    
    valueswhere = "(DATAREC, DOC, DATADOC, IDFURN, IDSTORE, IDAPLICATIE, IDTERMINAL, STARE, IDOPERATOR, IDLINK)"
    values = (datequery[1:-1], docnr, datequery[1:-1], idfurn, -1, 4, 1, 0, idoperator, 0)
    print(values)
    isqlinsert(2, "MOB_RECEPTIEHEADER", values, "DOC, IDFURN", valueswhere)
    
    isqloutput = isqlquery(1, "IDREC", "MOB_RECEPTIEHEADER", "DOC",  str(docnr) + "AND IDFURN = {}".format(idfurn))
    idrec = getOneItemFromDB(isqloutput)
    return idrec

# Insert into MOB_RECEPTIE NEW produs from receptie with cantitate
def sendMobReceptie(idrec, artnr, cantitate):
    now = datetime.datetime.now()
    lastupdate = str(now)[:-2]
    
    values = (1, idrec, artnr, cantitate, lastupdate)
    isqlinsert(1, "MOB_RECEPTIE", values, 0, 0)

# Change the boolean STARE from MOB_RECEPTIEHEADER to 1
def sendEmiteReceptie(idrec):
    isqloutput = isqlquery(1, "SUM(CANTITATE)", "MOB_RECEPTIE", "IDREC",  idrec)
    cantitatetotala = getOneItemFromDB(isqloutput)
    
    isqlupdate(1, "MOB_RECEPTIEHEADER", "STARE", 1, "IDREC", idrec)
    isqlupdate(1, "MOB_RECEPTIEHEADER", "CANTITATE_TOTALA", cantitatetotala, "IDREC", idrec)

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
            now = datetime.datetime.now()
            tmp = os.system('echo [{}] : GET Lista Furnizori >> {}MobilityServer.txt'.format(now,programdir))
        
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
            now = datetime.datetime.now()
            tmp = os.system('echo [{}] : POST Receptii In Lucru >> {}MobilityServer.txt'.format(now,programdir))
            
        # Insert into MOB_RECEPTIEHEADER either NEW or EXISTING receptie based on boolean boolNewReceptie
        if self.path == '/mobreceptieheader':
            # {"boolNewReceptie": "True", "doc": "123456", "idfurn": "096"}
            print(message)
            boolNewReceptie = message['boolNewReceptie']
            docnr = message['doc']
            idfurn = message['idfurn']
            
            isqloutput = isqlquery(1, "IDREC", "MOB_RECEPTIEHEADER", "DOC", str(docnr) + "AND IDFURN = {}".format(idfurn))
            idrec = getOneItemFromDB(isqloutput)
            
            # In case one RECEPTIE already exists with the same DOCNR send boolean false to Android
            if idrec != 0 and boolNewReceptie == True:
                self.wfile.write(json.dumps([{'success': False, 'idrec': idrec}]).encode('utf-8'))
                print ("DOCNR Already Exists!!")
            else:
                # Call subprogram to insert in DB
                idrec = sendMobReceptieHeader(docnr, idfurn, boolNewReceptie)
                # Get the idrec of entry with specific DOCNR
                self.wfile.write(json.dumps([{'success': True, 'idrec': idrec}]).encode('utf-8'))
            # Write to log current date and mark stuff
            now = datetime.datetime.now()
            tmp = os.system('echo [{}] : POST MOB_RECEPTIEHEADER >> {}MobilityServer.txt'.format(now,programdir))

        # Get produs NUME, PRET, ARTNR
        if self.path == '/produscurentpret':
            # {"codprodus": "5942325000233"}
            codprodus = message['codprodus']
            print(codprodus)
            descriere, pretfinal, artnr = getprodus(codprodus)

            self.wfile.write(json.dumps([{'numeprodus': descriere,'pretprodus': pretfinal, 'artnr': artnr}]).encode('utf-8'))
            # Write to log current date and mark stuff
            now = datetime.datetime.now()
            tmp = os.system('echo [{}] : POST Produs Curent Pret >> {}MobilityServer.txt'.format(now,programdir))

        # Get IDREC, ARTNR, DOCNR, CANTITATE for the current PRODUCT to add to MOB_RECEPTIE
        if self.path == '/produscurent':
            # {"idrec": "49", "artnr": "12007", "docnr": "123456", "cantitate": "15"}
            print(message)
            idrec = message['idrec']
            artnr = message['artnr']
            docnr = message['docnr']
            cantitate = message['cantitate']
            sendMobReceptie(idrec, artnr, cantitate)

            self.wfile.write(json.dumps([{'success': True}]).encode('utf-8'))
            # Write to log current date and mark stuff
            now = datetime.datetime.now()
            tmp = os.system('echo [{}] : POST Produs Curent >> {}MobilityServer.txt'.format(now,programdir))

        # Change boolean STARE to 1 in MOB_RECEPTIEHEADER when receptie is FINISHED
        if self.path == '/emitereceptie':
            # {"idrec": "49"}
            idrec = message['idrec']
            sendEmiteReceptie(idrec)

            self.wfile.write(json.dumps([{'success': True}]).encode('utf-8'))
            # Write to log current date and mark stuff
            now = datetime.datetime.now()
            tmp = os.system('echo [{}] : POST Emite Receptie >> {}MobilityServer.txt'.format(now,programdir))

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
now = datetime.datetime.now()
tmp = os.system('echo ==================================== >> {}MobilityServer.txt'.format(programdir))
tmp = os.system('echo [{}] : Start program >> {}MobilityServer.txt'.format(now,programdir))

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
