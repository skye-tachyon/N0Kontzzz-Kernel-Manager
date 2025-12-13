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


**N0Kontzzz Kernel Manager** is a sophisticated Android application designed for rooted devices. Developed using Kotlin and Jetpack Compose, this utility offers comprehensive real-time monitoring and tuning capabilities for CPU performance, thermal management, and battery statistics. It is specifically optimized for the Poco F4 (munch) utilizing the [N0Kontzzz](https://github.com/bimoalfarrabi/kernel_xiaomi_sm8250_n0kontzz) custom kernel.


## Features

- **Real-time System Monitoring**  
  Detailed visualization of individual CPU core frequencies and system status in a responsive user interface.

- **Advanced Battery Analytics**  
  Comprehensive battery history tracking including charging speeds, discharge rates, screen-on time, and deep sleep statistics. Includes automated reset options based on device reboot, charging events, or specific battery levels.

- **Thermal Management**  
  Access to system thermal zone data for in-depth debugging and monitoring of device temperature profiles.

- **CPU Performance Tuning**
  Direct control over CPU governors and frequencies via native shell execution using [libsu](https://github.com/topjohnwu/libsu).

- **Modern User Interface**  
  Designed with the latest Jetpack Compose and Material Design 3 Expressive components for a cohesive and intuitive user experience.

- **Efficient Architecture**  
  Built on a lightweight MVVM architecture to ensure optimal performance and minimal resource usage on rooted devices.

- **Per-App Profiles**  
  Configure and automatically apply specific system profiles (Performance/Balanced), GPU tweaks, and charging behaviors for individual applications.

- **Bypass Charging**  
  Directly power the device from the AC adapter while pausing battery charging. This effectively minimizes heat buildup during intensive workloads and extends overall battery health. *Note: This feature requires kernel support.*

- **KGSL Skip Pool Zeroing**  
  Optimizes Adreno GPU memory management by skipping the zero-initialization of memory pages, resulting in reduced overhead and improved graphical performance. *Note: This feature requires kernel support.*

---

## Requirements

- Poco F4 (munch) device running N0Kontzzz or FusionX kernel.
- Root access (Magisk or KernelSU supported).

---

## Permissions

- **Root Access (`android.permission.ACCESS_SUPERUSER`)**  
  Fundamental for executing kernel-level operations such as CPU frequency scaling and thermal management via `libsu`.

- **Usage Access (`android.permission.PACKAGE_USAGE_STATS`)**  
  Required for the **Per-App Profiles** feature to detect which application is currently in the foreground and apply the corresponding system profile.

- **Storage Access (`android.permission.MANAGE_EXTERNAL_STORAGE`)**  
  Used to backup and restore application configurations, profiles, and logs.

- **Query All Packages (`android.permission.QUERY_ALL_PACKAGES`)**  
  Necessary for listing installed applications when creating new per-app profiles.

- **Battery Optimization (`android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)**  
  Ensures the background monitoring service remains active to reliably apply profiles and track battery statistics without being killed by the system.

- **System Alert Window (`android.permission.SYSTEM_ALERT_WINDOW`)**  
  (Optional) Used for displaying overlays or critical alerts if enabled.

- **Privacy Focused**  
  No internet access permission is requested. The application operates entirely offline to ensure data privacy.

---

## Technology Stack

- [Kotlin](https://kotlinlang.org/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [libsu](https://github.com/topjohnwu/libsu)
- MVVM Architecture
- Material Design 3

---

## Repository

Contributions are welcome. Please feel free to fork the repository, submit issues, or create pull requests.

---
> [!TIP]
>
> - **Performance Mode**: Utilize the `performance` governor for demanding computational tasks.
> - **Balanced Profile**: The `schedutil` governor is recommended for an optimal balance between system responsiveness and power efficiency.


---

### Acknowledgments
- **[Xtra Kernel Manager](https://github.com/Gustyx-Power/Xtra-Kernel-Manager)** — The foundational project for this application.
- **[Danda](https://github.com/Danda420)** — For significant contributions to development and insights into Android system internals.
- **[RvKernel Manager](https://github.com/Rve27/RvKernel-Manager)** —  Provided inspiration for specific features and implementation references.
- **Poco F4 Community** —  For ongoing support and feedback.
---

## Disclaimer

> This application executes advanced system-level operations that may impact device stability. Use is at your own risk. The developers assume no responsibility for potential issues resulting from improper configuration.

---