package com.mdd.prepaid_lib_flutter_null_safety

import android.content.Context
import java.io.File
import java.io.PrintWriter

class FileUtils(val context: Context) {

    val fileName = "android_mdd_lib.txt"

    fun createFile() {
        val privateStorage = context.getExternalFilesDir(null)
        // have the object build the directory structure, if needed.
        privateStorage?.mkdirs()
        // create a File object for the output file
        val file = File(privateStorage, fileName)

        // create a new file
        val isNewFileCreated: Boolean = file.createNewFile()

        if (isNewFileCreated) {
            println("$fileName is created successfully.")
        } else {
            println("$fileName already exists.")
        }
    }

    fun writeFile(inputText: String) {

        val privateStorage = context.getExternalFilesDir(null)
        // have the object build the directory structure, if needed.
        privateStorage?.mkdirs()
        // create a File object for the output file
        val file = File(privateStorage, fileName)

        // create a new file
        file.writeText(inputText)
    }

    fun deleteContent() {
        var privateStorage = File(context.getExternalFilesDir(null), fileName)

        val writer = PrintWriter(privateStorage)
        writer.print("")
        writer.close()
    }

    fun readFileAsLinesUsingUseLines(fileName: String): List<String> =
        File(context.getExternalFilesDir(null), fileName).useLines { it.toList() }
}