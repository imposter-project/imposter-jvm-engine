package io.gatehill.imposter.config.support

import io.gatehill.imposter.config.S3FileDownloaderTest
import io.vertx.core.Handler
import org.testcontainers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
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
    fun startLocalStack(): LocalStackContainer {
        val localStack = LocalStackContainer(DockerImageName.parse("localstack/localstack:3"))
            .withServices("s3")
            .apply { start() }
        makeS3Client(localStack).createBucket(CreateBucketRequest.builder().bucket("test").build())
        return localStack
    }

    fun makeS3Client(localStack: LocalStackContainer): S3Client {
        return S3Client.builder()
            .forcePathStyle(true)
            .endpointOverride(URI.create(localStack.endpoint.toString()))
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
            .build()
    }

    fun uploadFileToS3(localStack: LocalStackContainer, baseDir: String, filePath: String) {
        val s3 = makeS3Client(localStack)

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
