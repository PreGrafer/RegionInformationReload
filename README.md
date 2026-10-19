# RegionInformationReload 3.1

区域与生物群系提示插件，支持立方体、球体、圆柱体、多区域重叠、进入权限、物品交互限制，以及首次进入/停留周期指令。原 RIRBetterCommands 能力已整合。

## 兼容目标与验证

兼容目标为 **Spigot / Paper 1.9–26.2**，使用 Spigot 1.9 API 编译，输出 Java 8 字节码。26.2 是已核对的 API 版本上限；后续版本仍需运行兼容检查，不预先承诺未来接口不会变化。

- 不依赖 NMS 或 CraftBukkit 版本包名，不打包服务端 API、PlaceholderAPI 或测试库。
- 保留 `api-version: '1.13'`：1.9 的描述文件解析器忽略该未知字段，现代服务端采用现代材质语义，避免开启整套 legacy material 转换。
- 标题时长、ActionBar、粒子名与新旧生物群系接口由兼容层处理。没有可用 ActionBar API 的旧服务端会回退为聊天消息；不支持五参数标题的服务端使用默认标题时长。
- 运行 Java 版本应遵循服务端要求。插件输出 Java 8 字节码不意味着现代服务器可以使用 Java 8。
- PlaceholderAPI 可选，请安装适配自己服务端的版本。当前不声明 Folia 支持。

本次完成的构建、回归与直接 API 符号检查见 [验证记录](docs/VALIDATION.md)。尚未进行真实服务端启动和客户端联机验收。

依据：[Spigot 26.2 API](https://hub.spigotmc.org/javadocs/spigot/)、[现代 Biome 接口](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/block/Biome.html)、[Minecraft 26.2 发布说明](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-2)、[Paper plugin.yml](https://docs.papermc.io/paper/dev/plugin-yml/)。

## 位置检查与资源控制

移动事件跳过纯转头，按世界和区块获取候选区域，并在玩家未切换区块时复用候选列表。候选区域使用真实小数坐标判断；球体、圆柱体使用平方距离，避免开平方和方块级判断漏掉小边界。

每个区域最多展开 `Settings.index.maxChunksPerRegion` 个区块，总索引引用上限为 262144。超出限制的区域放在世界级候选列表中，以限制内存使用。大量互相覆盖或超大区域仍会增加单次查询成本。

全插件只有一个同步调度任务，每 tick 直接处理待确认玩家和到期玩家，不再复制、遍历全部玩家状态。移动标记去重并复用两个集合；每名玩家在到期队列中最多占一项，以校准、群系检查、停留动作的最近期限唤醒。退出或重载会删除旧计划，不积累作废条目。

区域校准分散到 `regionSpeed` 个 tick 内，每名玩家最多间隔一个配置周期校准一次。静止玩家跳过几何查询；没有到期动作时不扫描指令列表。玩家加入后的位置确认和群系初始化仍在下一 tick 完成，首次初始化不做分批延迟。玩家数据使用单独串行写入线程批量保存，异步线程不调用 Bukkit 世界或玩家 API。

`/rir stats` 显示查询数、候选测试数、索引条目数，以及 `scheduledPlayers`（队列玩家数）、`playerUpdates`（累计调度处理次数）、`calibrations`（累计校准次数），可观察索引和调度效果。计数不等同于 CPU/TPS 基准。粒子预览只发送给请求者，有固定粒子预算与 2 秒冷却。

## 安装和升级

将 `target/RegionInformationReload-3.1.0-SNAPSHOT.jar` 放入服务器 `plugins` 后启动。新安装默认没有活动区域，避免示例指令影响玩家。

| 文件 | 用途 |
| --- | --- |
| `config.yml` | 检测频率、默认提示、群系翻译等设置 |
| `regions.yml` | 区域几何、权限、消息与指令 |
| `players.yml` | UUID 首次进入历史、提示偏好、旧附属迁移状态 |

旧 `config.yml` 的 `Regions` 会自动备份并迁移。BetterCommands 可用 `/rir importbettercommands` 导入。[完整升级步骤与行为变化](docs/UPGRADE-3.1.md)。

## 命令

主命令 `/regioninformationreload`，别名 `/rir`。所有命令都提供按权限过滤的补全。

| 命令 | 用途 | 权限 |
| --- | --- | --- |
| `/rir help`、`/rir here` | 帮助、当前区域与群系 | `RIR.help` |
| `/rir switch biome` | 持久化群系提示开关 | `RIR.switch.biome` |
| `/rir switch region` | 持久化区域提示开关 | `RIR.switch.region` |
| `/rir createMode` | 切换工具选点；默认金锄头 | `RIR.admin` |
| `/rir pos1`、`/rir pos2` | 使用脚下位置选点 | `RIR.admin` |
| `/rir create cube <id>` | 使用两个选点创建立方体 | `RIR.admin` |
| `/rir create ball <id> [radius]` | 使用第一点创建球体 | `RIR.admin` |
| `/rir create cylinder <id> [radius] [height]` | 使用第一点创建圆柱体 | `RIR.admin` |
| `/rir list [page]`、`/rir check <id>` | 分页列表、区域详情 | `RIR.admin` |
| `/rir draw <id>` | 查看区域边界 | `RIR.admin` |
| `/rir set <id> <name\|priority\|enabled\|permission> <value>` | 修改名称、优先级、开关、进入权限 | `RIR.admin` |
| `/rir delete <id> confirm` | 删除区域，需完整输入确认词 | `RIR.admin` |
| `/rir reload`、`/rir stats` | 安全重载、运行计数 | `RIR.admin` |
| `/rir validate` | 检查磁盘配置，报告未加载世界；不应用配置或执行迁移 | `RIR.admin` |
| `/rir importbettercommands` | 迁移旧附属配置和玩家历史 | `RIR.admin` |
| `/itemtype`（`/it`） | 查看主手物品的材质名称 | `RIR.itemtype` |

帮助、提示开关、物品查询默认允许玩家使用，管理权限默认 OP。`RIR.bypass.enter`、`RIR.bypass.interact` 默认 OP，分别绕过进入限制与物品交互限制。

新建区域默认公开；`/rir set spawn permission public` 设置公开，`/rir set spawn permission vip.spawn` 设置指定权限。优先级影响消息顺序和主区域显示；重叠区域的权限和限制全部生效。

## 区域示例

```yaml
Regions:
  rest_area:
    name: '&a休息区'
    world: world
    type: cylinder
    centerX: 0.5
    centerY: 64
    centerZ: 0.5
    radius: 12
    height: 8
    enabled: true
    priority: 10
    enterPermission: ''
    inInfos:
      - '[TITLE]-欢迎来到::%name%::10::40::10'
    outInfos:
      - '[ACB]-离开 %name%'
    firstEnterInfos:
      - '[MSG]-%player%，这是你第一次来到这里！'
    repeatInfos:
      '200':
        - '[ACB]-你正在 %name% 休息'
    banInteractItems: [LAVA_BUCKET, FIRE_CHARGE]
```

球体使用 `centerX/Y/Z` 和 `radius`；立方体使用 `X1/Y1/Z1`、`X2/Y2/Z2`。更多示例在 [regions.example.yml](src/main/resources/regions.example.yml)。半径、高度必须为正数，坐标必须有限；旧配置的出生、踢出位置字段仍可读取。

消息支持 `[MSG]-`、`[TITLE]-`、`[ACB]-`、`[PCOM]-`、`[CCOM]-`。后两种分别由玩家、控制台执行命令，可省略开头的 `/`。只识别开头的类型标记，正文中的标记不会被当成命令。

内置变量：`%player%`、`%uuid%`、`%name%`、`%id%`，群系消息另有 `%oldBiome%`、`%newBiome%`。支持 `&` 颜色码，未配置翻译的群系显示原名称。区域消息不自动解析第三方 PlaceholderAPI 变量。

## PlaceholderAPI 与开发接口

| 占位符 | 含义 |
| --- | --- |
| `%RIR_region%` | 按优先级排列的所有区域名称 |
| `%RIR_region_primary%` | 优先级最高的区域名称 |
| `%RIR_region_ids%` | 按优先级排列的区域 ID |
| `%RIR_region_count%` | 区域数量 |
| `%RIR_biome%` | 缓存的群系显示名 |

占位符读取不可变快照，可供异步显示插件使用。区域状态通常在移动后一个 tick 确认，群系缓存受 `biomeSpeed` 控制。

公共接口为 `pregrafer.rir.api.RegionApi`。`RegionEnterAttemptEvent` 用于进入前的可取消检查，`RegionEnteredEvent` 用于确认进入后的奖励，`RegionLeftEvent` 表示实际离开。源码包名已统一，不提供旧附属二进制兼容桥接。[接入注意事项](docs/UPGRADE-3.1.md#开发接口)。

成员关系支持按 UUID 查询：`getPlayerCurrentRegionIds(UUID)` 和 `getRegionPlayers(String)` 返回只读快照。区域定义与玩家会话分离，已加载定义不能通过 `configure()` 修改，避免绕过索引更新。创建或修改区域应走管理命令与配置保存流程。

## 构建与源码结构

建议使用 JDK 17、Maven 3.9 执行 `mvn verify`。构建自动从官方仓库解析依赖，不再使用 `libs` 中的手工 JAR。

GitHub Actions 配置在 [.github/workflows/build.yml](.github/workflows/build.yml)：推送或 PR 后运行 1.9/1.12.2/1.13.2 API 回归测试、1.21.11/26.2 API 编译，上传以 1.9 API 构建的插件和测试报告。CI 不等同于真实服务器验收。

Java 根包从 `com.github.pregrafer.RegionInformationReload` 缩短为 `pregrafer.rir`。`com.github` 不是 Java 必需的前缀；保留作者和项目名称可避免过于通用的包名。Maven 的 `groupId` 是独立的制品坐标，不控制源码目录，继续使用 `com.github.pregrafer`。

```text
src/main/java/pregrafer/rir/
├── RirPlugin.java       插件入口
├── api/                 查询接口
├── command/             命令与补全
├── compat/              服务端版本差异
├── config/              配置、迁移与持久化
├── event/               进入检查、确认进入、离开事件
├── geometry/            三维坐标
├── integration/         PlaceholderAPI 接入
├── listener/            Bukkit 事件监听
├── message/             消息和指令
├── region/              区域及三种形状
├── spatial/             区块索引、选点与预览
└── tracking/            玩家状态和调度
```

测试位于 `src/test/java/pregrafer/rir/`。包名全小写，类与文件使用 PascalCase，方法和字段使用 lowerCamelCase，常量使用 UPPER_SNAKE_CASE。布尔方法使用 `is`、`has` 或 `can` 表达含义；时间字段和访问器标明 `Tick`/`Ticks`，如 `getRegionCheckIntervalTicks()`。区域类采用 `CuboidRegion`、`SphereRegion`、`CylinderRegion`，配置与命令仍使用既有的 `cube`、`ball`、`cylinder`。

改动说明与调度实现见 [重构与设计说明](docs/REFACTORING.md)。待确认玩家处理、到期队列和分散校准现已组合实现；普通移动的即时权限检查与下一 tick 确认进入继续保留。

最近的功能、性能、可用性与扩展接口检查见 [2026-10-19 检查记录](docs/AUDIT-2026-10-19.md)。
