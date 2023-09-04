# RegionInformationReload-v1.9.1

# Create your region

      RegionInformationReload-Commands:
      /rir check <regionUniqueId> - 查看区域详细信息 #RIR.admin
      /rir create <type> <regionUniqueId> - 创建一个type类型的新区域(需要点位选择) #RIR.admin
      /rir createMode - 进入点位选择模式(手持config中的tool) #RIR.admin
      /rir help - 获取帮助 #RIR.help
      /rir reload - 重载插件 #RIR.admin
      /rir switch <biome/region> - 打开/关闭功能 #RIR.switch.biome/region

变量PlaceHolderAPI
%RIR_region% 玩家所处区域
%RIR_biome% 玩家所处生物群系

区域进入权限 RIR.enter.regionUID

创建cube区域时需要选取两个点 创建ball与cylinder只需要选取一个点
修复了部分bug

# API Coming Soon...

All function released.