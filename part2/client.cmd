@echo off
cd /d "%~dp0"
mvn -q exec:java -Dexec.mainClass=client.AuctionClient
