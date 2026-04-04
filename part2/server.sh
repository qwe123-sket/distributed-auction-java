#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

PID_DIR=".pids"
REG_PORT=1099
FE_MAIN="frontend.FrontEndServer"
REPLICA_MAIN="replica.ReplicaMain"

mkdir -p "$PID_DIR"

write_pid() {
  echo "$2" > "${PID_DIR}/$1.pid"
}

echo "Stopping old processes..."
pkill -x rmiregistry || true
pkill -f 'frontend\.FrontEndServer' || true
pkill -f 'replica\.ReplicaMain' || true
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
( mvn -q exec:java -Dexec.mainClass="${REPLICA_MAIN}" -Dexec.args="1" ) &
write_pid "replica1" $!
sleep 1
( mvn -q exec:java -Dexec.mainClass="${REPLICA_MAIN}" -Dexec.args="2" ) &
write_pid "replica2" $!
sleep 1
( mvn -q exec:java -Dexec.mainClass="${REPLICA_MAIN}" -Dexec.args="3" ) &
write_pid "replica3" $!

echo "System ready within ~10 seconds."
echo "PID files in ${PID_DIR}."
