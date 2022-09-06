package tools

import com.google.gson.Gson
import org.openrndr.math.clamp
import java.io.File


/**
 * Convert a json file to a list of normalized values.
 */
fun jsonToNormalizedVector(json: String): List<Double> {
    val a = Gson().fromJson(json, HashMap::class.java) as HashMap<String, Any>
    val result = mutableListOf<Double>()
    fun emitValue(value: Double, min:Double, max:Double) {
        result.add((value.clamp(min, max) - min) / (max - min))
    }
    for (key in a.keys) {
        if (a[key] is Map<*, *>) {
            val m = a[key] as Map<*, *>
            for (key2 in m.keys) {
                val vm = m[key2] as Map<*, *>
                if (vm.containsKey("intValue")) {
                    val v = vm["intValue"] as Double
                    emitValue(v, vm["minValue"] as Double, vm["maxValue"] as Double)
                }
                else if (vm.containsKey("doubleValue")) {
                    val v = vm["doubleValue"] as Double
                    emitValue(v, vm["minValue"] as Double, vm["maxValue"] as Double)
                }
                else if (vm.containsKey("vector2Value")) {
                    val v = vm["vector2Value"] as Map<String, Double>
                    val vx = v["x"] as Double
                    val vy = v["y"] as Double
                    emitValue(vx, 0.0, 1.0)
                    emitValue(vy, 0.0, 1.0)
                }
                else {
                    error("unknown type")
                }
            }
        }
    }
    return result
}

fun normalizedVectorToMap(json: String, values: List<Double>) : Map<String, Any> {
    val result = Gson().fromJson(json, HashMap::class.java) as java.util.HashMap<String, Any>
    val a = Gson().fromJson(json, HashMap::class.java) as HashMap<String, Any>

    var valueIndex = 0
    fun loadValue(min:Double, max:Double): Double {
        return values[valueIndex++] * (max - min) + min
    }
    for (key in a.keys) {
        if (a[key] is java.util.Map<*, *>) {
            val m = a[key] as MutableMap<String, Any>
            for (key2 in m.keys) {
                val vm = m[key2] as MutableMap<String, Any>
                if (vm.containsKey("intValue")) {
                    vm["intValue"] = loadValue(vm["minValue"] as Double, vm["maxValue"] as Double).toInt()
                }
                else if (vm.containsKey("doubleValue")) {
                    vm["doubleValue"] = loadValue(vm["minValue"] as Double, vm["maxValue"] as Double)
                }
                else if (vm.containsKey("vector2Value")) {
                    val v = vm["vector2Value"] as MutableMap<String, Double>
                    v["x"] = loadValue(0.0, 1.0)
                    v["y"] = loadValue(0.0, 1.0)
                }
                else {
                    error("unknown type")
                }
            }
        }
    }
    return a
}


fun main() {
    val v = jsonToNormalizedVector(File("gui-parameters/CoverGen-latest.json").readText())
    println(v)
    val a = Gson().fromJson(File("gui-parameters/CoverGen-latest.json").readText(), HashMap::class.java) as HashMap<String, Any>

    println(a)
    val m = normalizedVectorToMap(File("gui-parameters/CoverGen-latest.json").readText(), v)
    println(m)
}
