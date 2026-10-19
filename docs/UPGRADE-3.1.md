# 升级到 3.1

## 替换主插件

1. 停服并备份 `plugins/RegionInformationReload`。替换主插件 JAR 后再启动，不建议使用 PlugMan 等热加载工具。
2. 首次启动若不存在 `regions.yml`，插件校验旧 `config.yml` 后，将 `Regions` 复制到 `regions.yml`，并保留 `config.pre-3.1.yml` 备份。原配置不会删除。
3. 此后只修改 `regions.yml` 中的区域。`config.yml` 内旧 `Regions` 不再参与加载，避免删除过的区域重新出现。
4. 修改后执行 `/rir reload`。无效 YAML、缺少坐标、负半径、错误消息模板等会拒绝整次重载，继续使用此前有效配置。

可先执行 `/rir validate` 检查保存的配置。它不更改当前区域、玩家状态和文件，也不实际执行旧配置迁移；未加载的世界会给出提示。布尔值应写为 `true`/`false`，不要加引号；优先级必须为整数，禁用交互物品必须是字符串列表。错误类型会明确报错，不再悄悄退回默认值。`20` 和 `020` 这样重复的数值周期会被拒绝。

旧 `defaultRedius` 拼写仍可读取；新配置使用 `defaultRadius`，两者并存时后者优先。
旧区域未配置 `enterPermission` 时保留 `RIR.enter.<id>` 权限要求。通过命令新建的区域默认公开，可用 `/rir set <id> permission <node>` 改成限制进入。

区域 ID 限制为 1–64 个 ASCII 字母、数字、下划线或连字符，以避免 YAML 路径和权限节点歧义。原本使用其他字符的 ID 需要手动重命名，同时调整引用它的附属配置和权限。

## 整合 RIRBetterCommands

主插件现已包含附属的两个核心能力：首次进入消息/指令，以及停留期间的周期消息/指令。

1. 停服后移除 `RIRBetterCommands.jar`，保留 `plugins/RIRBetterCommands/config.yml`。
2. 启动新版主插件，确认旧区域已迁移。
3. 用控制台或管理员执行 `/rir importbettercommands`。

| 旧配置 | 新配置 |
| --- | --- |
| `Commands.first.<id>` | `Regions.<id>.firstEnterInfos` |
| `Commands.<ticks>.<id>` | `Regions.<id>.repeatInfos.<ticks>` |
| `Data.<id>` 玩家名列表 | `players.yml` 中的旧名字记录，玩家进入相关区域后逐步关联 UUID |

迁移会先验证所有引用和模板，再创建 `regions.before-bettercommands-<时间>.yml`、`players.before-bettercommands-<时间>.yml` 备份。已有内容不同的配置不会被覆盖；完全相同的内容可以重复合并。玩家历史先写入，区域规则随后启用，最后标记迁移完成。中途失败时可修复问题后重试。旧附属的源文件不会修改。

周期必须为 20–2147483647 tick，包括 25、37 等非整秒周期。与旧版全局时钟不同，新版从玩家进入区域时开始计算，每次离开后重新计时，避免刚进入就触发停留奖励。

旧名字只能匹配仍使用该名字的玩家；已经改名且尚未建立 UUID 对应关系的历史无法可靠自动认领。不要通过离线模式 UUID 猜测正版玩家身份。

## 行为变化

- `activeOnPlayerJoin` 和 `/rir switch` 只控制提示；进入权限、物品交互限制、配置中的命令依旧生效。关闭提示不会绕过区域规则。
- 区域边界仍包含端点；圆柱体高度从 `centerY` 向上延伸，保留旧几何语义。新建立方体覆盖所选两个方块。
- 普通移动/传送先做进入权限与可取消事件检查，实际进入状态及奖励在下一 tick 根据真实位置确认。
- 重载、创建、删除、属性修改会静默重建在线玩家状态，避免重放进入指令。位于新建区域内的玩家需要离开再进入，才触发该区域的首次进入逻辑。
- 坐骑等未触发玩家移动事件的路径依靠 `regionSpeed` 校准，因此不是完整的领地防护插件，也不提供路径线段穿越检测。生物群系提示按 `biomeSpeed` 合并短时间内的变化。
- 校准现按玩家分散到整个 `regionSpeed` 周期，最大校准间隔仍为该值，不再要求所有玩家在同一 tick 校准。无需添加配置字段。群系和停留动作按独立期限调度；首次登录仍在下一 tick 初始化，重载仍静默重建状态。
- 无权限出生或校准发现越界时，尝试返回上次有效位置、配置中的 `kick` 位置或世界出生点；这些位置都受区域权限校验。请确保至少存在一个可用的公开落点。普通步行/传送拒绝优先取消移动，不强行传送。
- `players.yml` 每 60 秒异步批量保存，正常关闭时等待并保存最新状态。首次进入历史和偏好在正常重启后保留；突然断电可能丢失最近一批记录，不承诺崩溃情况下奖励严格恰好执行一次。
- 删除区域不会清空该 ID 的首次进入历史；复用 ID 时不会重新发放已领取的首次奖励。

## 开发接口

当前项目没有需要保留兼容性的附属，源码统一迁移到 `pregrafer.rir`，不保留旧类路径或方法别名。旧附属 JAR 不能直接使用新的开发接口；有源码时需要更新导入和调用并重新编译。BetterCommands 的功能与配置导入已经由主插件提供，不需要安装旧附属。

`pregrafer.rir.event.RegionEnterAttemptEvent` 是进入前的可取消检查事件，**不保证移动最终成功**。其他区域或插件仍可能取消外层移动，因此不应在这个事件中直接发放不可回滚奖励。`RegionEnteredEvent` 在真实位置确认、全部成员关系更新后触发，适合处理进入成功逻辑；`RegionLeftEvent` 在实际离开后触发。

通过 Bukkit ServicesManager 获取 `RegionApi`：

```java
import pregrafer.rir.api.RegionApi;
import pregrafer.rir.region.Region;

RegionApi api = Bukkit.getServicesManager().load(RegionApi.class);
List<Region> regions = api.getRegions(player.getLocation());
```

查询前应声明插件依赖或确认服务已注册。API 查询返回集合快照；位置查询按优先级降序、ID 升序排列。除 PlaceholderAPI 已发布的字符串快照外，请在主线程使用插件 API。`RegionRepository`、`PlayerTracker` 等属于实现层，应通过 `RegionApi` 查询。

2026-10-19 的接口整理将在线成员从 `Region` 定义移到玩家追踪器；不再提供 `Region.getPlayerNames()`。使用 `api.getRegionPlayers(regionId)` 查询 UUID 集合，使用 `api.getPlayerCurrentRegionIds(uuid)` 查询玩家所属区域。返回的集合不可修改，且不随后续移动改变。已加载的区域定义禁止再调用 `configure()`；持久化修改仍通过管理命令或配置完成，并重建索引。`Region` 基类现在要求形状实现提供边界和包含判定，不再以默认 `false` 隐藏漏实现。

代码路径、方法和字段的命名变化不影响插件名、`plugins/RegionInformationReload` 数据目录、已有配置字段、权限节点和游戏命令。旧配置字段的读取兼容与旧附属 API 的二进制兼容是两件事；本次保留前者。

## 上服验收

自动化验证不等于真实服务器验证。请在测试服检查：普通玩家与 OP、公开/私有重叠区域、传送/传送门/重生、坐骑、生物群系 Y 轴变化、主副手交互、首次与周期指令、PlaceholderAPI 有无安装、停服重启和与其他插件同时取消移动。
