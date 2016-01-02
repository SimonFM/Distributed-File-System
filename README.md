# Distributed File System
A distributed File System written in Scala. It currently only works for .txt files, but it could easily be modified in the future should someone wish to do so.

##Infomation about it
Features I chose to implement were:

1. File System
  - All my files are stored on File Servers called Nodes and also on my replicated Nodes.
  - They are able to perfrom READ, WRTITE and DELETE operations.
2. Directory Service 
  - Tells the client which node the file is located on
  - Each unique file is mapped to a different file server (via round robin, this isn't the most ideal solution but it does ensure fairness as each node will be garunteed a file). Different files are distinguished by the their file name. So each file needs to have a unique name, this also means that files with the same name will not be on the same server.
3. Locking System
  - It is a basic locking system, when the user applies a WRITE, it first looks to see if anyone has a lock on that file (ie if they're writing to that file) and it will send a message back to the user saying that someone else has a lock on the file, otherwise if there isn't a lock the lock is given to the user and then their write is applied and they release the lock.
4. Caching
  - Initially I had my caching on the Directory Server, it is still there but It is not implemented by any service. My design in this was that the directory server could hold the last X number of files accessd so a user would not have to constantly go to the file servers every time they wanted to read / write a file. This then ended up being a bottle neck as multiple users accessing the same file(s) would end up reading and writing in a inconsisitent pattern. It also breaks up the idea of a distributed system, so i scrapped the idea.
  - Caching is done locally for each client, inside a folder called (cache) and that is where the files are written to and from. When a read is performed (or a write) the client asks the node it obtained the file from for the latest version (this is in a form of a timestamp of last modified) and if the client's is in anyway different, their version is then updated.


##Flow Of Operation
1. Client.connect(Directory Server) 
2. location = DirectoryServer.LookUp(file) 
3. Client.connect(location)
4. Client.performOperation(file, contentsOfFile)
5. Client.close()


##API Commands:
### Delete a file
```Javascript
DELETE_FILE:
FILE_NAME:--test.txt
END;
```
---

### Write to a file
```javascript
WRITE_FILE:\n
FILE_NAME:--test.txt\n
CONTENTS:--'newThings To say'\n
END;

SUCCESS -- Sent back
FAILURE -- error occured

SAVED: fileName
END
```
---

###Read the contents of a file
```javascript
-- Tested works
GET_FILE:\n
FILE_NAME:--test.txt\n
END;

--Transferring file data
FILE_CONTENTS:\n
CONTENTS: <contents>\n
END;
```
---

###Release lock on a file
```javascript
RELEASE:
FILENAME:--filename.txt
END;

SUCCESS;
NOPE;
```
---

###Search for File
```javascript
To Proxy / Node
SEARCH:
FILENAME:--test.txt
END;


From Node / to Client:
SEARCH:
FILEPATH:--NameOfServer:test.txt
END;
```
---
