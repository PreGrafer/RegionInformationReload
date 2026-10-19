# 3.1 验证记录

最近验证日期：2026-10-19。所有命令在隔离的构建/测试目录运行，没有启动或修改 Minecraft 生产服务器。

整体检查完成后，共 47 项测试在 1.9、1.12.2 和 1.13.2 API 下分别通过；1.21.11 和 26.2 API 源码编译通过。下表所有构建、直接符号检查与 JAR 审计均于本次检查重新执行。

## 完成的检查

| API / 环境 | 结果 |
| --- | --- |
| Spigot 1.9 API / JDK 17 | `mvn verify`，47 项测试通过，生成发布候选 JAR |
| Spigot 1.12.2 API / JDK 17 | 47 项测试通过 |
| Spigot 1.13.2 API / JDK 17 | 47 项测试通过 |
| Spigot 1.21.11 API / JDK 25 | 源码编译通过；1.9 基线字节码的 115 个直接 Bukkit 符号解析通过 |
| Spigot 26.2 API / JDK 25 | 源码编译通过；1.9 基线字节码的 115 个直接 Bukkit 符号解析通过 |
| Spigot 1.9 API / 实际 Java 8 运行时 | 115 个直接 Bukkit 符号解析通过；移除 PlaceholderAPI 类路径后主类校验通过，构造执行到 Bukkit 的 PluginClassLoader 检查 |
| JAR 审计 | 33 个插件 class 全部为 major version 52（Java 8）；未打包 Bukkit、PlaceholderAPI 或测试类；plugin.yml 版本替换成功 |
| GitHub Actions | YAML 与矩阵、权限、上传条件的结构检查通过；实际 GitHub 运行待推送后确认 |
| Git 差异 | `git diff --check` 无空白错误 |

源码和测试均使用 `pregrafer.rir` 根包；全部 Java 包声明与物理路径匹配。最新 JAR 的 33 个 class 全部位于 `pregrafer/rir/`，没有旧根包或旧根包引用；`plugin.yml` 指向 `pregrafer.rir.RirPlugin`，对应 class 存在。构建产物旁生成 `.jar.sha256` 校验文件。调度设计见 [重构与设计说明](REFACTORING.md)，本轮改进与后续方向见 [整体检查报告](AUDIT-2026-10-19.md)。

本轮基线产物大小为 90,692 字节，SHA-256 为 `7d01ffbd23584959b52623f1f2fa27cc5b3c902991f3f28cfb2c7125407eee22`。此值仅对应本次本地构建，其他时间的构建可能因归档元数据而不同。

接口解析检查覆盖直接方法/字段以及 class/interface 调用类型，**不覆盖反射分支、服务端初始化、网络包、客户端表现或第三方插件组合**。没有把 API 测试标记为真实服务器测试，也不表示逐一实测了中间每个 Minecraft 小版本。兼容目标覆盖常规 Spigot/Paper，不包括 Folia。

## 回归覆盖

本轮新增的 11 项整体检查回归：

- 异步保存失败后，同步保存与正常停服仍可持久化最新记录。
- 同一区域内的周期传送不推迟其他独立周期动作。
- 严格拒绝错误 YAML 类型、重复数值周期和无效旧配置节点。
- `/rir validate` 不改变文件、运行中配置或迁移状态；报错后保留旧配置。
- UUID 成员关系不受玩家名变化影响；查询返回只读快照；发布后的区域定义不可再修改。
- 候选列表可复用，局部与全世界候选合并保持顺序和只读语义。
- 持续异常按指数退避，移动标记无法绕过退避，日志有频率上限；停用清除会话。
- PAPI 查询与插件停用交错时，不会因二次读取追踪器出现空指针。

此前调度优化新增的 14 项队列与调度回归：

- 空闲且未到期的 tick 不调用玩家 API；只移动一名玩家时，不访问其他未到期玩家；重复标记合并为一次确认。
- 40 名玩家初始化后的一个完整默认校准周期内，每 tick 校准 2 人，每名玩家仍按 20 tick 周期执行。这是调度工作量验证，不是 CPU/TPS 压测。
- 无移动事件的位置变化、原地权限撤销在一个配置校准周期内被处理。
- 群系与停留动作同 tick 到期均执行；25/37 tick 周期、区域重叠变化及离开区域保持正确计时。
- 指令移动产生的新标记下一 tick 确认；进入检查和到期指令中的重载会作废旧状态、旧动作及旧计划。
- 正在处理移动批次时退出并以同 UUID 重进，旧会话不再执行；退出后队列立即清空，不继续访问该玩家。
- 单个玩家更新抛出异常后延至下一 tick 重试，同批其他玩家继续执行，不在同 tick 重试循环。
- 队列按期限处理、稳定处理同 tick 项、前移/后移期限无重复；远期任务反复改期后取消不会留下旧项；3000 tick 随机调度与取消符合最早有效期限。

此前 22 项回归继续通过：

- 900 个随机形状区域、2000 个随机位置与全量扫描对比，包含负坐标、不同世界和超大区域。
- 10000 个按网格分散的区域，目标区块只需检查 1 个候选区域；这是候选数验证，不是 TPS/CPU 基准。
- 球形小数边界、圆柱顶部、不同世界、区域优先级、禁用区域和几何对象防御性复制。
- 重叠区域被拒绝时不提交任何新状态，不执行成功消息/奖励。
- 附属取消进入事件、其他插件取消外层移动、最终传送目标改变。
- 进入成功事件内可以读取完整的新成员关系；退出后清理区域成员与玩家缓存。
- 关闭提示依然保持区域追踪和指令执行；离开后停止周期动作。
- 25 tick 周期按到期 tick 执行；静止玩家不重复做几何查询。
- 重载不重放进入指令；消息中的传送命令中断原位置剩余动作，不提前消耗首次进入记录。
- PlaceholderAPI 使用已发布字符串快照，退出后清除。
- 命令创建后索引立即生效，重复 ID/跨世界选点无法覆盖原区域，删除要求确认词，补全按权限和前缀过滤。
- 旧区域自动迁移、无效重载保留旧数据、错误消息/半径/周期拒绝加载。
- 消息模板不修改调用方列表，也不把正文内的命令标记当作命令。
- BetterCommands 导入可重复合并、拒绝冲突；旧玩家名、UUID 首次记录和提示偏好在正常重启后保留。

## 复现

普通构建（JDK 17、Maven 3.9）：

```sh
mvn verify
```

本次受限 Windows 环境不允许 Mockito 自动附加代理，且虚拟临时目录不支持测试所需的原子重命名。验证时通过 Maven `argLine` 显式加载测试依赖中的 Byte Buddy agent，并把 `java.io.tmpdir` 指向工作区内的测试临时目录。未修改生产保存逻辑；普通开发环境仍优先使用上面的标准构建命令。

指定旧版 API 并使用独立输出目录：

```sh
mvn "-Dspigot.version=1.13.2-R0.1-SNAPSHOT" "-Drir.buildDirectory=target/api113" test
```

新版 API 源码检查（JDK 25，不运行需要真实注册表的现代 Biome mock）：

```sh
mvn "-Dspigot.version=26.2-R0.1-SNAPSHOT" "-Drir.buildDirectory=target/api26" "-Dmaven.test.skip=true" compile dependency:build-classpath "-Dmdep.outputFile=target/classpath26.txt"
```

使用 `tools/ApiLinkage.java` 检查 **原始 1.9 构建产物** 在目标 API 下的直接符号；不能改成只检查针对新版重新编译的 classes。该小工具复用测试依赖 Byte Buddy 内置的 ASM，不进入插件 JAR。

Windows PowerShell 示例（JDK 25 的 `java`/`javac` 已加入 PATH）：

```powershell
$rirApiClasspath = (Get-Content target/classpath26.txt -Raw).Trim()
javac --release 8 -cp $rirApiClasspath -d target/linkage tools/ApiLinkage.java
java -cp "target/linkage;$rirApiClasspath" ApiLinkage target/classes
```

真实上服验收项目在 [升级指南](UPGRADE-3.1.md#上服验收)。
