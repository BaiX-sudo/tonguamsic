# smali 补丁说明

本目录存放本项目对原版 APK smali 的全部改动，均为标准 **unified diff**，可使用 `patch` 或 `git apply` 应用。

```
patches/
└── com/xifeng/music/
    ├── App.smali.patch              +4 行
    └── ui/screens/as.smali.patch     -11 / +5 行
```

两处补丁的路径均相对于 apktool 解包后的根目录（即补丁路径里的 `smali/` 与解包目录中的 `smali/` 一一对应）。

## 应用方式

在解包目录 `decoded/` 的上一级执行：

```bash
# GNU Patch
patch -p1 -d decoded < patches/com/xifeng/music/App.smali.patch
patch -p1 -d decoded < patches/com/xifeng/music/ui/screens/as.smali.patch

# 或 Git
git apply -p1 --directory=decoded patches/com/xifeng/music/App.smali.patch
git apply -p1 --directory=decoded patches/com/xifeng/music/ui/screens/as.smali.patch
```

> `git apply` 无法创建目标父目录时，请先确认 `decoded/smali/com/xifeng/music/ui/screens/` 已存在。

## 补丁校验

应用前可用下面两条命令确认待修改的 smali 与补丁所基于的版本一致（输出应**为空**）：

```bash
grep -c 'AnnouncementService;->a:Lcom/xifeng/music/network/AnnouncementService;' \
     decoded/smali/com/xifeng/music/ui/screens/as.smali    # 期望 1

grep -c 'invoke-static {p0}, Lcom/xifeng/music/xifeng/Guard;->install' \
     decoded/smali/com/xifeng/music/App.smali               # 期望 0
```

若两条输出不符合预期，说明你的原版 APK 与本仓库基于的版本不一致，直接应用补丁可能失败。版本基线为 **冬瓜音乐 1.0.7（versionCode 26）**，SHA-256 `A4C2DE503A0C98635B86E4DF1CA89BCD71F3F63BCFAA55739D12C2A8378A485E`。

## 两处补丁做了什么

### `App.smali.patch`

在 `App.onCreate()` 的协程启动之后、`return-void` 之前，插入两行调用：

```smali
invoke-static {p0}, Lcom/xifeng/music/xifeng/Disclaimer;->install(Landroid/app/Application;)V
invoke-static {p0}, Lcom/xifeng/music/xifeng/Guard;->install(Landroid/app/Application;)V
```

`install()` 内部通过 `registerActivityLifecycleCallbacks` 挂钩，在 `MainActivity` 每次 `onResume` 时重建接管层，因此不需要改动加固壳里的 `MainActivity.onCreate`。

### `as.smali.patch`

把原本发起远程公告请求的分支替换为直接返回一个空的 `kotlinx.serialization.json.JsonElement` 单例。改后不再调用 `AnnouncementService.getAnnouncement`，强制更新弹窗及其伴随的副作用（网络请求、状态写入）一并消失。

这是本次改动中唯一触及原生分发链路的部分，也是「去运控」的关键。

## 不要做的事

- 不要尝试去 patch `libgushi.so` / `libmod_gushi.so` 中的逻辑，它们是加密的原生字节码。
- 不要把 `smali_classes2/` 与 `smali/` 混淆目录层级：部分包分散在两个 dex 中，apktool 会自动处理，合并会导致 dex 边界错乱。