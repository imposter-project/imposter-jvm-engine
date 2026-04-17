package io.gatehill.imposter.config.support

import io.gatehill.imposter.config.S3FileDownloaderTest
import io.vertx.core.Handler
import org.ministack.testcontainers.MiniStackContainer
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.net.URI
import java.nio.file.Paths
import java.util.concurrent.CountDownLatch
import java.util.function.Consumer

object TestSupport {
    fun startMiniStack(): MiniStackContainer {
        val miniStack = MiniStackContainer("1.2.20")
            .apply { start() }
        makeS3Client(miniStack).createBucket(CreateBucketRequest.builder().bucket("test").build())
        return miniStack
    }

    fun makeS3Client(miniStack: MiniStackContainer): S3Client {
        return S3Client.builder()
            .forcePathStyle(true)
            .endpointOverride(URI.create(miniStack.endpoint))
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
            .build()
    }

    fun uploadFileToS3(miniStack: MiniStackContainer, baseDir: String, filePath: String) {
        val s3 = makeS3Client(miniStack)

        if (filePath.endsWith("/")) {
            s3.putObject(
                PutObjectRequest.builder().bucket("test").key(filePath).build(),
                software.amazon.awssdk.core.sync.RequestBody.fromString("")
            )
        } else {
            val specFilePath = Paths.get(S3FileDownloaderTest::class.java.getResource("$baseDir/$filePath")!!.toURI())
            s3.putObject(
                PutObjectRequest.builder().bucket("test").key(filePath).build(),
                specFilePath
            )
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
