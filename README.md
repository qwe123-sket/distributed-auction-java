# Distributed Auction System (Java)

Course project: **single-server auction (Part 1)** and **replicated, sequencer-based auction (Part 2)**. Clients use **gRPC**; the front-end talks to backends via **Java RMI**.

## Layout

| Directory | Description |
|-----------|-------------|
| `part1/` | One RMI auction server + gRPC front-end |
| `part2/` | Multiple replicas, two-phase replication + gRPC front-end with `FrontEndAdmin` |

Root `pom.xml` is an optional Maven aggregator for local IDE use; each part still builds with `mvn package` inside its folder.

## Run (Linux / lab)

```bash
cd part1 && ./server.sh   # or cd part2 && ./server.sh
# Client (same directory):
mvn exec:java -Dexec.mainClass=client.AuctionClient
```

## Run (Windows)

```powershell
cd part1   # or part2
.\server.ps1
.\client.ps1
```

Use **either** Part 1 **or** Part 2 at a time (same ports `1099` / `50055`).

## Requirements

- JDK 17+
- Maven 3.x

## Git / GitHub

First time in this repo, set your author (appears on commits; use your GitHub noreply email if you prefer):

```bash
git config user.name "Your Name"
git config user.email "your-email@example.com"
git commit -m "Initial commit: distributed auction system (gRPC + RMI, Part1 and Part2)"
```

Suggested repository name: **`distributed-auction-java`**. Create an **empty** repo under your account, then:

```bash
git remote add origin https://github.com/qwe123-sket/distributed-auction-java.git
git push -u origin main
```

(Remote may already be set in this clone; if `git remote -v` shows it, only run `git push -u origin main`.)

## License

Specify your license here if you publish publicly.
