#!/bin/sh
clear

#compile the server & client
scalac DirectoryServer.scala
scalac ReplicatedServer.scala
scalac Node.scala
scalac Client.scala

#compile the extra classes
scalac FileMap.scala
scalac Cache.scala
scalac FileManager.scala