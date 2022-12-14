package fillin

import com.google.gson.Gson
import java.io.File

class GraphData(val type:String, val name:String, val ogdata: Map<String, Any>)


fun main() {
    val data = Gson().fromJson(File("offline-data/graph/mapped-v2r1.json").readText(), Array<GraphData>::class.java)
    val uuids = File(protoJsonPath).listFiles()?.filter { it.isFile && it.extension == "json" }?.map { it.nameWithoutExtension }


    val rowNumbers = uuids?.map { uuid -> data.indexOfFirst { it.ogdata.get("uuid") ==  uuid} }


    File("offline-data/resolved").mkdirs()
    if (rowNumbers != null) {
        File("offline-data/resolved/proto-row-idx.csv").writeText(rowNumbers.joinToString("\n"))
    }
}