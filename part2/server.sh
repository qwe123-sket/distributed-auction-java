#!/usr/bin/env bash
set -euo pipefail

# Run relative paths from this script's directory
cd "$(dirname "${BASH_SOURCE[0]}")"

PID_DIR=".pids"
REG_PORT=1099
FE_MAIN="frontend.FrontEndServer"
REPLICA_MAIN="replica.ReplicaMain"

mkdir -p "$PID_DIR"

write_pid() {  # write_pid <name> <pid>
  echo "$2" > "${PID_DIR}/$1.pid"
}

echo "Stopping old processes..."
# Best-effort stop anything left over
pkill -x rmiregistry || true
pkill -f 'frontend\.FrontEndServer' || true
pkill -f 'replica\.ReplicaMain' || true
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
sleep 1

echo "Starting FrontEnd..."
( mvn -q exec:java -Dexec.mainClass="${FE_MAIN}" ) &
write_pid "frontend" $!
sleep 2

echo "Starting replicas..."
for id in 1 2 3; do
  ( mvn -q exec:java -Dexec.mainClass="${REPLICA_MAIN}" -Dexec.args="${id}" ) &
  write_pid "replica${id}" $!
  sleep 1
done

echo "Part 2 ready within ~7 seconds."
echo "Client: mvn -q exec:java -Dexec.mainClass=client.AuctionClient"
echo "PID files in ${PID_DIR}."
