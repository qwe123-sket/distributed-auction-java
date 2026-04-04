#!/usr/bin/env bash
set -euo pipefail

# Run relative paths from this script's directory
cd "$(dirname "${BASH_SOURCE[0]}")"

PID_DIR=".pids"
REG_PORT=1099
FE_MAIN="frontend.FrontEndServer"
SV_MAIN="server.ServerMain"

mkdir -p "$PID_DIR"

write_pid() {  # write_pid <name> <pid>
  echo "$2" > "${PID_DIR}/$1.pid"
}

echo "Stopping old processes..."
# Best-effort stop anything left over
pkill -x rmiregistry || true
pkill -f 'server\.ServerMain' || true
pkill -f 'frontend\.FrontEndServer' || true
# Also stop anything from prior .pids
if [ -d "$PID_DIR" ]; then
  for f in "$PID_DIR"/*.pid; do
    [ -f "$f" ] || continue
    pid=$(cat "$f")
    kill "$pid" 2>/dev/null || true
    rm -f "$f"
  done
fi
sleep 1

echo "Building project..."
mvn -q clean package

echo "Starting rmiregistry..."
rmiregistry -J-Djava.class.path=target/classes "$REG_PORT" &
write_pid "rmiregistry" $!
sleep 1   # give registry a moment

echo "Starting AuctionServer..."
( mvn -q exec:java -Dexec.mainClass="${SV_MAIN}" ) &
write_pid "server" $!
sleep 1   # ensure server binds before FE connects

echo "Starting FrontEnd..."
( mvn -q exec:java -Dexec.mainClass="${FE_MAIN}" ) &
write_pid "frontend" $!

echo "System ready within ~5 seconds."
echo "PID files in ${PID_DIR}."
