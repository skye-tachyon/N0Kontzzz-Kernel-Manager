<p align="center">
  <img src="nkm-logo.png" alt="NKM Logo">
</p>
<h1 align="center" style="font-size: 48px;">N0Kontzzz Kernel Manager</h1>

![Platform](https://img.shields.io/badge/platform-Android-green?style=for-the-badge&logo=android)
![Language](https://img.shields.io/badge/language-Kotlin-purple?style=for-the-badge&logo=kotlin)
![UI](https://img.shields.io/badge/Jetpack-Compose-blue?style=for-the-badge&logo=jetpackcompose)
![License](https://img.shields.io/github/license/bimoalfarrabi/N0Kontzzz-Kernel-Manager?style=for-the-badge&refresh=1)
![Root Required](https://img.shields.io/badge/Root-Required-critical?style=for-the-badge&logo=android)
[![Repo Size](https://img.shields.io/github/repo-size/bimoalfarrabi/N0Kontzzz-Kernel-Manager?style=for-the-badge&logo=github)](https://github.com/bimoalfarrabi/N0Kontzzz-Kernel-Manager)
[![Downloads](https://img.shields.io/github/downloads/bimoalfarrabi/N0Kontzzz-Kernel-Manager/total?color=%233DDC84&logo=android&logoColor=%23fff&style=for-the-badge)](https://github.com/bimoalfarrabi/N0Kontzzz-Kernel-Manager/releases)
[![Telegram](https://img.shields.io/badge/Telegram-Join-2CA5E0?style=for-the-badge&logo=telegram&logoColor=white)](https://t.me/n0kontzzz)

> [!CAUTION]
> **Use this application at your own risk.**
> This utility performs advanced system-level operations and kernel tuning that may impact device stability, cause data loss, or potentially damage hardware if misconfigured. The developers assume no responsibility for any issues or damages resulting from the use of this software.

**N0Kontzzz Kernel Manager** is a sophisticated Android application designed for rooted devices. Developed using Kotlin and Jetpack Compose, this utility offers comprehensive real-time monitoring and tuning capabilities for CPU performance, thermal management, and battery statistics. It is specifically optimized for the Poco F4 (munch) utilizing the N0Kontzzz custom kernels.

## Features

- **Real-time System Monitoring**
  Comprehensive dashboard providing live updates on critical system components in a responsive user interface.
  - **CPU**: Individual core frequencies, load percentages, and real-time temperatures.
  - **GPU**: Live tracking of GPU clock speed and utilization load.
  - **Memory**: Precise monitoring of RAM and ZRAM usage, including used/free capacity and compression efficiency.
  - **Storage**: Real-time breakdown of internal storage usage with exact filesystem statistics.
  - **Battery**: High-precision telemetry for voltage, current (mA), power consumption (Watts), and health status.

- **Advanced Battery Analytics**  
  Comprehensive battery history tracking including charging speeds, discharge rates, screen-on time, and deep sleep statistics. Includes automated reset options based on device reboot, charging events, or specific battery levels.

- **Process Monitor**  
  Real-time task manager that monitors CPU and RAM usage across all system and user processes. Features precise CPU delta calculation via `/proc` parsing, factual RAM usage (RSS) in MB, and advanced filtering options to distinguish between User and System/Root applications. Includes customizable sample rates and sorting capabilities.

- **Thermal Management**  
  Access to system thermal zone data for in-depth debugging and monitoring of device temperature profiles.

- **Performance Mode**
  Quickly toggle between system-wide performance presets tailored for different usage scenarios.
  - **Balanced**: Employs the `schedutil` governor for an optimal balance between system responsiveness and power efficiency.
  - **Performance**: Locks CPU clusters to the `performance` governor for maximum throughput and zero latency during intensive tasks.
  - **Powersave**: Switches to the `powersave` governor to minimize energy consumption and extend battery life. *Note: This mode is only functional if your kernel includes the `powersave` governor.*

- **CPU Performance Tuning**
  Comprehensive control over CPU scaling behaviors.
  - **Cluster Control**: Adjust Min/Max frequencies for Little, Big, and Prime clusters independently.
  - **Governor Selection**: Manually switch between all available kernel governors for fine-grained tuning.

- **GPU Control**
  Fine-tune graphics processing unit settings for gaming or efficiency.
  - **Frequency Scaling**: Set minimum and maximum GPU frequencies to cap performance or force high speeds.
  - **Governor Management**: Choose GPU governors (e.g., `simple_ondemand`) to dictate how the GPU ramps up clock speeds under load.
  - **Power Levels**: Configure GPU power throttling levels to manage heat generation during intense gaming sessions.

- **RAM & Memory Management**
  Advanced tools to optimize system memory usage and multitasking.
  - **ZRAM Config**: Monitor ZRAM usage and compression algorithms (e.g., LZ4, ZSTD) to maximize available memory.
  - **Virtual Memory (VM) Tweaks**: Adjust `swappiness`, `dirty_ratio`, and `vfs_cache_pressure` to control how aggressively the system swaps data and caches files.
  - **Low Memory Killer (LMK)**: View and tune LMK parameters to keep critical apps alive in the background.

- **Modern User Interface**
  Designed with the latest Jetpack Compose and Material Design 3 Expressive components for a cohesive and intuitive user experience.
  - **Dynamic Theming**: Support for Light, Dark, and System-adaptive themes.
  - **AMOLED Mode**: Pure black background optimization for OLED displays to maximize power savings in dark mode.
  - **Customizable Notification Icon**: Choose between live battery percentage, app logo, or a transparent icon for a cleaner status bar.
  - **Multi-language Support**: Fully localized in English and Indonesian.
  - **Permission Manager**: Transparently view and verify all critical system permissions requested by the application, including Root access and Usage statistics.
- **Intelligent Backup & Restore**
  Securely manage your application configurations with built-in safety mechanisms and granular control.
  - **Granular Management**: Selectively backup or restore specific categories including System Tuning, Network & Storage, Battery & Charging, or general UI preferences.
  - **Compatibility Guard**: The application performs strict validation during the restore process. It checks every setting (governors, frequencies, etc.) against the current kernel's capabilities. Unsupported values are safely skipped to prevent system instability or bootloops.
  - **Instant Application**: Restored settings are applied to the system nodes immediately upon completion, ensuring the device state matches your backup without requiring a reboot.
  - **Secure Storage**: Leverages the Android Storage Access Framework (SAF) for secure, user-granted file access to local storage.

- **Efficient Architecture**  
  Built on a lightweight MVVM architecture to ensure optimal performance and minimal resource usage on rooted devices.

- **Kernel Log**
  View real-time kernel diagnostic messages (dmesg) directly within the app. Features include:
  - **Live Monitoring**: Automatically updates as new logs are generated.
  - **Search**: Filter logs by keyword to quickly find specific events.
  - **Export**: Save the full log output to a file for sharing or debugging.
  - **Optimized Performance**: Efficiently handles large logs with background processing and smart caching.

- **Per-App Profiles**
  Automate your device's behavior by creating custom profiles that activate when specific applications are in the foreground.
  - **Performance Mode Integration**: Assign `Balanced`, `Performance`, or `Powersave` modes to specific apps.
  - **CPU Tuning**: Customize CPU governor and frequency settings for Little, Big, and Prime clusters.
  - **GPU Tuning**: Set GPU frequency limits, governor, and power levels per application.
  - **Thermal Tuning**: Apply specific thermal profiles to manage heat dissipation during intensive tasks.
  - **Graphics Optimization**: Toggle `KGSL Skip Pool Zeroing` and `Avoid Dirty PTE` for specific games to boost FPS.
  - **Battery Protection**: Automatically enable `Bypass Charging` for intensive apps to prevent battery wear and heat.

- **FPS Meter & Benchmarking**
  Advanced performance monitoring tool for gamers and power users.
  - **Real-time Overlay**: High-precision floating window displaying current FPS, 1% Low FPS, and Frame Time (ms).
  - **Interactive Design**: Fully draggable interface with **Contextual Opacity**—the overlay automatically fades when you touch outside to minimize visual distraction and returns to full opacity when touched internally.
  - **Comprehensive Benchmarking**: Record gameplay sessions to generate detailed performance reports with deep hardware telemetry.
  - **Advanced Analytics**:
    - **Performance Stats**: Track Average, Maximum, Minimum, and Variance FPS alongside 1% and 0.1% Lows.
    - **Frame Time Jitter**: Visualize frame delivery consistency to identify and debug micro-stutters.
    - **CPU Cluster Telemetry**: Simultaneous tracking of frequencies for Little, Big, and Prime clusters.
    - **Power & Thermal Tracking**: Monitor real-time power consumption (Watts), battery level, and temperatures (Avg/Max).
    - **Resource Correlation**: Align FPS data with live CPU and GPU utilization graphs.
  - **History & Visualization**: Review past sessions with high-resolution multi-line charts and localized duration summaries.

- **Bypass Charging**  
  Directly power the device from the AC adapter while pausing battery charging. This effectively minimizes heat buildup during intensive workloads and extends overall battery health. *Note: This feature requires kernel support.*

- **Charging Control**  
  Automates the charging cycle by stopping at a user-defined percentage and resuming when the battery drops to a lower level.  
  *Note: This feature requires **Battery Monitor** to be enabled and a kernel that supports **Bypass Charging** (as they utilize the same control node). To maximize power efficiency, the automation logic piggybacks on the Battery Monitor's existing background polling cycle rather than running a separate service.*

- **USB Fast Charge**  
  Forces the device to charge at a faster rate by increasing the charging current limits. Use with caution as it may increase heat buildup. *Note: This feature requires kernel support.*

- **KGSL Skip Pool Zeroing**  
  Optimizes Adreno GPU memory management by skipping the zero-initialization of memory pages, resulting in reduced overhead and improved graphical performance. *Note: This feature requires kernel support.*

- **Avoid Dirty PTE**
  Prevents the system from brute-force clearing dirty pages, theoretically improving benchmark scores and reducing overhead. *Note: This feature requires kernel support and may occasionally impact ZRAM consistency.*

- **TCP Congestion Control**
  Optimize network data throughput by selecting from available TCP congestion control algorithms (e.g., BBR, Cubic, Reno). Custom selections are reset upon reboot by default, but can be persisted using the **Apply on Boot** switch.

- **I/O Scheduler Tuning**
  Optimize storage read/write performance by choosing the most suitable kernel I/O scheduler for your workload. Preferences return to system defaults after a restart unless the **Apply on Boot** option is enabled.

- **Custom Tunables**
  Gain full control over kernel parameters by adding your own custom tunables.
  - **Path Selection**: Easily navigate the root file system to select any writable kernel node (e.g., `/proc/...` or `/sys/...`).
  - **Value Configuration**: Read the current value of any node and set your desired custom value.
  - **Boot Persistence**: Choose whether to apply your custom values automatically on device boot, ensuring your tweaks persist across restarts.
  - **Verification**: The interface displays both the target value and the actual current value, allowing you to verify if your settings have been applied successfully or overridden by the system.

- **Dexopt (App Optimization)**
  Manually trigger Android's Dalvik Executable Optimization process to improve application launch speeds and overall system smoothness.
  - **Speed Profile Compilation**: Compiles applications based on usage profiles for faster execution.
  - **Layout Compilation**: Optimizes view hierarchies to reduce UI rendering latency.
  - **Background Dexopt**: Executes the system's background maintenance optimization script immediately.

- **Background App Blocker**
  Utilizes a specialized kernel interface to prevent selected applications from consuming resources in the background. This feature functions by writing the package names to a kernel-managed blocklist, forcing the system to restrict their activity.
  *Technical Requirement: This feature is only available on kernels that expose the `/sys/kernel/n0kz_attributes/bg_blocklist` or `/sys/kernel/e404/bg_blocklist` nodes.*

---

## Requirements

- Kona (SM8250) devices running N0Kontzzz, N0kernel, FusionX, Lunar, E404R, perf+, or Oxygen+ kernel.
- Root access (Magisk or KernelSU supported).

---

## Permissions

- **Root Access (`android.permission.ACCESS_SUPERUSER`)**  
  Fundamental for executing kernel-level operations such as CPU frequency scaling and thermal management via `libsu`.

- **Dump (`android.permission.DUMP`)**  
  Required for the **Dexopt** feature to execute and monitor system-level package optimization commands.

- **Usage Access (`android.permission.PACKAGE_USAGE_STATS`)**  
  Required for the **Per-App Profiles** feature to detect which application is currently in the foreground and apply the corresponding system profile.

- **Storage Access (`android.permission.MANAGE_EXTERNAL_STORAGE`)**  
  Used to backup and restore application configurations, profiles, and logs.

- **Query All Packages (`android.permission.QUERY_ALL_PACKAGES`)**  
  Necessary for listing installed applications when creating new per-app profiles.

- **Display over other apps (`android.permission.SYSTEM_ALERT_WINDOW`)**  
  Required for the **FPS Meter** feature to show the real-time performance overlay on top of other applications and games.

- **Battery Optimization (`android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)**  
  Ensures the background monitoring service remains active to reliably apply profiles and track battery statistics without being killed by the system.

- **Vibration (`android.permission.VIBRATE`)**  
  Used to provide tactile haptic feedback when interacting with UI elements like sliders, enhancing the user experience.

- **Notifications (`android.permission.POST_NOTIFICATIONS`)**  
  Required to display persistent status notifications for background services like the Battery Monitor and Thermal Service.

- **Boot Completed (`android.permission.RECEIVE_BOOT_COMPLETED`)**  
  Allows the application to automatically initialize services and re-apply saved kernel profiles immediately after the device restarts.

- **Foreground Service (`android.permission.FOREGROUND_SERVICE`)**  
  Ensures critical monitoring services (Battery, App Monitor) continue running reliably in the background without being terminated by the system.

- **Privacy Focused**  
  No internet access permission is requested. The application operates entirely offline to ensure data privacy.

---

## Technology Stack

N0Kontzzz Kernel Manager is built with a modern Android stack, prioritizing performance, stability, and maintainability:

- **Language**: [Kotlin](https://kotlinlang.org/) — Utilizing Coroutines and Flow for efficient asynchronous programming.
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) — Built with Material Design 3 Expressive components for a reactive and fluid user experience.
- **Architecture**: **MVVM (Model-View-ViewModel)** — Implementing a clean separation of concerns and reactive data patterns.
- **Root Management**: [libsu](https://github.com/topjohnwu/libsu) — Industry-standard library for secure and high-performance root shell execution.
- **Dependency Injection**: [Dagger Hilt](https://dagger.dev/hilt/) — Simplifies DI and improves modularity across the application.
- **Local Database**: [Room Persistence](https://developer.android.com/training/data-storage/room) — Reliable SQLite abstraction for storing battery history, profiles, and custom tunables.
- **Background Tasks**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) & Foreground Services — Ensures critical monitoring and automation persist reliably.
- **Data Storage**: [DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore) — Modern, thread-safe replacement for SharedPreferences.
- **Serialization**: [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization) — Type-safe JSON parsing for configuration and backups.
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/) — Fast and lightweight image loading for UI assets.
- **Navigation**: [Compose Navigation](https://developer.android.com/jetpack/compose/navigation) — Type-safe navigation handling within the single-activity architecture.

---

## Repository

Contributions are welcome. Please feel free to fork the repository, submit issues, or create pull requests.

---
> [!TIP]
>
> - **Performance Mode**: Utilize the `performance` governor for demanding computational tasks.
> - **Balanced Profile**: The `schedutil` governor is recommended for an optimal balance between system responsiveness and power efficiency.
> - **Powersave Mode**: Employ the `powersave` governor to maximize battery life, especially during periods of low activity.


---

### Acknowledgments
- **[Xtra Kernel Manager](https://github.com/Gustyx-Power/Xtra-Kernel-Manager)** — The foundational project for this application.
- **[Danda](https://github.com/Danda420)** — For significant contributions to development and insights into Android system internals.
- **[RvKernel Manager](https://github.com/Rve27/RvKernel-Manager)** —  Provided inspiration for specific features and implementation references.
- Poco F4 Community —  For ongoing support and feedback.
---
