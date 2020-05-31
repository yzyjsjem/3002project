import sys
import time
import socket
import os
import threading

Name = sys.argv[1]#station name
Tcport = int(sys.argv[2])
Udport = int(sys.argv[3])
Neinfo = list(sys.argv[4:])
nextstopl = []#all next stop stop will store in here 
stopinfo = []#all specific timetable will store in here
routine='' 
findway=False
finalway=''
ath = time.localtime().tm_hour#get the hour of local time
atm = time.localtime().tm_min#get the minute of local time 
print(ath, atm)

def getns():#read the timetable to fufill the stopinfo and nextstopl
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
           global ath
           global atm
           if timeh > ath or (timeh == ath and timem >= atm):
              stopinfo.append(line)
              nextstopl.append(nextstop)
        f.close()


def tcp():#tcp communication/including send the first udp information if get the order 
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
            data1=str(data,encoding = "utf8")
            if "/?to=" in data1:#check if this is an order for destination
               print('get the order')
               browser2 = data1.split("to=")
               browser3 = browser2[1]
               browser4 = browser3.split(" HTTP/1.1")
               end = browser4[0]#find the destination
               print(end)
            else:#for example no destination entered
               cmd ="HTTP/1.1 404 Not Found \n" + "Content-Type: text/html\n" + "Content-Length: 29" + "\n\n"+ "<h1>Request Not Correct!</h1>"
               conn.send(cmd.encode('utf-8'))
               continue
            getns()#read the timetable 
            for i in nextstopl:#case0: if the destination is in the nextstop
               if i==end:
                  directway=stopinfo[i]
                  cmd="HTTP/1.1 200 ok \n" + "Content-Type: text/html\n" + "Content-Length: " \
                    + (str)(len(directway)) + "\n\n" + directway
                  conn.send(cmd.encode('utf-8'))#directway
                  s.close()
               else:
                  sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
                  sock.bind(('0.0.0.0',9999))
                  for i in range(len(nextstopl)):
                     routine=str(Udport)+','+end+','+stopinfo[i]#store the initial udport and destination in the information
                     answer=bytes(routine,encoding='utf-8')
                     for j in Neinfo:
                        k=int(j)
                        sock.sendto(answer,('0.0.0.0',k))
                  sock.close()
            while not findway: #if fail to find a way, stuck in the loop and wait for 5minute, return no way
               time.sleep(5)
               noway='there is no way'
               cmd = "HTTP/1.1 200 ok \n" + "Content-Type: text/html\n" + "Content-Length: " \
                    + (str)(len(noway)) + "\n\n" + noway
               conn.send(cmd.encode('utf-8'))#there is no way
               s.close()

             #if find a way, tell the browser the way i get  
            cmd = "HTTP/1.1 200 ok \n" + "Content-Type: text/html\n" + "Content-Length: " \
                    + (str)(len(finalway)) + "\n\n" + finalway
            conn.send(cmd.encode('utf-8'))#return correct answer
            s.close()
            

def udp():#udp communication
   sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
   sock.bind(('0.0.0.0',Udport))
   while True:
    data, addr = sock.recvfrom(1024)
    routine=bytes.decode(data)
    arrive=routine.split(",")
    if ':' in arrive[0]:#the finalway information will start with a time 
       global finalway
       finalway=routine
       global findway
       findway=True
       print('find the way') 
       continue
    else:
       arrivetime = arrive[len(arrive) - 2]#get the arrivetime
       arrivestop = arrive[len(arrive) - 1]
       des = arrive[1]# get the destination information
       at = arrivetime.split(":")
       global ath
       global atm
       ath = int(at[0])
       atm = int(at[1])
       getns()#read the timetable again due to the change in ath and atm
       getend=False#if this station could go to the destination
       for i in nextstopl:
          if i==des:
             getend=True
       check=routine.split(',')
       havepassed=False#check if the station have been passed before in the routine
       for i in check[2:-1]:
          if i==Name:
             havepassed=True
       if havepassed:#case1: the station have been passed before, don't use this inforamtion
          havepassed=False
          continue
       elif arrivestop!=Name:#case2: if this routine is not for the station right now, pass it to neighour udport
          for i in Neinfo:
             k=int(i)
             answer=bytes(routine,encoding='utf-8')
             sock.sendto(answer,('0.0.0.0',k))
       elif arrivestop==Name and getend:#case3: if this routine is for this station and can go to the destination
          getend=False
          for i in range(len(nextstopl)):
             if nextstopl[i]==des:
                routine+=stopinfo[i]
                break
          split=routine.split(',')
          aim=int(split[0])
          finalway=''
          for i in split[2:]:#take the first two information out and send this to the inital station
             finalway+=i
          answer=bytes(finalway,encoding='utf-8')
          sock.sendto(answer,('0.0.0.0',aim))
          print('ready to finish')
       else:
          for i in range(len(nextstopl)):#case4: this routine if for this station but can't go to the destination
             routine+=stopinfo[i]
             answer=bytes(routine,encoding='utf-8')
             for j in Neinfo:
                k=int(j)
                sock.sendto(answer,('0.0.0.0',k))#add my routine and send to neighours
   sock.close()

thread1 = threading.Thread(target=tcp)
thread2 = threading.Thread(target=udp)

thread1.start()
thread2.start()
thread1.join()
thread2.join()          
       
          



    






