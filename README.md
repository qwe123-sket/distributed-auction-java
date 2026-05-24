# 分布式拍卖系统（Java）

Java 17 实现的在线拍卖服务：客户端通过 **gRPC** 访问前端，后端在 Part 2 中通过 **RMI** 在多个副本之间复制写操作。

| 目录 | 说明 |
|------|------|
| `part1/` | 单机 RMI 后端 + gRPC 前端 |
| `part2/` | 多副本（≥3）+ 日志复制与 Sequencer |
| `pom.xml` | 根目录 Maven 聚合，便于 IDE 导入 |

Part 1 与 Part 2 **不要同时运行**（共用端口 **1099**、**50055**）。

## 架构（Part 2）

```
Client (gRPC) → FrontEnd → Replica × N (RMI)
```

写操作由当前 **Sequencer** 分配序号，经 `propose`、多数派确认、`commit` 后，各副本按相同顺序 `apply` 到状态机。Leader 不可用时，FrontEnd 会重选 Sequencer 并重试读写。

## 环境

- JDK 17+
- Maven 3.x（`mvn` 在 `PATH`）
- 建议设置 `JAVA_HOME`

Proto 生成类报错时，在对应 `part` 下执行 `mvn compile`。

## 构建

```bash
cd part1 && mvn -q package
cd ../part2 && mvn -q package
```

或在仓库根目录：`mvn -q package`

## 运行（Linux / macOS）

```bash
cd part1   # 或 part2
chmod +x server.sh
./server.sh
```

另开终端：

```bash
mvn -q exec:java -Dexec.mainClass=client.AuctionClient
```

## 运行（Windows）

```powershell
cd part1   # 或 part2
.\server.ps1
```

另一终端：`.\client.ps1`

手动 Maven 时 `-D` 需加引号：

```powershell
mvn exec:java "-Dexec.mainClass=client.AuctionClient"
```

## 接口

| RPC | 说明 |
|-----|------|
| `register` | 注册，返回 `user_id` |
| `newAuction` | 上架拍品 |
| `bid` | 出价 |
| `closeAuction` | 物主结拍 |
| `listItems` / `getSpec` | 列表与查询 |

## 验收

`client.AuctionClient` 为自动化用例。服务端运行后执行客户端：

- 各项为 `[PASS]`，无 `[FAIL]`
- 末尾有 `--- Summary:` 行

Part 1 与 Part 2 客户端逻辑相同。

## License

[MIT License](LICENSE)
