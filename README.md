# TaskManager

一个简洁的 Android 任务管理应用，使用 Kotlin 开发，帮助用户记录和管理日常任务。

## 功能特性

- 📝 **任务记录** - 创建和编辑任务内容
- 📅 **日期显示** - 自动记录任务创建日期和星期
- 📊 **进度跟踪** - 通过滑动方式设置任务完成进度 (0-100%)
- 💾 **本地存储** - 使用 SQLite 数据库持久化保存任务数据
- 🎨 **Material Design** - 遵循 Material Design 3 设计规范
- 🌓 **日夜主题** - 支持深色/浅色主题切换
- 🔔 **解锁启动** - 设备解锁或屏幕唤醒时自动启动应用

## 技术栈

- **语言**: Kotlin
- **最低 SDK**: API 24 (Android 7.0)
- **目标 SDK**: API 34 (Android 14)
- **编译 SDK**: 34
- **构建工具**: Gradle 8.4.2, Kotlin 2.0.0

### 主要依赖

- AndroidX Core KTX 1.13.1
- AppCompat 1.7.0
- Material Components 1.12.0
- Activity KTX 1.9.2
- ConstraintLayout 2.1.4

## 项目结构

```
app/
├── src/main/
│   ├── java/com/example/taskmanager/
│   │   ├── MainActivity.kt          # 主界面，展示任务列表
│   │   ├── SlidingCellView.kt       # 自定义任务单元格视图（支持滑动调整进度）
│   │   ├── UnlockReceiver.kt        # 广播接收器（监听设备解锁事件）
│   │   └── database/
│   │       └── TaskDatabaseHelper.kt # SQLite 数据库操作辅助类
│   ├── res/
│   │   ├── layout/                   # 布局文件
│   │   ├── values/                   # 资源值（颜色、字符串、主题）
│   │   ├── drawable/                 # 可绘制资源
│   │   └── mipmap-anydpi-v26/        # 应用图标
│   └── AndroidManifest.xml           # 应用清单
├── build.gradle.kts                  # 模块级构建配置
└── proguard-rules.pro                # ProGuard 混淆规则
```

## 核心功能说明

### 任务单元格 (TaskCellView)

- **双击编辑**: 双击空白单元格可输入任务内容
- **长按滑动**: 长按任务卡片 2 秒后，左右滑动可调整完成进度
- **状态显示**: 
  - 无内容：白色背景
  - 有内容未完成：亮黄色背景 + 红色文字
  - 进度 100%：深绿色背景（已完成）

### 数据库设计

任务表 (`tasks`) 包含以下字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `_id` | INTEGER | 主键，自增 |
| `date` | TEXT | 任务标识符（唯一） |
| `weekday` | TEXT | 星期几 |
| `create_time` | TEXT | 创建时间 |
| `progress` | INTEGER | 完成进度 (0-100) |
| `complete_time` | TEXT | 完成时间 |
| `remark` | TEXT | 任务内容 |

## 构建与运行

### 环境要求

- Android Studio Hedgehog 或更高版本
- JDK 17
- Android SDK 34

### 构建步骤

1. 克隆项目到本地
2. 使用 Android Studio 打开项目
3. 同步 Gradle 文件
4. 连接 Android 设备或启动模拟器
5. 点击 Run 按钮构建并安装应用

```bash
# 或使用命令行构建
./gradlew assembleDebug
```

## 权限说明

| 权限 | 用途 |
|------|------|
| `RECEIVE_BOOT_COMPLETED` | 接收设备启动完成广播 |
| `USER_PRESENT` | 监听设备解锁事件 |
| `SCREEN_ON` | 监听屏幕唤醒事件 |

## 配置说明

### 国内镜像源

项目已配置以下国内 Maven 镜像源以加速依赖下载：

- 华为云镜像
- 腾讯云镜像
- 阿里云镜像

配置文件位于 `settings.gradle.kts`

## 注意事项

⚠️ **缺失资源文件**: 当前项目缺少以下布局文件，需要补充后才能正常编译运行：

- `res/layout/activity_main.xml` - 主界面布局
- `res/layout/task_cell.xml` - 任务单元格布局
- `res/layout/input_dialog.xml` - 任务输入对话框布局

请确保添加这些布局文件后再进行构建。

## License

本项目仅供学习和参考使用。

---

**开发信息**:
- 包名: `com.example.taskmanager`
- 版本: 1.0 (versionCode: 1)
- 应用名称: TaskManager
