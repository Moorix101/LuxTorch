package com.moorixlabs.luxtorch;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int CAMERA_PERMISSION_CODE = 100;

    // UI Components
    private Switch autoModeSwitch;
    private ImageView flashIcon;
    private TextView luxValueText;
    private TextView thresholdText;
    private SeekBar thresholdSeekBar;
    private ProgressBar luxProgressBar;
    private TextView flashStatusText;
    private View buttonGlow;
    private View statusIndicator;

    // Sensors & Camera
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private Sensor proximitySensor;
    private CameraManager cameraManager;
    private String cameraId;

    // State
    private boolean isFlashOn = false;
    private boolean isAutoMode = false;
    private boolean isNear = false; // Is the phone covered?
    private float luxThreshold = 15.0f; // Default threshold
    private float currentLux = 0.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        initializeSensors();
        checkPermissions();
        setupListeners();
    }

    private void initializeViews() {
        autoModeSwitch = findViewById(R.id.autoModeSwitch);
        flashIcon = findViewById(R.id.flashIcon);
        luxValueText = findViewById(R.id.luxValueText);
        thresholdText = findViewById(R.id.thresholdText);
        thresholdSeekBar = findViewById(R.id.thresholdSeekBar);

        // --- New UI Components ---
        luxProgressBar = findViewById(R.id.luxProgressBar);
        flashStatusText = findViewById(R.id.flashStatusText);
        buttonGlow = findViewById(R.id.buttonGlow);
        statusIndicator = findViewById(R.id.statusIndicator);

        // --- Set Initial States ---
        // Set max for progress bar (e.g., 100 lux is a good dim-room max)
        luxProgressBar.setMax(100);

        // Cleaner threshold text to match XML design
        thresholdText.setText(String.format("%.0f lux", luxThreshold));
        thresholdSeekBar.setProgress((int) luxThreshold);

        // Set initial UI states
        updateFlashIcon(); // Set correct icon
        flashStatusText.setText("FLASH OFF");
        statusIndicator.setAlpha(0.0f); // Hide
        buttonGlow.setAlpha(0.0f); // Hide
    }

    private void initializeSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager == null) {
            Toast.makeText(this, "Device does not support sensors!", Toast.LENGTH_LONG).show();
            return;
        }

        // Light Sensor
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (lightSensor == null) {
            Toast.makeText(this, "No light sensor found!", Toast.LENGTH_LONG).show();
        }

        // --- Proximity Sensor (for "Pocket Mode") ---
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if (proximitySensor == null) {
            Toast.makeText(this, "No proximity sensor. 'Pocket Mode' disabled.", Toast.LENGTH_SHORT).show();
        }

        // Camera Manager
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (cameraManager != null) {
                cameraId = cameraManager.getCameraIdList()[0];
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE);
        }
    }

    private void setupListeners() {
        // Auto mode switch
        autoModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isAutoMode = isChecked;
            if (isAutoMode) {
                startSensorMonitoring();
                flashStatusText.setText("AUTO MODE ACTIVE");
                Toast.makeText(this, "Auto mode enabled", Toast.LENGTH_SHORT).show();

                // When auto-mode is turned on, run the logic immediately
                evaluateAutoModeLogic();
            } else {
                stopSensorMonitoring();
                if (isFlashOn) {
                    toggleFlash(false); // Turn off flash when disabling auto mode
                }
                flashStatusText.setText("FLASH OFF");
                Toast.makeText(this, "Auto mode disabled", Toast.LENGTH_SHORT).show();
            }
        });

        // Manual flash toggle
        // This assumes you are still using the flashIcon for the click.
        // If you changed to flashButtonContainer, just change "flashIcon" here.
        flashIcon.setOnClickListener(v -> {
            if (!isAutoMode) {
                toggleFlash(!isFlashOn);
            } else {
                Toast.makeText(this, "Disable Auto Mode for manual control", Toast.LENGTH_SHORT).show();
            }
        });

        // Threshold adjustment
        thresholdSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                luxThreshold = progress;
                // Update text to match the "10 lux" style from XML
                thresholdText.setText(String.format("%.0f lux", luxThreshold));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(MainActivity.this,
                        "Sensitivity set to " + (int)luxThreshold + " lux",
                        Toast.LENGTH_SHORT).show();

                // --- THIS IS THE FIX ---
                // Instantly re-evaluate the flash state with the new threshold
                evaluateAutoModeLogic();
            }
        });
    }
    private void startSensorMonitoring() {
        if (sensorManager != null) {
            if (lightSensor != null) {
                sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
            if (proximitySensor != null) {
                sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    private void stopSensorMonitoring() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    private void toggleFlash(boolean turnOn) {
        try {
            if (cameraManager != null && cameraId != null) {
                cameraManager.setTorchMode(cameraId, turnOn);
                isFlashOn = turnOn;

                // --- Update All UI Elements ---
                updateFlashIcon();

                if (isFlashOn) {
                    buttonGlow.animate().alpha(0.5f).setDuration(300);
                    statusIndicator.animate().alpha(1.0f).setDuration(300);
                    if (!isAutoMode) {
                        flashStatusText.setText("FLASH ACTIVE");
                    }
                } else {
                    buttonGlow.animate().alpha(0.0f).setDuration(300);
                    statusIndicator.animate().alpha(0.0f).setDuration(300);
                    if (!isAutoMode) {
                        flashStatusText.setText("FLASH OFF");
                    }
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error controlling flash", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateFlashIcon() {
        if (isFlashOn) {
            flashIcon.setImageResource(R.drawable.ic_flash_on);
            // flashIcon.setAlpha(1.0f); // Already 1.0f by default
        } else {
            flashIcon.setImageResource(R.drawable.ic_flash_off);
            // flashIcon.setAlpha(0.5f); // Let's use the icon's natural alpha
        }
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();

        if (sensorType == Sensor.TYPE_LIGHT) {
            currentLux = event.values[0];
            luxValueText.setText(String.format("%.1f lux", currentLux));

            // Update progress bar, capping at its max value
            luxProgressBar.setProgress((int) Math.min(currentLux, luxProgressBar.getMax()));

        } else if (sensorType == Sensor.TYPE_PROXIMITY) {
            // Check if object is "near". Most sensors return 0 for near.
            isNear = (event.values[0] < proximitySensor.getMaximumRange());
        }

        // --- Updated Auto-Trigger Logic ---
        // Just call your new method here
        evaluateAutoModeLogic();
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for this implementation
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isAutoMode) {
            startSensorMonitoring();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopSensorMonitoring();

        // --- LIFECYCLE FIX ---
        // Always turn flash off when app is paused
        if (isFlashOn) {
            toggleFlash(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Final cleanup, though onPause() should handle most cases
        if (isFlashOn) {
            toggleFlash(false);
        }
    }
    // Add this new method to your MainActivity.java
    private void evaluateAutoModeLogic() {
        if (isAutoMode) {
            // Only turn on if it's dark AND the phone is NOT covered
            if (currentLux < luxThreshold && !isFlashOn && !isNear) {
                toggleFlash(true);
            }
            // Turn off if it gets light OR if the phone IS covered
            else if ((currentLux >= luxThreshold || isNear) && isFlashOn) {
                toggleFlash(false);
            }
        }
    }
}