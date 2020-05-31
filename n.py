import sys
import time
import socket
import os
import threading

Name = sys.argv[1]
Tcport = int(sys.argv[2])
Udport = int(sys.argv[3])
Neinfo = list(sys.argv[4:])
nextstopl = []
stopinfo = []
routine=''
findway=False
finalway=''
ath = time.localtime().tm_hour
atm = time.localtime().tm_min
print(ath, atm)

def getns():
        f = open('tt-'+Name)
        line = f.readlines()
        newline = line[1:]
        for line in newline:
           line=line.strip('\n')
           part = line.split(',')
           time = part[0]
           timepiece = time.split(':')
           timeh = int(timepiece[0])
           timem = int(timepiece[1])
           nextstop = part[-1]
           if timeh > ath or (timeh == ath and timem >= atm):
              stopinfo.append(line)
              nextstopl.append(nextstop)
        f.close()


def tcp():
      s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
      s.bind(('0.0.0.0',Tcport))   
      s.listen(5)
      print ('Server start at: %s:%s' %("0.0.0.0", Tcport))
      print ('wait for connection...')
      while True:
         conn, addr = s.accept()
         print ('Connected by ', addr)
         while True:
            data = conn.recv(1024)
            data1=str(data)
            if "b'GET /?to=" in data1:
               print('get the order')
               browser2 = data1.split("to=")
               browser3 = browser2[1]
               browser4 = browser3.split(" HTTP/1.1")
               end = browser4[0]
               print(end)
            else:
               cmd ="HTTP/1.1 404 Not Found \n" + "Content-Type: text/html\n" + "Content-Length: 29" + "\n\n"+ "<h1>Request Not Correct!</h1>"
               conn.send(cmd.encode('utf-8'))
               continue
            getns()
            for i in nextstopl:
               if i==end:
                  finalway=stopinfo[i]
                  cmd="HTTP/1.1 200 ok \n" + "Content-Type: text/html\n" + "Content-Length: "+ len(finalway) + "\n\n" + finalway
                  conn.send(cmd.encode('utf-8'))#directway
                  s.close()
               else:
                  sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
                  sock.bind(('0.0.0.0',9999))
                  for i in range(len(nextstopl)):
                     routine=str(Udport)+','+end+','+stopinfo[i]
                     answer=bytes(routine,encoding='utf-8')
                     for j in Neinfo:
                        k=int(j)
                        sock.sendto(answer,('0.0.0.0',k))
                  sock.close()
            while not findway:
               #print('im waiting')
               zhenyu='genius'
            cmd = "HTTP/1.1 200 ok \n" + "Content-Type: text/html\n" + "Content-Length: "+ len(finalway) + "\n\n" + finalway
            conn.send(cmd.encode('utf-8'))#return correct answer
            s.close()
            

def udp():
   sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
   sock.bind(('0.0.0.0',Udport))
   while True:
    data, addr = sock.recvfrom(1024)
    routine=bytes.decode(data)
    arrive=routine.split(",")
    if ':' in arrive[0]:
       finalway=finalway+routine
       findway=True
       print('find the way') 
    else:
       arrivetime = arrive[len(arrive) - 2]#get the arrivetime
       arrivestop = arrive[len(arrive) - 1]
       print('////'+arrivestop+'/////')
       end = arrive[1]# get the end information
       at = arrivetime.split(":")
       ath = int(at[0])
       atm = int(at[1])
    getns()
    getend=False
    for i in nextstopl:
       if i==end:
          getend=True
    check=routine.split(',')
    havepassed=False
    for i in check:
       if i==Name:
          havepassed=True
    if havepassed:
       havepassed=False
       continue
    elif arrivestop!=Name:
       for i in Neinfo:
          k=int(i)
          answer=bytes(routine,encoding='utf-8')
          sock.sendto(answer,('0.0.0.0',k))
    elif arrivestop==Name and getend:
       getend=False
       for i in range(len(nextstopl)):
          if nextstopl[i]==end:
             routine+=stopinfo[i]
             break
       split=routine.split(',')
       aim=int(split[0])
       finalway=''
       for i in split[2:]:
          finalway+=i
       answer=bytes(finalway,encoding='utf-8')
       sock.sendto(answer,('0.0.0.0',aim))
       print('ready to finish')
    else:
       for i in range(len(nextstopl)):
          routine+=stopinfo
          answer=bytes(routine,encoding='utf-8')
          for j in Neinfo:
             k=int(j)
             sock.sendto(answer,('0.0.0.0',k))
   sock.close()

thread1 = threading.Thread(target=tcp)
thread2 = threading.Thread(target=udp)

thread1.start()
thread2.start()
thread1.join()
thread2.join()          
       
          



    






