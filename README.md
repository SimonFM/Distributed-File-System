# Distributed File System
A distributed File System written in Scala. It currently only works for .txt files, but it could easily be modified in the future should someone wish to do so.
##Running DFS
To run my DFS, I used IntelliJ to run the separate servers on my local host. I was unable to gain access to OpenNebula in order to simulate my system in a real environment, so all the Host names and the Ports are hardcoded to work on the localhost. But the Logic should work the same, all that needs to be changed is the Host name and the Port associated with it.
- Directory Server (localhost, 8000)
- Client (localhost, 8000)  connects to directory server.
- File Servers(Nodes) (localhost, <Port of choosing>)  Allows users to enter their own port
- Replicated Nodes (localhost, ports(9000 to 9003))  these need to be hardcoded, as they only simulate replication.

To run the start.sh file on linux / mac, you will need to run each start.sh file in a separate terminal window (as the processes will consume one window)
###Flow of Start up
- Run Directory (please choose 8000 as a command line param)
- Run Nodes between (8080 -> maxPort(8082))
- Run ReplicatedNodes (9000 -> 9003)
- Run Client (enter desired Username)


##Infomation about it
Features I chose to implement were:

1. File System
  - All my files are stored on File Servers called Nodes and also on my replicated Nodes.
  - They are able to perfrom READ, WRTITE and DELETE operations.
2. Directory Service 
  - Tells the client which node the file is located on
  - Each unique file is mapped to a different file server (via round robin, this isn't the most ideal solution but it does ensure fairness as each node will be garunteed a file). Different files are distinguished by the their file name. So each file needs to have a unique name, this also means that files with the same name will not be on the same server.
  - A client is then also able to perform a lookup on a file. This returns where the file is saved (if it is saved at all).
3. Locking System
  - It is a basic locking system, when the user applies a WRITE, it first looks to see if anyone has a lock on that file (ie if they're writing to that file) and it will send a message back to the user saying that someone else has a lock on the file, otherwise if there isn't a lock the lock is given to the user and then their write is applied and they release the lock.
4. Caching
  - Initially I had my caching on the Directory Server, it is still there but It is not implemented by any service. My design in this was that the directory server could hold the last X number of files accessd so a user would not have to constantly go to the file servers every time they wanted to read / write a file. This then ended up being a bottle neck as multiple users accessing the same file(s) would end up reading and writing in a inconsisitent pattern. It also breaks up the idea of a distributed system, so i scrapped the idea.
  - Caching is done locally for each client, inside a folder called (cache) and that is where the files are written to and from. When a read is performed (or a write) the client asks the node it obtained the file from for the latest version (this is in a form of a timestamp of last modified) and if the client's is in anyway different, their version is then updated.

5. Replication
  All this part of the system does is perform immediate updates from the FileNodes, so all they contain is a back up of the last write performed.

##Flow Of Operation
1. Client.connect(Directory Server) 
2. location = DirectoryServer.LookUp(file) 
3. Client.connect(location)
4. Client.performOperation(file, contentsOfFile)
5. Client.close()


##API Commands:
### Delete a file
```Javascript
DELETE_FILE:\n
FILE_NAME:--test.txt\n
END;\n
```
---

### Write to a file
```javascript
WRITE_FILE:\n
FILE_NAME:--test.txt\n
CONTENTS:--'newThings To say'\n
END;


Sent Back
SAVED: fileName\n
END\n

or 
FAILURE;\n
\n
```
---

###Read the contents of a file
```javascript
-- Tested works\n
GET_FILE:\n
FILE_NAME:--test.txt\n
END;\n

--Transferring file data
FILE_CONTENTS:\n
CONTENTS: <contents>\n
END;\n
```
---

###Release lock on a file
```javascript
RELEASE:\n
FILENAME:--filename.txt\n
END;\n

SUCCESS;\n
NOPE;\n
```
---

###Search for File
```javascript
To Proxy / Node
SEARCH:\n
FILENAME:--test.txt\n
END;\n


From Node / to Client:\n
SEARCH:\n
FILEPATH:--NameOfServer:test.txt\n
END;\n
```
---
