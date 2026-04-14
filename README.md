# 分布式拍卖系统（Java）

**Part 1**：单机 RMI 拍卖服务 + gRPC 前端。  
**Part 2**：多副本 + Sequencer 复制 + gRPC 前端（`FrontEndAdmin`）。

本仓库按 **Windows + PowerShell** 使用方式整理；同一时刻只运行 Part 1 **或** Part 2（均占用端口 **1099**、**50055**）。

## 目录

| 目录 | 说明 |
|------|------|
| `part1/` | 单服务器 + `server.ps1` / `client.ps1` |
| `part2/` | 多副本 + `server.ps1` / `client.ps1` |
| `pom.xml` | 可选：根目录 Maven 聚合，便于 IDE 一次导入 |

## 环境

- JDK 17+
- Maven 3.x（`mvn` 在 PATH）
- 建议配置 `JAVA_HOME`（便于找到 `rmiregistry.exe`）

首次用 IDE 打开若 proto 相关类标红：在对应目录执行 `mvn compile`，或在 IDE 中 **Maven → Reload project**。

## 运行（Windows）

**方式 A — 仓库根目录：**

```powershell
cd <本仓库根目录>
.\start.ps1 -Part 1
```

或 `.\start.ps1 -Part 2`。

**方式 B — 进入子目录：**

```powershell
cd part1
.\server.ps1
```

另开终端（仍在 `part1`）：

```powershell
.\client.ps1
```

Part 2 将 `part1` 换成 `part2` 即可。也可用 `client.cmd`（CMD）代替 `client.ps1`。

PowerShell 下若需手写 Maven 客户端命令，**务必给 `-D` 加引号**：

```powershell
mvn exec:java "-Dexec.mainClass=client.AuctionClient"
```

## 构建

```powershell
cd part1; mvn -q package
cd ..\part2; mvn -q package
```

或在根目录（若已导入聚合 POM）：`mvn -q package`。

## 演示流程（给同学看：怎么跑、怎样算成功）

下面以 **Part 1** 为例；**Part 2** 步骤相同，只需把目录里的 `part1` 换成 `part2`，**不要**同时开两套服务（端口冲突）。

### 1. 准备

- 已安装 **JDK 17+**、**Maven**，`java` / `mvn` 在 PATH；建议设置 **`JAVA_HOME`**。
- 关闭之前占用的 **1099**、**50055** 进程（或先关掉上次跑过的服务端窗口）。

### 2. 终端 A：启动服务端

在仓库根目录执行其一：

```powershell
cd <本仓库根目录>
.\start.ps1 -Part 1
```

或在 `part1` 目录：

```powershell
cd part1
.\server.ps1
```

**说明**：脚本会编译并启动 **RMI 注册表 + 拍卖后端 + gRPC 前端**。窗口里应看到构建完成、进程已启动等日志；**请保持该窗口不关**。

**若这里报错或立刻退出**：先看是否端口被占用、JDK/Maven 是否可用，再对照本文「构建」一节在 `part1` 下执行 `mvn -q package` 看编译是否通过。

### 3. 终端 B：运行自动演示客户端

**新开**一个 PowerShell，进入同一套 Part 的目录：

```powershell
cd <本仓库根目录>\part1
.\client.ps1
```

（Part 2 则进入 `part2` 再执行 `.\client.ps1`，或用 `client.cmd`。）

客户端会**自动**完成：注册用户 → 创建拍卖 → 出价 → 列表与查询 → 结拍 → 若干边界情况，**无需手动输入**。

### 4. 怎样算「试验成功」

以 **客户端输出** 为准（服务端只要没崩即可）：

1. **每一行自检**应以 **`[PASS]`** 开头，不应出现 **`[FAIL]`**。  
   自检项会覆盖：三个用户 ID 互不相同、创建拍卖、非法用户、出价高低、列表与 `getSpec`、结拍与权限、重复结拍、不存在商品等。

2. 中间会打印类似：
   - `Users -> alice=... bob=... carol=...`
   - `Auctions -> item1=... item2=...`
   - `Bids on item1 -> ...`
   - `listItems count=...`
   - `getSpec(item1) highestBid=150 reserve=100`
   - `close item1 by alice -> winner=... price=150`

3. **最后一行**应出现英文提示（大意：上面所有 `[PASS]` 表示服务端行为正确）：

   ```text
   --- Summary: exercise register/newAuction/bid/list/getSpec/close + edge cases; all [PASS] above should print for a correct server. ---
   ```

**结论**：**全程只有 `[PASS]`、没有 `[FAIL]`，且出现上述 Summary 行，即演示成功。**  
若出现 `[FAIL]`，说明对应逻辑与作业要求不一致，需对照实现排查。

**Part 1 与 Part 2**：客户端脚本相同，**成功判据相同**；Part 2 的区别在后台多副本与复制，同学演示时仍看客户端是否**全部为 `[PASS]`** 即可。

## License

自行填写（若公开到 GitHub）。
