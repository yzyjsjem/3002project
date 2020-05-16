#!/usr/bin/env python3
import socket
import select
import sys

HEADER = 64
SERVER = 'localhost'
FORMAT = 'utf-8'
BUFF_SIZE = 1024 # 1 KiB
BACKLOG = 5

Destination = ''


#conn.close()

def TCP_handle_client(s,udp):
    print(f"[NEW TCP CONNECTION]{s}")
    data =''
    conn , addr = s.accept()
    packet = conn.recv(BUFF_SIZE).decode(FORMAT)
    data += packet
    conn.setblocking(0) 
    print('[DESTINATION]', TCP_handle_data(data))
    UDP_send_all_adjacents(TCP_handle_data(data),udp)
    conn.send("HTTP/1.1 200 OK\n".encode(FORMAT))
    conn.close()

def TCP_handle_data(data):
    lines = data.splitlines()
    line = lines[0].split()
    destination = line[1].split('=')
    return destination[1]

def UDP_handle_client(s):
    
    data,addr = s.recvfrom(BUFF_SIZE)
    print(f"[NEW UDP CONNECTION]{addr}")
    msg = data.decode(FORMAT)
    print(msg)



def UDP_send_all_adjacents(destination,udp):
    msg = 'Are you {} ?'.format(destination).encode(FORMAT)
    for port in NEIGHBOUR:
        address = (SERVER,port)
        udp.sendto(msg,address)
        print(f"[UDP SENT] Sent /{msg}/ to port: {address}")

def start():
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
    while True:
        print("[READY]")
        inputready,outputready,exceptready = select.select(input,output,input)

        for s in inputready:
            if s == tcp:
                TCP_handle_client(s,udp)
            elif s == udp:
                UDP_handle_client(s)
            else:
                print(f"unknown socket:{s}")
    





if __name__ == '__main__':
    print("[STARTING] server is starting...")
    STATIONNAME = sys.argv[1]
    TCPPORT = int(sys.argv[2])
    UDPPORT = int(sys.argv[3])
    NEIGHBOUR = list(map(int,sys.argv[4:]))
    
    start()