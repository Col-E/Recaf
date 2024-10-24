import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

buildscript {
    repositories {
        mavenCentral()
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
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
abstract class MyRepackager : TransformAction<TransformParameters.None> {
    @InputArtifact
    abstract fun getInputArtifact(): Provider<FileSystemLocation>
    override fun transform(outputs: TransformOutputs) {
        val input = getInputArtifact().get().asFile
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
            relocate(zipOut, ZipFile(input), "recaf/relocation/libs/vineflower/")
            zipOut.flush()
        }
    }

    fun relocate(zipOut: ZipOutputStream, zipFile: ZipFile, repackPrefix: String) {
        zipFile.use { zipIn ->
            val entriesList = zipIn.entries().toList()
            val entriesSet = entriesList.mapTo(mutableSetOf()) { it.name }
            for (entry in entriesList) {
                val newName = if (entry.name.contains("/") && !entry.name.startsWith("META-INF/")) {
                    repackPrefix + entry.name
                } else {
                    entry.name
                }
                zipOut.putNextEntry(ZipEntry(newName))
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
                            relocate(zos, ZipFile(tempJar), repackPrefix)
                            zos.flush()
                        }
                        bo.flush()
                        zipOut.write(bo.toByteArray())
                    }
                    if (tempJar.exists()) {
                        tempJar.delete()
                    }
                } else {
                    zipIn.getInputStream(entry).copyTo(zipOut)
                }
                zipOut.closeEntry()
            }
        }
    }
}


dependencies {
    attributesSchema {
        attribute(repackagedAttribute)
    }
    artifactTypes.getByName("jar") {
        attributes.attribute(repackagedAttribute, false)
    }
    registerTransform(MyRepackager::class) {
        from.attribute(repackagedAttribute, false).attribute(artifactTypeAttribute, "jar")
        to.attribute(repackagedAttribute, true).attribute(artifactTypeAttribute, "jar")
    }

    repackage(libs.vineflower)
    api(files(repackage.files))
}