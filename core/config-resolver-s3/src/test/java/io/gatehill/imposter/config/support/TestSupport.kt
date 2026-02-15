package io.gatehill.imposter.config.support

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import io.gatehill.imposter.config.S3FileDownloaderTest
import io.vertx.core.Handler
import org.testcontainers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
import java.nio.file.Paths
import java.util.concurrent.CountDownLatch
import java.util.function.Consumer

object TestSupport {
    fun startLocalStack(): LocalStackContainer {
        val localStack = LocalStackContainer(DockerImageName.parse("localstack/localstack:3"))
            .withServices("s3")
            .apply { start() }
        makeS3Client(localStack).createBucket("test")
        return localStack
    }

    fun makeS3Client(localStack: LocalStackContainer): AmazonS3 {
        return AmazonS3ClientBuilder.standard()
            .enablePathStyleAccess()
            .withEndpointConfiguration(
                AwsClientBuilder.EndpointConfiguration(
                    localStack.endpoint.toString(),
                    "us-east-1"
                )
            )
            .withCredentials(AWSStaticCredentialsProvider(BasicAWSCredentials("test", "test")))
            .build()
    }

    fun uploadFileToS3(localStack: LocalStackContainer, baseDir: String, filePath: String) {
        val s3 = makeS3Client(localStack)

        if (filePath.endsWith("/")){
            s3.putObject("test", filePath, "")
        } else {
            val specFilePath = Paths.get(S3FileDownloaderTest::class.java.getResource("$baseDir/$filePath")!!.toURI())
            s3.putObject("test", filePath, specFilePath.toFile())
        }
    }

    /**
     * Block the consumer until the handler is called.
     *
     * @param handlerConsumer the consumer of the handler
     * @param <T>             the type of the async result
     */
    @Throws(Exception::class)
    fun <T> blockWait(handlerConsumer: Consumer<Handler<T>>) {
        val latch = CountDownLatch(1)
        val handler = Handler { _: T -> latch.countDown() }
        handlerConsumer.accept(handler)
        latch.await()
    }
}
