
/*
 * Simple binary classification
 *
 * Each features have value of between 0 and 1.
 * If sum of them is smaller than 2, then its class is c0.
 * Otherwise, it belongs to c1.
 * This example code shows how to train and test the classifier.
 */
package com.cs442.activityrecognition

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*
import weka.core.Attribute
import weka.core.FastVector
import weka.core.Instance
import weka.core.Instances

import java.io.*


class MainActivity : AppCompatActivity(), SensorEventListener, View.OnClickListener {
    /* Feature names and class names. */
    private val numFeature = 3 // acc: x y z gyro: x y z
    private val numClass = 3 // stand walk jump

    private val listFeature = listOf("ax", "ay", "az")
    private val listClass = listOf("stand", "walk", "jump", "test")

//    private val random = Random()

    private val instances = createEmptyInstances()
    private val classifier = ClassifierWrapper()


    private lateinit var sensorManager: SensorManager
    private var mAccel: Sensor? = null
    private var mGyro: Sensor? = null
    private var isCollecting: Boolean = false
    private var currButton: Button? = null

    private var standDataAcc: ArrayList<Float> = arrayListOf()
    private var walkDataAcc: ArrayList<Float> = arrayListOf()
    private var jumpDataAcc: ArrayList<Float> = arrayListOf()

    private var standDataGyro: ArrayList<Float> = arrayListOf()
    private var walkDataGyro: ArrayList<Float> = arrayListOf()
    private var jumpDataGyro: ArrayList<Float> = arrayListOf()

    private var testingAcc: ArrayList<Float> = arrayListOf()
    private var testingGyro: ArrayList<Float> = arrayListOf()

    private var isTested: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(this, permissions, 0)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mGyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        val standBtn = findViewById<Button>(R.id.stand)
        val walkBtn = findViewById<Button>(R.id.walk)
        val jumpBtn = findViewById<Button>(R.id.jump)
        val trainBtn = findViewById<Button>(R.id.train)
        val testBtn = findViewById<Button>(R.id.test)
        val refreshBtn = findViewById<ImageButton>(R.id.refresh)

        standBtn.setOnClickListener(this)
        walkBtn.setOnClickListener(this)
        jumpBtn.setOnClickListener(this)
        trainBtn.setOnClickListener(this)
        testBtn.setOnClickListener(this)
        refreshBtn.setOnClickListener(this)
    }
        @SuppressLint("SetTextI18n")
    override fun onClick(v: View) {
            val output = findViewById<TextView>(R.id.mainOutput)
            when (v.id) {
                R.id.stand -> {
                    if (isCollecting) {
                        isCollecting = false
                        sensorManager.unregisterListener(this)
//                        writeToFile("standAcc.txt", this, standDataAcc)
//                        writeToFile("standGyro.txt", this, standDataGyro)
                        output.text = "Data Collecting Finished."
                        return
                    }
                    currButton = findViewById<Button>(v.id)
                    output.text = "Data Collecting: \n Stand"
                    sensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_NORMAL)
                    sensorManager.registerListener(this, mGyro, SensorManager.SENSOR_DELAY_NORMAL)
                }
                R.id.walk -> {
                    if (isCollecting) {
                        isCollecting = false
                        sensorManager.unregisterListener(this)
//                        writeToFile("walkAcc.txt", this, walkDataAcc)
//                        writeToFile("walkGyro.txt", this, walkDataGyro)
                        output.text = "Data Collecting Finished."
                        return
                    }
                    currButton = findViewById<Button>(v.id)

                    output.text = "Data Collecting: \n Walk"
                    sensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_NORMAL)
                    sensorManager.registerListener(this, mGyro, SensorManager.SENSOR_DELAY_NORMAL)
                }
                R.id.jump -> {
                    if (isCollecting) {
                        isCollecting = false
                        sensorManager.unregisterListener(this)
//                        writeToFile("jumpAcc.txt", this, jumpDataAcc)
//                        writeToFile("jumpGyro.txt", this, jumpDataGyro)
                        output.text = "Data Collecting Finished."
                        return
                    }
                    currButton = findViewById<Button>(v.id)

                    output.text = "Data Collecting: \n Jump"
                    sensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_NORMAL)
                    sensorManager.registerListener(this, mGyro, SensorManager.SENSOR_DELAY_NORMAL)
                }
                R.id.train -> {
                    if (standDataAcc.size == 0 || standDataGyro.size == 0) {
                        output.text = "Cannot train. \n Missing Stand Data"
                    } else if (walkDataAcc.size == 0 || walkDataAcc.size == 0) {
                        output.text = "Cannot train. \n Missing Walk Data"
                    } else if (jumpDataAcc.size == 0 || jumpDataGyro.size == 0) {
                        output.text = "Cannot train. \n Missing Jump Data"
                    } else {
                        output.text = "Training in progress. \n Please wait."
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE ) == PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        ) {
                            currButton = findViewById(v.id)
                            training()
                            isTested = true
                        } else {
                            val permissions = arrayOf(
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            )
                            ActivityCompat.requestPermissions(this, permissions, 0)
                            output.text = "Permission was not granted. \n Unable to train."
                        }
                    }
                    isCollecting = false
                    return
                }
                R.id.test -> {
                    if (isCollecting) {
                        isCollecting = false
                        sensorManager.unregisterListener(this)
                        output.text = "Testing Finished."
                        return
                    }
                    if (isTested) {
                        currButton = findViewById(v.id)
                        output.text = "Current Activity:"
                        sensorManager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_NORMAL)
                        sensorManager.registerListener(this, mGyro, SensorManager.SENSOR_DELAY_NORMAL)
                    } else {
                        output.text = "Data not trained yet!"
                        return
                    }
                }
                R.id.refresh -> {
                    reset()
//                    Log.e("Reached", "Reached")
                    output.text = "Resetted Training Files"
                    isCollecting = false
                    isTested = false
                    return
                }
            }
            isCollecting = true
        }

    private fun training() {
        /* Create random data */
        fillInstances()

        /* Train classifier */
        classifier.train(instances)

        /* Test classifier */
//        testClassifier(100)

        /* Make sure you have permission to access external storage */
        /* Save classifier model */
        classifier.save("myModel.arff")

        /* Load classifier model */
        classifier.load("myModel.arff")

//        testClassifier(10)
        val output = findViewById<TextView>(R.id.mainOutput)
        output.text = "Training Finished!"

    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
       // Do something here if sensor changes.

    }

    override fun onSensorChanged(event :SensorEvent) {
        val sensorName = event.sensor.name
//        Log.d("Sensor Output", sensorName + ": X: " + event.values[0] + "; Y: " + event.values[1] + "; Z: " + event.values[2] + ";")
        val output = findViewById<TextView>(R.id.mainOutput)
        when (currButton) {
            findViewById<Button>(R.id.stand) -> {
                if (sensorName.contains("accelerometer", true)) {
                    standDataAcc.add(event.values[0])
                    standDataAcc.add(event.values[1])
                    standDataAcc.add(event.values[2])
                } else {
                    standDataGyro.add(event.values[0])
                    standDataGyro.add(event.values[1])
                    standDataGyro.add(event.values[2])
                }
            }
            findViewById<Button>(R.id.walk) -> {
                if (sensorName.contains("accelerometer", true)) {
                    walkDataAcc.add(event.values[0])
                    walkDataAcc.add(event.values[1])
                    walkDataAcc.add(event.values[2])
                } else {
                    walkDataGyro.add(event.values[0])
                    walkDataGyro.add(event.values[1])
                    walkDataGyro.add(event.values[2])
                }
            }
            findViewById<Button>(R.id.jump) -> {
                if (sensorName.contains("accelerometer", true)) {
                    jumpDataAcc.add(event.values[0])
                    jumpDataAcc.add(event.values[1])
                    jumpDataAcc.add(event.values[2])
                } else {
                    jumpDataGyro.add(event.values[0])
                    jumpDataGyro.add(event.values[1])
                    jumpDataGyro.add(event.values[2])
                }
            }
            findViewById<Button>(R.id.train) -> {
                output.text = "Training Finished!"
            }
            findViewById<Button>(R.id.test) -> {
                if (sensorName.contains("accelerometer", true)) {
                    testingAcc.add(event.values[0])
                    testingAcc.add(event.values[1])
                    testingAcc.add(event.values[2])
                } else {
                    testingGyro.add(event.values[0])
                    testingGyro.add(event.values[1])
                    testingGyro.add(event.values[2])
                }

                if (testingAcc.size >= 500) {
                    var standCount = 0
                    var walkCount = 0
                    var jumpCount = 0

                    for (i in 0 until testingAcc.size step 3) {
                        val instance = createInstance("stand", i, testingAcc)
                        instance.setDataset(instances)
                        val str = classifier.predict(instance)
//                        Log.e("Classifer: ", str)

                        when (classifier.predict(instance)) {
                            "stand" -> standCount += 1
                            "walk" -> walkCount += 1
                            "jump" -> jumpCount += 1
                        }
                    }
                    Log.e("Counts ", "$standCount $walkCount $jumpCount")

                    if (standCount >= walkCount && standCount >= jumpCount) {
                        output.text = "Current Activity: \nstand"
                    } else if (walkCount > standCount && walkCount > jumpCount) {
                        output.text = "Current Activity: \nwalk"
                    } else {
                        output.text = "Current Activity: \njump"
                    }
                    testingAcc.clear()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mAccel?.also { accel ->
            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL)
        }
        mGyro?.also { gyro ->
            sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    private fun writeToFile(filename: String, context: Context, list :ArrayList<Float>) {
        try {
            val outputStreamWriter =
                OutputStreamWriter(context.openFileOutput(filename, Context.MODE_PRIVATE))
            for (i in 0 until list.size step 3) {
                outputStreamWriter.write(list[i].toString() + " " + list[i+1].toString() + " " + list[i+2].toString())
                outputStreamWriter.write("\n")
            }
            outputStreamWriter.close()

        } catch (e: IOException) {
            Log.e("Exception", "File write failed: $e")
        }
    }

    private fun readFromFile(filename: String, context: Context): String {

        val file = File(context.filesDir, filename)
        try {
            val contents = file.readText()
            Log.e("Filename: ", filename)
            Log.e("contents " + contents.length, contents)

            return contents

        } catch (e: IOException) {
            Log.e("Exception", "File read failed: $e")

        }
        return ""
    }

    private fun createEmptyInstances(): Instances {
        /* Create an empty list of instances */
        val attrs = FastVector()

        for (f in listFeature) {
            attrs.addElement(Attribute(f))
        }

        val classes = FastVector()
        for (c in listClass) {
            classes.addElement(c)
        }
        attrs.addElement(Attribute("label", classes))

        return Instances("myInstances", attrs, 10000)
    }

    private fun createInstance(feature: String, idx: Int, list: ArrayList<Float>): Instance {
        /* Create a single instance, which consists 5 features and the class label. */
        /* In this example, class is simply determined by sum of each feature values. */
        val attrClass = instances.attribute("label")

        val instance = Instance(numFeature + 1)

        for (i in 0 until 3) {
            val aatr = instances.attribute(listFeature[i])
            instance.setValue(aatr, list[idx + i].toDouble())
        }

        instance.setValue(attrClass, feature)
        return instance
    }

    private fun fillInstances() {
        /* Fill random instances */
        val attrClass = instances.attribute("label")
        instances.setClass(attrClass)
        for (i in 0 until standDataAcc.size step 3) {
            val instance = createInstance("stand", i, standDataAcc)
            instances.add(instance)
        }
        for (i in 0 until walkDataAcc.size step 3) {
            val instance = createInstance("walk", i, walkDataAcc)
            instances.add(instance)
        }
        for (i in 0 until jumpDataAcc.size step 3) {
            val instance = createInstance("jump", i, jumpDataAcc)
            instances.add(instance)
        }
    }

    private fun testClassifier(testNum: Int) {
        /* Wrapper function for generating random instances, and apply classifier on them. */
        var cnt = 0

        for (i in 0 until standDataAcc.size step 3) {
            val instance = createInstance("stand", i, standDataAcc)
            instance.setDataset(instances)

            val actualClass = instance.stringValue(instances.attribute("label"))
            val predictClass = classifier.predict(instance)

            Log.i("testClassifier", "Classified as : $predictClass, actually : $actualClass")
            if (predictClass == actualClass)
                cnt += 1
        }
        Log.i("testClassifier", "accuracy: $cnt / " + standDataAcc.size/3)
    }

    private fun reset() {
        standDataAcc.clear()
        walkDataAcc.clear()
        jumpDataAcc.clear()

        standDataGyro.clear()
        walkDataGyro.clear()
        jumpDataGyro.clear()

        testingAcc.clear()
        testingGyro.clear()
    }
}