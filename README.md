# 分布式拍卖系统（Java）

## 提交结构（与课程要求对齐）

- 最终提交为 **一个 zip**，内含两个顶层目录：**`part1/`**、**`part2/`**。
- 每个 `part` 目录**根下**须有 **`server.sh`**：负责启动 **`rmiregistry`**、**前端**，以及
  - **Part 1**：单台后端服务；
  - **Part 2**：**至少 3 个**副本进程。
- 初始化应在约 **10 秒内**完成；实验室环境需能通过 **`./server.sh`** 正常跑通（需本机已安装 JDK、Maven，且 `mvn` 在 `PATH` 中）。

根目录 **`pom.xml`** 为 Maven 聚合，**仅便于本地 IDE 导入**，若课程要求 zip 内只含 `part1` 与 `part2`，打包时可按说明排除根目录文件。

| 目录 | 说明 |
|------|------|
| `part1/` | 单机 RMI + gRPC 前端；`server.sh`、`server.ps1`（Windows 可选）、`client.ps1` |
| `part2/` | 多副本复制；同上 |

**约束**：不得修改课程提供的 **`auction.proto`** 及指定骨架接口/类的约定（以作业说明为准）。

Part 1 与 Part 2 **不要同时运行**（共用端口 **1099**、**50055**）。

---

## 环境

- JDK 17+
- Maven 3.x（`mvn` 在 `PATH`）
- 建议设置 `JAVA_HOME`

若 IDE 中 proto 生成类报错：在对应 `part` 下执行 `mvn compile`，或重新加载 Maven 项目。

---

## 构建

```bash
cd part1 && mvn -q package
cd ../part2 && mvn -q package
```

已使用根目录聚合 POM 时，可在仓库根执行：`mvn -q package`。

---

## 运行（实验室 / Linux，课程要求）

在对应目录赋予执行权限后启动：

```bash
cd part1   # 或 part2
chmod +x server.sh
./server.sh
```

另开终端，在同一 `part` 目录下运行客户端（需已 `mvn package`）：

```bash
mvn -q exec:java -Dexec.mainClass=client.AuctionClient
```

---

## 运行（Windows，可选）

本机调试可在 `part1` 或 `part2` 下使用：

```powershell
.\server.ps1
```

另一终端同目录：

```powershell
.\client.ps1
```

PowerShell 下手写 Maven 时，`-D` 参数需加引号，例如：

```powershell
mvn exec:java "-Dexec.mainClass=client.AuctionClient"
```

---

## 验收

`client.AuctionClient` 为自动化用例。服务端保持运行后执行客户端，以标准输出为准：

- 各项为 `[PASS]`，不得出现 `[FAIL]`；
- 末尾出现以 `--- Summary:` 开头的 Summary 行。

Part 1 与 Part 2 客户端逻辑一致，验收标准相同。

---

## License

[MIT License](LICENSE)
