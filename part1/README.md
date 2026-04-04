# SCC.331 Coursework — Part 1 (Single-Server)

This project runs a FrontEnd (gRPC) and a single Auction Server (RMI).  
Processes are started via `server.sh` and tracked with PID files under `.pids/` so they can be cleaned up safely.

## Prerequisites
- Java 17+
- Maven 3.8+
- Linux shell
- Port **1099** free for `rmiregistry` (RMI) and port **50055** free for front-end

## Build & Run

```bash
# from the project root
./server.sh
```

What it does:


- mvn clean package


- starts rmiregistry on the default port 1099


- starts server.ServerMain (Auction Server)


- starts frontend.FrontEndServer (gRPC FrontEnd)


## Stop and Clean processes
Using make:

```bash
make clean
```

This:

- reads PIDs from .pids/ and sends TERM (then KILL if needed),

- removes PID files and runs mvn clean,

- best-effort pkill fallback for rmiregistry, server.ServerMain, frontend.FrontEndServer.

# Troubleshooting

- “Address already in use” / rmiregistry won’t start
Another rmiregistry is running. Try pkill rmiregistry or do 

```bash
make clean
```
