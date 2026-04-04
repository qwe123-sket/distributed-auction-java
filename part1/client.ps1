# Run gRPC client (PowerShell: quote -D... or Maven breaks)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
& mvn -q exec:java "-Dexec.mainClass=client.AuctionClient"
