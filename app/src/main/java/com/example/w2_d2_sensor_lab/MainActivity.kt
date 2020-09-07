package com.example.w2_d2_sensor_lab

import android.content.Context
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.abs

/**
 * @author Topias Peiponen
 * @since 04.09.2020
 */

/**
 * This activity utilizes gyroscope and light sensors in order to
 * manipulate properties of the transforming box in the middle of the activity.
 * The transforming box visualises the values of the sensors.
 *
 * Property manipulations include box scaling, color, positioning and rotation.
 */
class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var sensorManager : SensorManager
    private var gyroscopeSensor : Sensor? = null
    private var lightSensor : Sensor? = null
    private val lightAndColorMap : HashMap<Float, Drawable?> = HashMap()

    /**
     * Setting arbitrary multipliers and maximum/minimum values in order to
     * better showcase sensor functionality
     */
    companion object {
        private const val MAX_BOX_SIZE = 6.0f
        private const val MIN_BOX_SIZE = 1.0f
        private const val MAX_X_TRANSLATION = 500
        private const val MAX_Y_TRANSLATION = 500
        private const val X_TRANSLATION_MULTIPLIER = 20
        private const val Y_TRANSLATION_MULTIPLIER = 20
        private const val ROTATION_MULTIPLIER = 30
        private const val LIGHT_SCALING_DIVIDER = 2000
        private const val LIGHT_MIN_THRESHOLD = 0f
        private const val LIGHT_DIM_THRESHOLD = 1000f
        private const val LIGHT_MEDIUM_THRESHOLD = 5000f
        private const val LIGHT_BRIGHT_THRESHOLD = 10000f
        private const val LIGHT_MAX_THRESHOLD = 40000f
        private const val LIGHT_STATUS_VERY_DIM = "very dim"
        private const val LIGHT_STATUS_DIM = "dim"
        private const val LIGHT_STATUS_MEDIUM = "well lit"
        private const val LIGHT_STATUS_BRIGHT = "bright"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Fill color and light map
        lightAndColorMap[LIGHT_MIN_THRESHOLD] = getDrawable(R.color.veryDimLight)
        lightAndColorMap[LIGHT_DIM_THRESHOLD] = getDrawable(R.color.dimLight)
        lightAndColorMap[LIGHT_MEDIUM_THRESHOLD] = getDrawable(R.color.mediumLight)
        lightAndColorMap[LIGHT_BRIGHT_THRESHOLD] = getDrawable(R.color.brightLight)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val allSensor = sensorManager.getSensorList(Sensor.TYPE_ALL)
        for (sensor in allSensor) {
            Log.d("SensorList", sensor.toString())
        }

        // Checking if gyroscope sensor exists in device
        if (sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null ) {
            gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

            // Setting default text for gyroscope
            gyroAxisX.text = getString(R.string.gyro_axisX_text, 0.0f)
            gyroAxisY.text = getString(R.string.gyro_axisY_text, 0.0f)
            gyroAxisZ.text = getString(R.string.gyro_axisZ_text, 0.0f)
        } else {
            Log.d("SensorManager", "Gyroscope sensor not found.")
        }

        // Checking if light sensor exists in device
        if (sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null ) {
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

            // Setting default text for light
            lightText.text = getString(R.string.light_illuminance_text, 0.0f)
            lightStatusText.text = getString(R.string.light_status_text, LIGHT_STATUS_VERY_DIM)

        } else {
            Log.d("SensorManager", "Light sensor not found.")
        }
    }

    override fun onResume() {
        super.onResume()
        gyroscopeSensor?.also {
                gyroscope -> sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL)
        }
        lightSensor?.also {
                light -> sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No need for anything here
    }

    override fun onSensorChanged(event : SensorEvent) {
        if (event.sensor == gyroscopeSensor) {
            // Check if sensor values are currently 0.0 (device hasn't moved)
            if (event.values[0] != 0.0f && event.values[1] != 0.0f && event.values[2] != 0.0f) {
                // Log data
                Log.d("GyroSensor", "X axis: ${(event.values[0])}")
                Log.d("GyroSensor", "Y axis: ${(event.values[1])}")
                Log.d("GyroSensor", "Z axis: ${(event.values[2])}")

                // Input data to UI
                gyroAxisX.text = getString(R.string.gyro_axisX_text, (event.values[0]))
                gyroAxisY.text = getString(R.string.gyro_axisY_text, (event.values[1]))
                gyroAxisZ.text = getString(R.string.gyro_axisZ_text, (event.values[2]))

                if (event.values[0] != 0.0f) {
                    changeBoxPosition(event.values[0], "X")
                }
                if (event.values[1] != 0.0f) {
                    changeBoxPosition(event.values[1], "Y")
                }
                if (event.values[2] != 0.0f) {
                    rotateBox(event.values[2])
                }
            }
        }
        if (event.sensor == lightSensor) {
            Log.d("LightSensor", "Illuminance: ${(event.values[0]).toString()}")

            lightText.text = getString(R.string.light_illuminance_text, (event.values[0]))
            changeBoxSize(event.values[0])
            changeBoxColor(event.values[0])
        }
    }
    private fun changeBoxPosition(axisValue : Float, axisType : String) {
        /**
         * Axises are reversed in gyroscope sensors
         * therefore translationX uses Y axis and vice versa
         */
        var axisMultiplied : Float = 1.0f
        if (axisType == "Y") {
            axisMultiplied = axisValue * X_TRANSLATION_MULTIPLIER
            Log.d("ChangeBoxY", "AxisForTranslationX $axisMultiplied")
        } else if (axisType == "X") {
            axisMultiplied = axisValue * Y_TRANSLATION_MULTIPLIER
            Log.d("ChangeBoxX", "AxisForTranslationY $axisMultiplied")
        }
        if (axisType == "Y" && abs(axisMultiplied) <= MAX_X_TRANSLATION) {
            transformingBox.translationX = axisMultiplied
        } else if (axisType == "X" && abs(axisMultiplied) <= MAX_Y_TRANSLATION) {
            transformingBox.translationY = axisMultiplied
        }
    }

    private fun rotateBox(axisY : Float) {
        transformingBox.rotation = axisY * ROTATION_MULTIPLIER
    }

    /**
     * Box size manipulation requires scaling the illuminance value gotten from the sensor.
     */
    private fun changeBoxSize(lux : Float) {
        val scaledLux = (lux / LIGHT_SCALING_DIVIDER)
        if (scaledLux >= MIN_BOX_SIZE && scaledLux < MAX_BOX_SIZE) {
            transformingBox.scaleX = scaledLux
            transformingBox.scaleY = scaledLux
        } else if (scaledLux > MAX_BOX_SIZE) {
            transformingBox.scaleX = MAX_BOX_SIZE
            transformingBox.scaleY = MAX_BOX_SIZE
        } else if (scaledLux < MIN_BOX_SIZE) {
            transformingBox.scaleX = MIN_BOX_SIZE
            transformingBox.scaleY = MIN_BOX_SIZE
        }
    }

    /**
     * Modifying box color based on the arbitrary light thresholds.
     */
    private fun changeBoxColor(lux : Float) {
        if (lux >= LIGHT_MIN_THRESHOLD && lux < LIGHT_DIM_THRESHOLD) {
            transformingBox.background = lightAndColorMap[LIGHT_MIN_THRESHOLD]
            lightStatusText.text = getString(R.string.light_status_text, LIGHT_STATUS_VERY_DIM)
        } else if (lux >= LIGHT_DIM_THRESHOLD && lux < LIGHT_MEDIUM_THRESHOLD) {
            transformingBox.background = lightAndColorMap[LIGHT_DIM_THRESHOLD]
            lightStatusText.text = getString(R.string.light_status_text, LIGHT_STATUS_DIM)
        } else if (lux >= LIGHT_MEDIUM_THRESHOLD && lux < LIGHT_BRIGHT_THRESHOLD) {
            transformingBox.background = lightAndColorMap[LIGHT_MEDIUM_THRESHOLD]
            lightStatusText.text = getString(R.string.light_status_text, LIGHT_STATUS_MEDIUM)
        } else if (lux >= LIGHT_BRIGHT_THRESHOLD && lux < LIGHT_MAX_THRESHOLD || lux <= LIGHT_MAX_THRESHOLD) {
            transformingBox.background = lightAndColorMap[LIGHT_BRIGHT_THRESHOLD]
            lightStatusText.text = getString(R.string.light_status_text, LIGHT_STATUS_BRIGHT)
        }
    }
}
