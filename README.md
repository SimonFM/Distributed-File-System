# Distributed-File-System
A distributed File System written in Scala.

Features I chose to implement were:

1. File System (Bare Bones)

2. Directory Service (Tells the client where the files are)

3. Locking System (Very basic)

4. Replication (On the Node side)

5. Caching (Done in the Directory Service)


The Directory Server is connected to all the other NodeServers (who store all the files) and the client interacts with the Directory Server. So the Directory Server acts much like a proxy for the client, giving it the results it wants.

API Commands:

##########
# READS #
##########
-- Tested works

GET_FILE:\n

FILE_NAME:--test.txt\n

END;

--Transferring file data
FILE_CONTENTS:\n

CONTENTS: <contents>\n

END;
##########
# WRITES #
##########
-- Tested works

WRITE_FILE:\n

FILE_NAME:--test.txt\n

CONTENTS:--'newThings To say'\n

END;

SUCCESS -- Sent back

FAILURE -- error occured




// #JustDirectoryThings

LS -- Test works
