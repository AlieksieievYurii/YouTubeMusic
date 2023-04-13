import org.gradle.api.Plugin
import org.gradle.api.Project

class FileEncryption implements Plugin<Project> {
    void apply(Project project) {
        project.task('decryptGoogleServicesFile') {
            def inputFile = new File("${project.rootDir}/google-services.json.encrypted")
            def outputFile = new File(project.projectDir, "google-services.json")
            doLast {
                if (outputFile.exists()) {
                    println 'google-services.json is already decrypted!'
                    return
                }

                if (!inputFile.exists())
                    throw new IllegalStateException("Input file does not exist! $inputFile")

                String key = System.env.ANDROID_ENCRYPTION_KEY
                if (key == null)
                    throw new IllegalStateException("You need to define system env variable: ANDROID_ENCRYPTION_KEY")

                if (key.length() != 16)
                    throw new IllegalStateException("The key length must equal 16 characters!")

                CryptoUtils.decrypt(key, inputFile, outputFile)
            }
        }

        project.task('encryptFile') {
            def inputFileInstance = null
            def key = null
            doFirst {
                if (!project.hasProperty('inputFile'))
                    throw new IllegalStateException('You need to define input file for the task: -PinputFile=<...>')

                inputFileInstance = new File(project.projectDir, project.inputFile)

                if (!inputFileInstance.exists())
                    throw new FileNotFoundException("Give file does not exist: $inputFileInstance")

                if (!inputFileInstance.isFile())
                    throw new IllegalStateException("Given path is not a file: $inputFileInstance")

                key = System.env.ANDROID_ENCRYPTION_KEY
                if (key == null)
                    throw new IllegalStateException("You need to define system env variable: ANDROID_ENCRYPTION_KEY")

                if (key.length() != 16)
                    throw new IllegalStateException("The key length must be more than 16 characters!")
            }

            doLast {
                def outputFile = new File(project.rootDir, inputFileInstance.name + '.encrypted')
                CryptoUtils.encrypt(key, inputFileInstance, outputFile)
            }
        }
    }
}