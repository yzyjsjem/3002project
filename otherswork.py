#!/usr/bin/env python3
import socket
import select
import sys
import datetime

HEADER = 64
SERVER = 'localhost'
FORMAT = 'utf-8'
BUFF_SIZE = 1024 # 1 KiB
BACKLOG = 5


TIMETABLE = []

#conn.close()

def TCP_handle_client(s,udp):
    data =''  
    conn , addr = s.accept()
    print(f"[NEW TCP CONNECTION]{addr}")
    packet = conn.recv(BUFF_SIZE)
    data += packet.decode(FORMAT)
    if data:
        conn.setblocking(0)
        destination = TCP_handle_data(data,conn)
        if destination:
            UDP_first_send_all_adjacents(destination,udp)
        return conn
    else:
        conn.close()
        


def TCP_handle_data(data,conn):
    lines = data.splitlines()
    for line in lines:
        if line:
            line = line.split()
            if line[0] == 'GET':
                if line[1].startswith('/?to='):
                    line[1] = line[1].split('=')
                    return(line[1][1])
                if line[1] == '/favicon.ico':
                    conn.send("HTTP/1.1 400 Bad Request\n".encode(FORMAT))
                    conn.send('Content-Type: text/html\n'.encode(FORMAT))
                    conn.close()
                    print(f"[TCP CONNECTION CLOSED]")

def TCP_reply_close(startStation,dest,path,msg_namePath,conn,startTime,finalTime):
    conn.send("HTTP/1.1 200 OK\n".encode(FORMAT))
    conn.send('Content-Type: text/html\n'.encode(FORMAT))
    conn.send("""
        <html>
        <body>
        <h1>Travel Guide </h1> Leaving {} at {} you will arrive at {} at {}
        </body>
        </html>
        """.format(startStation,unconvertTime(startTime),dest,unconvertTime(finalTime)).encode(FORMAT))
    # conn.send('Path: {path}'.encode(FORMAT))
    # conn.send('NamePath: {NamePath}'.encode(FORMAT))
    print(f"[TCP CONNECTION CLOSED]")
    
    conn.close()

def UDP_handle_client(s,udp,conn):
    data,addr = s.recvfrom(BUFF_SIZE)
    print(f"[NEW UDP CONNECTION]{addr}")
    msg = data.decode(FORMAT)
    UDP_handle_message(msg,udp,conn)

def UDP_handle_message(msg,udp,conn):
    msg = msg.splitlines()
    msg_startStation = ''
    msg_startPort = ''
    msg_dest = ''
    msg_path = ''
    path = ''
    msg_namePath = ''
    msg_complete = False
    msg_departTimes = ''
    msg_arriveTimes = ''
    msg_timeDone = False
    startTime = ''
    finalTime = ''
    for line in msg:
        if line.startswith('Start:'):
            msg_start = line.split()
            msg_startStation = msg_start[1]
            msg_startPort = msg_start[2]
        if line.startswith('Destination:'):
            msg_dest = line
            dest = msg_dest.split()
            dest = dest[1]
        if line.startswith('Path:'):    
            msg_path = line
            path = msg_path.split()
            path = ' '.join(path[1:])
        if line.startswith('NamePath:'):
            msg_namePath = line
        if line.startswith('COMPLETE'):
            msg_complete = True
        if line.startswith('DepartTimes:'):
            msg_departTimes = line
        if line.startswith('ArriveTimes:'):
            msg_arriveTimes = line
        if line.startswith('TIMEDONE'):
            msg_timeDone = True
            finalTime = msg_arriveTimes.split()
            finalTime = finalTime[-1]
            startTime = msg_arriveTimes.split()
            startTime = startTime[1]


    if msg_complete:
        if msg_timeDone:
            namePath = msg_namePath.split()
            startStation = namePath[1]
            TCP_reply_close(startStation,dest,path,msg_namePath,conn,startTime,finalTime)
        else:
            calculateTime(udp,msg_path,msg_namePath,msg_departTimes,msg_arriveTimes,msg_dest)
    elif dest == STATIONNAME:
        path += ' ' + str(UDPPORT)
        msg_namePath +=' ' + STATIONNAME
        msg_namePath += '\n'
        msg = 'Start: {} {}\n'.format(msg_startStation,msg_startPort)
        msg += msg_dest + '\n'
        msg += 'Path:'
        msg += ' {}\n'.format(path)
        msg += msg_namePath + '\n'
        msg += msg_departTimes + '\n'
        msg += msg_arriveTimes + '\n'
        msg += 'COMPLETE\n'
        print('[MSG]:',msg)
        address = (SERVER,int(msg_startPort))
        udp.sendto(msg.encode(FORMAT),address)
        print(f"[UDP SENT] Sent msg to port: {address}")
    else:
        print(f'[Not Complete]:Has been to: {path}')
        path += ' ' + str(UDPPORT)
        msg_namePath +=' ' + STATIONNAME
        visited = path.split()
        visited = list(map(int,visited))
        for port in NEIGHBOUR:
            if port not in visited:
                msg = 'Start: {} {}\n'.format(msg_startStation,msg_startPort)
                msg += msg_dest + '\n'
                msg += 'Path:'
                msg += ' {}\n'.format(path)
                msg += msg_namePath + '\n'
                msg += msg_departTimes + '\n'
                msg += msg_arriveTimes + '\n'
                print('[MSG]:',msg)
                address = (SERVER,port)
                udp.sendto(msg.encode(FORMAT),address)
                print(f"[UDP SENT] Sent msg to port: {address}")


def calculateTime(udp,msg_path,msg_namePath,msg_departTimes,msg_arriveTimes,msg_dest):
    path = msg_path.split()
    path = path[1:]
    print('[Testing][Path]',path)
    namePath = msg_namePath.split()
    namePath = namePath[1:]
    print('[Testing][namePath]',namePath)
    departTimes = msg_departTimes.split()
    departTimes = departTimes[1:]
    print('[Testing][departTimes]',departTimes)
    arriveTimes = msg_arriveTimes.split()
    arriveTimes = arriveTimes[1:]
    print('[Testing][arriveTimes]',arriveTimes)
    dest = msg_dest.split()
    dest = dest[1]
    done = False
    for x in range(0,len(path)-1):
        if int(path[x]) == UDPPORT:
            thisStation = STATIONNAME
            print('[Testing][thisStation',thisStation)
            nextStation = namePath[x+1]
            print('[Testing][ nextStation',nextStation)
            for line in TIMETABLE[1:]:
                print('[LINE]',line)
                if int(arriveTimes[x])<line[0]:
                    msg_departTimes += ' ' + str(line[0])
                    msg_arriveTimes += ' ' + str(line[3])
                    msg = 'COMPLETE\n'
                    msg += msg_departTimes + '\n'
                    msg += msg_arriveTimes + '\n'
                    msg += msg_path + '\n'
                    msg += msg_dest + '\n'
                    msg += msg_namePath + '\n'
                    print('[line[4]]:',line[4])
                    print('[msg_dest]:',msg_dest)
                    if line[4] == dest:
                        msg += 'TIMEDONE\n'
                        print('[MSG]:',msg)
                        address = (SERVER,int(path[0]))
                        udp.sendto(msg.encode(FORMAT),address)
                        print(f"[UDP SENT] Sent msg to port: {address}")
                        break

                    for port in NEIGHBOUR:
                        if port == int(path[x+1]):
                            print('[MSG]:',msg)
                            address = (SERVER,port)
                            udp.sendto(msg.encode(FORMAT),address)
                            print(f"[UDP SENT] Sent msg to port: {address}")
                
                



def UDP_first_send_all_adjacents(destination,udp):
    msg = 'Start: {} {}\n'.format(STATIONNAME,UDPPORT)
    msg += 'Destination: {}\n'.format(destination)
    msg += 'Path:'
    msg += ' {}\n'.format(UDPPORT)
    msg += 'NamePath: {}\n'.format(STATIONNAME)
    msg += 'DepartTimes: \n'
    msg += 'ArriveTimes: 01\n'##{}\n'.format(getTime())
    print('[MSG]',msg)
    for port in NEIGHBOUR:
        address = (SERVER,port)
        udp.sendto(msg.encode(FORMAT),address)
        print(f"[UDP SENT] Sent msg to port: {address}")



def getTime():
    currentDT = datetime.datetime.now()
    time = currentDT.strftime("%H:%M")
    return convertTime(time)

def convertTime(hhmm):
    time = hhmm.split(':')
    return (60*(int(time[0])))+int(time[1])
    
def unconvertTime(inti):
    hours = int(int(inti)/60)
    minutes = int(inti)%60
    time = datetime.datetime(2020, 5, 17,hours,minutes)
    return time.strftime("%H:%M")


def parseTimetable(STATIONNAME):
    f = open(f'tt-{STATIONNAME}','r')
    for line in f:
        #print(line)
        TIMETABLE.append(line.strip().split(','))
    f.close()
    for line in TIMETABLE[1:]:
        line[0] = convertTime(line[0])
        line[3] = convertTime(line[3])

def start():
    parseTimetable(STATIONNAME)
    print('[VARIBLES]:',STATIONNAME,TCPPORT,UDPPORT,NEIGHBOUR)
    # create tcp socket
    tcp = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    tcp.bind((SERVER, TCPPORT))
    tcp.listen(BACKLOG)
    print(f"[LISTENING] TCP is listening on {SERVER, TCPPORT}")

    # create udp socket
    udp = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    udp.bind((SERVER,UDPPORT))
    print(f"[LISTENING] UDPPORT is listening on {SERVER,UDPPORT}")

    input = [tcp,udp]
    output = []
    conn = ''
    while True:
        print("[READY]")
        inputready,outputready,exceptready = select.select(input,output,input)

        for s in inputready:
            if s == tcp:
                conn = TCP_handle_client(s,udp)
            elif s == udp:
                UDP_handle_client(s,udp,conn)
            else:
                print(f"unknown socket:{s}")
    

if __name__ == '__main__':
    print("[STARTING] server is starting...")
    STATIONNAME = sys.argv[1]
    TCPPORT = int(sys.argv[2])
    UDPPORT = int(sys.argv[3])
    NEIGHBOUR = list(map(int,sys.argv[4:]))
    
    start()