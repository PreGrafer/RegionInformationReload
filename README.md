# RegionInformationReload

一个用于 **区域提示 + 生物群系提示** 的 Spigot 插件，支持自定义进入/离开信息、区域交互限制，以及 PlaceholderAPI 变量。

## 当前版本信息

- 主类: `com.github.pregrafer.RegionInformationReload.RegionInformationReload`
- 命令别名: `/rir`（主命令），`/it`（`/itemtype`）
- 软依赖: `PlaceholderAPI`

## 主要功能

- 基于玩家移动检测区域进入/离开，并触发 `inInfos` / `outInfos`
- 区域类型支持: `cube`、`ball`、`cylinder`
- 支持嵌套区域（玩家可同时处于多个区域）
- 支持生物群系名称自定义与切换提示
- 支持区域内禁用指定物品交互（`banInteractItems`）
- 支持 PlaceholderAPI:
    - `%RIR_region%` 玩家当前所在区域名（多个区域会拼接）
    - `%RIR_biome%` 玩家当前生物群系自定义名称

## 安装

1. 将插件 Jar 放入服务器 `plugins` 目录。
2. 若需要占位符功能，请先安装 `PlaceholderAPI`。
3. 启动服务器生成默认 `config.yml`。
4. 按需修改配置后执行 `/rir reload`。

> 说明: `plugin.yml` 中 `api-version` 为 `1.13`，`pom.xml` 编译依赖为 `spigot-api 1.12.2-R0.1-SNAPSHOT`。跨版本使用请自行实测。

## 命令

### 主命令 `/regioninformationreload`（别名 `/rir`）

- `/rir help` 查看帮助（`RIR.help`）
- `/rir reload` 重载插件（`RIR.admin`）
- `/rir createMode` 切换选点模式（`RIR.admin`）
- `/rir create <ball|cube|cylinder> <regionUniqueId>` 创建区域（`RIR.admin`）
- `/rir draw <regionUniqueId>` 粒子描边显示区域（`RIR.admin`）

### 工具命令 `/itemtype`（别名 `/it`）

- 查看主手物品类型名称（用于填写 `tool` 或 `banInteractItems`）

## 权限

- `RIR.admin` 管理命令权限
- `RIR.help` 帮助命令权限
- `RIR.enter.<regionUniqueId>` 进入指定区域权限

示例: `RIR.enter.region1`

## 创建区域流程

1. 执行 `/rir createMode` 进入选点模式。
2. 手持 `config.yml -> Settings.tool` 指定物品。
3. 左键方块记录第一点，右键方块记录第二点。
4. 执行创建命令:
    - 球体: `/rir create ball <id>`（使用第一点为球心）
    - 立方体: `/rir create cube <id>`（使用第一点和第二点）
    - 圆柱体: `/rir create cylinder <id>`（使用第一点为圆心）

## 配置说明

- `Settings.tool` 选点工具材质名（如 `GOLDEN_HOE`）
- `Settings.activeOnPlayerJoin.biome/region` 生物群系/区域提示总开关
- `Settings.biomeHighAccuracy` 生物群系高精度切换
- `Settings.biomeSpeed`、`Settings.regionSpeed` 检测频率参数
- `Tips.Entry.biomeInfos` 生物群系提示模板
- `Tips.Entry.kickInfos` 无权限进入区域时提示模板
- `Regions.<id>.banInteractItems` 区域内禁交互物品列表

消息格式支持:

- `[MSG]-文本`
- `[TITLE]-主标题::副标题::淡入::停留::淡出`
- `[ACB]-ActionBar文本`
- `[PCOM]-玩家执行指令`
- `[CCOM]-控制台执行指令`

内置占位符:

- `%player%` 玩家名
- `%name%` 区域名
- `%oldBiome%` / `%newBiome%` 生物群系名

## API

项目提供 `RegionAPI` / `RegionAPIImpl`，可用于:

- 根据 ID 获取区域
- 根据 `Location` 获取所在区域列表
- 获取玩家当前区域 ID 集合
- 获取全部已加载区域