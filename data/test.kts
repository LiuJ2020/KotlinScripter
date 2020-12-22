import java.io.File

fun File.printPathAndSubdirs() {
    println(path)
    listFiles { file -> file.isDirectory }?.forEach {
        it.printPathAndSubdirs()
    }
}

File(".").printPathAndSubdirs()