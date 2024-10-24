import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

buildscript {
    dependencies {
        classpath(libs.bundles.asm)
    }
}

plugins {
    id("java-library")
}

val artifactTypeAttribute = Attribute.of("artifactType", String::class.java)
val repackagedAttribute = Attribute.of("repackaged", Boolean::class.javaObjectType)


val repackage: Configuration by configurations.creating {
    attributes.attribute(repackagedAttribute, true)
}

// Configure project's dependencies
abstract class MyAbsRepackager : TransformAction<TransformParameters.None> {

    @InputArtifact
    abstract fun getInputArtifact(): Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = getInputArtifact().get().asFile
        val repackPrefix = if (input.name.contains(Regex("vineflower"))) {
            "recaf/relocation/libs/vineflower/"
        } else if (input.name.contains(Regex("java-decompiler-engine"))) {
            "recaf/relocation/libs/fernflower/"
        } else if (input.name.contains(Regex("forgeflower"))) {
            "recaf/relocation/libs/forgeflower/"
        } else {
            ""
        }
        val output = outputs.file(
            input.name.let {
                if (it.endsWith(".jar"))
                    it.replaceRange(it.length - 4, it.length, "-repackaged.jar")
                else
                    "$it-repackaged"
            }
        )
        println("Repackaging ${input.absolutePath} to ${output.absolutePath}")
        ZipOutputStream(output.outputStream()).use { zipOut ->
            relocate(zipOut, ZipFile(input), repackPrefix, null)
            zipOut.flush()
        }
    }


    fun relocate(
        zipOut: ZipOutputStream,
        zipFile: ZipFile,
        repackPrefix: String,
        prententriesSet: MutableSet<String>?
    ) {
        zipFile.use { zipIn ->
            val entriesList = zipIn.entries().toList()
            val entriesSet = entriesList.mapTo(mutableSetOf()) { it.name }
            if (prententriesSet != null) {
                entriesSet.addAll(prententriesSet)
            }
            for (entry in entriesList) {
                if (entry.name.endsWith("module-info.class")) {
                    continue
                }
                val newName =
                    if (!entry.isDirectory && entry.name.startsWith("META-INF/services/")) {
                        "META-INF/services/" + repackPrefix.replace(
                            '/',
                            '.'
                        ) + entry.name.substring(entry.name.lastIndexOf('/') + 1)
                    } else if (entry.name.contains("/") && !entry.name.startsWith("META-INF/")) {
                        repackPrefix + entry.name
                    } else {
                        entry.name
                    }
                zipOut.putNextEntry(ZipEntry(newName))
                if (!entry.isDirectory) {
                    if (entry.name.endsWith(".class")) {
                        val writer = ClassWriter(0)
                        ClassReader(zipIn.getInputStream(entry)).accept(
                            ClassRemapper(
                                writer,
                                object : Remapper() {
                                    override fun map(internalName: String?): String? {
                                        if (internalName == null) return null
                                        return if (entriesSet.contains("$internalName.class")) {
                                            repackPrefix + internalName
                                        } else {
                                            internalName
                                        }
                                    }
                                }
                            ),
                            0
                        )
                        zipOut.write(
                            writer.toByteArray()
                        )
                    } else if (entry.name.endsWith(".jar")) {
                        val tempJar = File.createTempFile("recaf-relocate-", ".jar")

                        // zipIn.getInputStream(entry).copyTo(fo)
                        tempJar.writeBytes(zipIn.getInputStream(entry).readBytes())
                        ByteArrayOutputStream().use { bo ->
                            ZipOutputStream(bo).use { zos ->
                                relocate(zos, ZipFile(tempJar), repackPrefix, entriesSet)
                                zos.flush()
                            }
                            bo.flush()
                            zipOut.write(bo.toByteArray())
                        }
                        if (tempJar.exists()) {
                            tempJar.delete()
                        }
                    } else if (entry.name.contains("/") && entry.name.startsWith("META-INF/services/")) {
                        val bufferedReader = BufferedReader(InputStreamReader(zipIn.getInputStream(entry)))
                        zipOut.write(
                            repackPrefix.replace(
                                '/',
                                '.'
                            ).toByteArray(Charsets.UTF_8)
                        )
                        zipOut.write(bufferedReader.readLine().toByteArray(Charsets.UTF_8))
                    } else {
                        zipIn.getInputStream(entry).copyTo(zipOut)
                    }
                }
                zipOut.closeEntry()
            }
        }
    }
}



dependencies {
    attributesSchema {
        attribute(repackagedAttribute)
        // attribute(f_repackagedAttribute)
    }
    artifactTypes.getByName("jar") {
        attributes.attribute(repackagedAttribute, false)
        // attributes.attribute(f_repackagedAttribute, false)
    }
    registerTransform(MyAbsRepackager::class) {
        from.attribute(repackagedAttribute, false).attribute(artifactTypeAttribute, "jar")
        to.attribute(repackagedAttribute, true).attribute(artifactTypeAttribute, "jar")
    }
    repackage(libs.vineflower)
    repackage(libs.fernflower)
    repackage(libs.forgeflower)

    api(files(repackage.files))
}