# LuxTorch 💡🔦

**A smart flashlight that automatically turns on in the dark, using your phone's ambient light sensor.**

LuxTorch is a modern Android utility that transforms the standard flashlight into an intelligent tool. By leveraging the same sensor your phone uses for auto-brightness, this app can automatically illuminate your surroundings the moment you enter a dark room, providing a seamless "set it and forget it" experience.

<p align="center">
  <img src="https://github.com/Moorix101/LuxTorch/blob/main/assets/1.jpeg" width="150" />
  <img src="https://github.com/Moorix101/LuxTorch/blob/main/assets/2.jpeg" width="150" />
  <img src="https://github.com/Moorix101/LuxTorch/blob/main/assets/3.jpeg" width="150" />
</p>



---

## 📖 Table of Contents

- [About The Project](#about-the-project-)
- [Key Features](#-key-features)
- [How It Works](#-how-it-works)
- [Getting Started](#-getting-started)
- [How to Use the App](#-how-to-use-the-app)
- [Technical Details](#-technical-details)
- [License](#-license)

---

## About The Project 🌟

The goal of LuxTorch was to create a flashlight app that thinks for itself. Instead of fumbling for a button in the dark, the app anticipates your needs. It uses the device's built-in ambient light sensor (`Sensor.TYPE_LIGHT`) to measure the surrounding brightness level (in **lux**, which inspired the name) and activates the torch automatically when the light drops below a threshold you define.

To make it even smarter, it also uses the proximity sensor to prevent the flash from turning on in your pocket or when the phone is face-down, saving battery and preventing accidental activation.

---

## ✨ Key Features

- **🤖 Smart Auto Mode:** The core feature. Automatically toggles the flashlight based on real-time ambient light levels.
- **🎚️ Adjustable Sensitivity:** A simple slider allows you to define exactly *how dark* it needs to be for the light to activate, giving you full control.
- **👜 Pocket-Safe:** Intelligently uses the proximity sensor to disable the flash when the phone is covered, in a bag, or in your pocket.
- **👆 Manual Override:** Works perfectly as a powerful, standard flashlight with a single tap when Auto Mode is disabled.
- **📊 Live Light Meter:** A dashboard card shows the current ambient light reading in lux, so you can see exactly what the sensor sees.
- **🎨 Modern Glass UI:** A beautiful and intuitive interface designed with a "glassmorphism" aesthetic, featuring clean cards and smooth animations.
- **🔋 Battery-Aware:** Carefully manages sensor listeners, unregistering them whenever the app is paused or closed to ensure minimal battery consumption.

---

## 🔧 How It Works

LuxTorch operates by listening to two key hardware sensors on your device:

1.  **Light Sensor:** The app gets a continuous stream of data from the `Sensor.TYPE_LIGHT`, measuring the ambient light in lux.
2.  **Proximity Sensor:** It simultaneously monitors the `Sensor.TYPE_PROXIMITY` to determine if the phone's screen is covered.
3.  **Logic Execution:** In Auto Mode, the app's main logic constantly checks two conditions:
    - Is `currentLux` less than the `luxThreshold` set by the user?
    - Is the phone *not* covered (i.e., `isNear` is false)?
4.  **Flash Control:** If both conditions are true, it uses the `CameraManager` API to turn the torch on. If either condition becomes false, it turns the torch off. This check is re-evaluated every time a sensor value changes or when the user adjusts the sensitivity slider.

---

## 🚀 Getting Started

Setting up the app is quick and easy.

1.  Download the latest APK from the [Releases](link/to/your/releases) page.
2.  Install the APK on your Android device.
3.  On first launch, grant the **Camera Permission** when prompted. This is required for the app to control the camera's LED flash.

That's it! The app is ready to use.

---

## 📱 How to Use the App

1.  **Manual Flash:** With "Auto Mode" off, simply tap the large power icon in the center to turn the flashlight on or off.
2.  **Auto Mode:** Flip the **Auto Mode** switch to enable the smart functionality. The manual button will be disabled, and the app will take over.
3.  **Adjust Sensitivity:** While in Auto Mode, move the **Sensitivity** slider to change the light threshold. The app will react instantly to the new setting.
    -   **Lower value** (more "Sensitive"): Requires near-total darkness to activate.
    -   **Higher value** (more "Tolerant"): Will activate in dimly lit rooms.

---

## 🛠️ Technical Details

-   **Language:** **Java**
-   **Architecture:** Single-Activity (`MainActivity`) that implements the `SensorEventListener` interface.
-   **Core APIs:**
    -   `SensorManager` to listen for `Sensor.TYPE_LIGHT` and `Sensor.TYPE_PROXIMITY`.
    -   `CameraManager` to control the torch mode (`setTorchMode`).
-   **UI Components:** Built with standard Android XML layouts, using `RelativeLayout`, `LinearLayout`, `Switch`, `SeekBar`, `ProgressBar`, and custom drawable shapes for the glass card effect.
-   **Permissions:** Requires `android.permission.CAMERA` to access the flash.
-   **Lifecycle Management:** Sensor listeners are correctly registered in `onResume()` and unregistered in `onPause()` to ensure battery efficiency and prevent leaks.

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.
