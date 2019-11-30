package com.cs442.activityrecognition

class FeatureExtractor {
    private fun getXArrayList(list: ArrayList<Float>) : ArrayList<Float> {
        var output = arrayListOf<Float>()
        if (list.size == 0) {
            return output
        }

        for (i in 0..list.size step 3) {
            output.add(list[i])
        }
        return output
    }
    private fun getYArrayList(list: ArrayList<Float>) : ArrayList<Float> {
        var output = arrayListOf<Float>()
        if (list.size == 0) {
            return output
        }

        for (i in 1..list.size step 3) {
            output.add(list[i])
        }
        return output
    }
    private fun getZArrayList(list: ArrayList<Float>) : ArrayList<Float> {
        var output = arrayListOf<Float>()
        if (list.size == 0) {
            return output
        }

        for (i in 2..list.size step 3) {
            output.add(list[i])
        }
        return output
    }

    private fun getMin(list: ArrayList<Float>): Float {
        return list.min()!!.toFloat()
    }

    private fun getMax(list: ArrayList<Float>): Float {
        return list.max()!!.toFloat()
    }

    private fun getMean(list: ArrayList<Float>): Double {
        return list.average()
    }

    private fun getVariance(list: ArrayList<Float>): Double {
        val mean = getMean(list)
        var square = 0.0
        for (a in list)
            square += (a - mean) * (a - mean)
        return square / list.size
    }
}