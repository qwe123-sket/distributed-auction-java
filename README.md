# 分布式拍卖系统（Java）

| 模块 | 说明 |
|------|------|
| Part 1 | 单机 RMI 拍卖服务，gRPC 对外接口 |
| Part 2 | 多副本与 Sequencer 复制，gRPC 前端（含 `FrontEndAdmin`） |

开发与运行环境以 **Windows** 为准。Part 1 与 Part 2 **不可同时启动**（共用 **1099** / **50055**）。

## 仓库结构

| 路径 | 内容 |
|------|------|
| `part1/` | 单节点：`server.ps1`、`client.ps1`、`client.cmd` |
| `part2/` | 多副本：同上 |
| 根目录 `pom.xml` | Maven 聚合（可选，便于 IDE 导入） |

## 环境要求

- JDK 17+
- Maven 3.x（`mvn` 在 `PATH` 中）
- 建议设置 `JAVA_HOME`（`rmiregistry` 依赖）

IDE 中若 protobuf 生成类报错：在对应 `part` 目录执行 `mvn compile`，或在 IDE 中重新加载 Maven 项目。

## 构建

```powershell
cd part1; mvn -q package
cd ..\part2; mvn -q package
```

已配置聚合 POM 时可在仓库根目录执行 `mvn -q package`。

## 启动服务

**根目录：**

```powershell
cd <仓库根目录>
.\start.ps1 -Part 1
```

Part 2 将参数改为 `-Part 2`。

**子目录：**

```powershell
cd part1
.\server.ps1
```

客户端在**另一终端**、同一 `part` 目录下执行 `.\client.ps1`；亦可使用 `client.cmd`（CMD）。Part 2 将路径中的 `part1` 改为 `part2`。

PowerShell 下若直接使用 Maven 启动客户端，`-D` 参数需加引号：

```powershell
mvn exec:java "-Dexec.mainClass=client.AuctionClient"
```

## 验收输出

`client.AuctionClient` 为自动化用例，无交互输入。服务端保持运行后执行客户端，以**标准输出**判定：

- 各检查点须为 `[PASS]`，不得出现 `[FAIL]`。
- 末尾须出现 `AuctionClient` 打印的 Summary 行（以 `--- Summary:` 开头）。

上述条件满足即视为该次运行通过。Part 1 与 Part 2 使用同一客户端逻辑，验收标准一致；Part 2 仅后端拓扑不同。

## License

[MIT License](LICENSE)
