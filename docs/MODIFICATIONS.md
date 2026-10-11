# 改动清单

本文逐项记录本项目对「冬瓜音乐 1.0.7」所做的全部改动。基线 APK 的 SHA-256 为
`A4C2DE503A0C98635B86E4DF1CA89BCD71F3F63BCFAA55739D12C2A8378A485E`。

原版客户端使用 ApkVmProtect 加固，界面与交互逻辑位于原生库 `libgushi.so` / `libmod_gushi.so` 内，
无法通过 smali 修改。因此除第 1、7、8 项外，所有功能都通过在 `MainActivity` 上挂载接管层
（`Guard`）的方式实现。

---

## 1. 移除远程公告与强制更新

| 项 | 值 |
|---|---|
| 文件 | `smali/com/xifeng/music/ui/screens/as.smali` |
| 补丁 | `patches/com/xifeng/music/ui/screens/as.smali.patch` |
| 改动量 | −11 行 / +5 行 |

原实现在协程中调用 `AnnouncementService.getAnnouncement()`，拿到远端公告后决定是否弹出更新对话框。
补丁删除了该调用，改为直接返回 `kotlinx.serialization.json.JsonElement` 的空单例：

```smali
.line 33
sget-object p1, Lkotlin/z;->a:Lkotlin/z;

.line 39
return-object p1
```

**副作用一并消失**：不再发起该网络请求，不再写入公告相关状态，也不会触发强更弹窗。
`AnnouncementService` 类本身保留在包内未做删除，以便回退。

---

## 2. 新增首启免责声明弹窗

| 项 | 值 |
|---|---|
| 文件 | `src/java/com/xifeng/music/xifeng/Disclaimer.java` |
| 安装入口 | `App.smali`（见第 7 项） |

`Disclaimer.install(Application)` 在应用启动时调用，首次进入时展示一张仿原版公告样式的卡片，内容为：

- 顶部大字标题：`软件由 [头像] 不许人间见白头 二次分发`
- 头像使用自定义 `CenterImageSpan`，按 `Paint` 的 ascent/descent 做垂直居中，避免图文基线错位
- **10 秒阅读倒计时**，期间两个按钮均不可点击
- 按钮：`不再显示`（写入 `SharedPreferences: xifeng_disclaimer`，键 `never_show`）与 `关闭`
- 主题色随系统深浅色模式切换

---

## 3. 重写「关于我们」页面

| 项 | 值 |
|---|---|
| 文件 | `src/java/com/xifeng/music/xifeng/AboutActivity.java` |
| Manifest | 见第 8 项 |

原版关于我们页由原生壳渲染，其中的联系方式、小程序入口、打赏与侵权说明均无法直接修改。
本项目新增一个自绘的 `AboutActivity` 予以替代：

- 顶部展示**真实的应用 Logo**。原包中该图标是矢量 `VectorDrawable`，`Resources#getDrawable`
  在纯代码环境下返回的对象缺少必要信息，因此改为读取矢量 XML 后自行栅格化，再用 `SRC_IN`
  圆形遮罩裁切，避免了「画不出图」和「裁不出圆」两个问题。
- 展示三名开发者头像（圆形）与署名。头像图片已随仓库提供在 `res/drawable/`：

  | 署名 | 仓库内资源 | 说明 |
  |---|---|---|
  | BaiX | `res/drawable/avatar_user.png` | 320×320 圆图 |
  | deepseek | `res/drawable/avatar_deepseek.png` | 320×320，原图白底已转为透明 |
  | space bunny | `res/drawable/avatar_bunny.png` | 320×320 |

  三张图在构建时复制到 `decoded/res/drawable/`，代码侧一律用圆形遮罩裁切，
  不依赖 drawable 自身的圆角。

- 移除原页面的联系方式、小程序、打赏与侵权说明。
- 主题色取 M3 / 系统强调色，在系统深浅色下均已实机确认。

---

## 4. 「反馈BUG」改为唤起系统邮箱

| 项 | 值 |
|---|---|
| 文件 | `src/java/com/xifeng/music/xifeng/Guard.java` |
| 方法 | `Guard.sendFeedback(Activity)` |

原版该行的点击被原生壳分发到加固后的逻辑。本项目改为直接发起 `ACTION_SENDTO`：

- 收件人：`hjl1357275420@hotmail`
- 主题：`冬瓜音乐 问题反馈`
- 正文自动填充：`v1.0.7`、设备厂商与型号、Android 版本，以及空的问题描述区
- 若系统无可用邮箱应用，弹出 Toast「未找到可用的邮箱应用」

---

## 5. 首页折叠态顶部按钮改为唤起浏览器

| 项 | 值 |
|---|---|
| 文件 | `src/java/com/xifeng/music/xifeng/Guard.java` |
| 方法 | `Guard.openProfile(Activity)` |

仅当检测到首页处于**滚动折叠态**（顶栏为「冬瓜音乐 + 右侧图标」的形态）时，该按钮才被接管；
点击后发起 `ACTION_VIEW` 打开 <https://github.com/BaiX-sudo>。无可用浏览器时弹出 Toast 提示。

未折叠状态（顶部为搜索栏）下该区域不被接管，原生齿轮按钮功能保持不变。

---

## 6. 修正被接管控件的手势判定

| 项 | 值 |
|---|---|
| 文件 | `src/java/com/xifeng/music/xifeng/Guard.java` |
| 类 | `Guard.TouchHandler` |

接管层最初的实现在 `ACTION_DOWN` 阶段就消费事件，导致设置页的两行控件**滑动或长按时立即跳转**。
现改为标准点击判定：

| 阶段 | 行为 |
|---|---|
| `ACTION_DOWN` | 命中接管区域则消费事件，记录坐标与时间，绘制自定义圆形按压高亮 |
| `ACTION_MOVE` | 位移未超过 `ViewConfiguration#getScaledTouchSlop` 时继续消费；一旦超过，先合成一个 `DOWN` 再把后续事件转交底层，使列表正常滚动，并撤掉按压高亮 |
| `ACTION_UP` | 若此前已在转发，转交 `UP` 后结束；若未转发且 `eventTime - downTime ≤ 600ms`，才执行跳转 |
| `ACTION_CANCEL` | 清理状态，不执行任何动作 |

实机回归覆盖两个接管控件 × 点击/滑动/长按共 6 项，外加折叠态按钮 3 项，全部通过。

### 命中区域

区域以设备实际布局标定，集中在 `Guard.attach()` 顶部：

```java
int aboutTop = dp(activity, 407);   int aboutBottom = dp(activity, 463);  // 关于我们
int bugTop   = dp(activity, 347);   int bugBottom   = dp(activity, 402);  // 反馈BUG
int tipTop   = dp(activity,  45);   int tipBottom   = dp(activity,  92);  // 折叠态顶部按钮
int tipLeft  = dp(activity, 370);   int tipRight    = dp(activity, 420);
```

**换设备或改系统字号必须重新标定**，否则接管区域会整体错位。

### 状态识别

`Guard.step()` 每 350ms 用 `PixelCopy` 抓取一帧，由两个纯像素算法判定当前状态：

- `isSparse()`：在画面 `y ∈ [55%, 80%]`、`x ∈ [12%, 88%]` 的网格上采样，判断整体色彩跨度是否
  `< 26`，用于识别内容稀疏的「设置页」。
- `isLibraryTop()`：在 `y ∈ [2%, 8.5%]` 逐行检查是否存在「背景均匀、左侧有元素（差值 > 40）、
  右侧有元素（差值 > 60）」的顶栏，用于识别首页的折叠态。

两者结果写入静态字段 `settingsVisible` 与 `libraryTop`，`TouchHandler` 据此决定是否接管触摸。

---

## 7. 注入入口

| 项 | 值 |
|---|---|
| 文件 | `smali/com/xifeng/music/App.smali` |
| 补丁 | `patches/com/xifeng/music/App.smali.patch` |
| 改动量 | +4 行 |

在 `App.onCreate()` 的 `return-void` 之前插入：

```smali
invoke-static {p0}, Lcom/xifeng/music/xifeng/Disclaimer;->install(Landroid/app/Application;)V
invoke-static {p0}, Lcom/xifeng/music/xifeng/Guard;->install(Landroid/app/Application;)V
```

两个 `install()` 都只注册 `ActivityLifecycleCallbacks`，真正的接管层在 `MainActivity` 每次
`onResume` 时重建，因此不需要改动加固壳内部的 `MainActivity`。

---

## 8. 注册新 Activity

在 `AndroidManifest.xml` 的 `<application>` 内加入：

```xml
<activity
    android:name="com.xifeng.music.xifeng.AboutActivity"
    android:exported="false"
    android:label="关于我们"
    android:theme="@style/Theme.InnerTune"
    android:configChanges="orientation|screenSize|keyboardHidden|uiMode" />
```

---

## 附：注入代码的编译约束

构建工具链为 `javac`（JDK 21，`-source 8 -target 8`）+ `d8`（build-tools 34.0.0 内置，实测为
`D8 8.2.2-dev`）。d8 在处理**非 `static` 方法内定义的匿名内部类**时会抛出
`NullPointerException: Cannot invoke "String.length()" because "<parameter1>" is null`。
因此本项目所有注入代码一律使用**具名静态嵌套类**（见 `Guard.TouchHandler`、`Guard.CopyListener`、
`Disclaimer.CenterImageSpan` 等）。升级工具链时请保留这一写法。