package classes

import com.google.gson.Gson
import org.openrndr.extra.noise.Random
import tools.normalizedVectorToMap
import java.io.File
import java.io.FileReader

class Entry(val type:String, val name:String, var ogdata:Map<String, Any> = emptyMap())

fun main() {

    val file = Gson().fromJson(FileReader(File("data/mapped-v2r1.json")),Array<Entry>::class.java).toList().filter {
        it.ogdata.isNotEmpty()
    }

    println(file.size)

    val picked = (0..1000).map {
        Random.pick(file)
    }.filter { (it.ogdata["uuid"]as String)!!.isNotEmpty()  }.distinct()

    println(picked.size)

    val finalJson = Gson().toJson(picked)
    File("data/RandomPicked2.json").writeText(finalJson)
}