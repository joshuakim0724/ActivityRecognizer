package com.cs442.activityrecognition

import android.util.Log
import weka.classifiers.bayes.NaiveBayes
import weka.classifiers.bayes.BayesNet
import weka.classifiers.trees.J48
import weka.core.Environment

import weka.core.Instance
import weka.core.Instances
import weka.core.converters.ArffSaver
import java.io.*

class ClassifierWrapper {
    private val TAG = "ClassifierWrapper"
    private var instancesForTraining: Instances? = null
    private var classifier = NaiveBayes()
    /* You can try other algorithms, i.e. */
    /* private var classifier = BayesNet() */
    /* private var classifier = J48() */

    fun train(instances: Instances) {
        classifier.buildClassifier(instances)
        instancesForTraining = instances
    }

    fun predict(instance: Instance): String? {
        val result = classifier.classifyInstance(instance)
        return instancesForTraining!!.classAttribute().value(result.toInt())
    }

    fun save(fileName: String) {
        val saver = ArffSaver()

        saver.instances = instancesForTraining
        val dirPath = "/sdcard/wekaExample"

        val filePath = "$dirPath/$fileName"

        val dirFile = File(dirPath)
        if (!dirFile.exists()) {
            dirFile.mkdirs()
        }

        saver.setFile(File(filePath))
        saver.writeBatch()
    }

    @Throws(FileNotFoundException::class)
    fun load(fileName: String) {
        val dirPath = "/sdcard/wekaExample"
        val filePath = "$dirPath/$fileName"

        if (!File(filePath).exists()) throw FileNotFoundException("$fileName does not exist")

        val reader = BufferedReader(FileReader(filePath))
        val data = Instances(reader)
        if (data.classIndex() == -1)
            data.setClassIndex(data.numAttributes() - 1)
        train(data)
    }
}