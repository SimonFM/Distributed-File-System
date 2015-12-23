# Distributed-File-System
A distributed File System written in Scala.

Features I chose to implement were:

1. File System
... READ
... WRITE
... DELETE
2. Directory Service 
...Tells the client where the files are
3. Locking System 
...If a user has accessed a file it is locked to them, they must release the lock themeselves
4. Caching
...Done in the Directory Service


The Directory Server is connected to all the other NodeServers (who store all the files) and the client interacts with the Directory Server. So the Directory Server acts much like a proxy for the client, giving it the results it wants.

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
