package org.dmlc.tracker.utils

import java.io.{File, FileOutputStream, InputStream}

import org.apache.spark.util.IntParam
import org.apache.spark.util.logging.{RollingFileAppender, SizeBasedRollingPolicy, TimeBasedRollingPolicy}
import org.apache.spark.{Logging, SparkConf}

import scala.util.control.ControlThrowable

private[tracker] class FileAppender(inputStream: InputStream, file: File, bufferSize: Int = 8192)
  extends Logging {
  @volatile private var outputStream: FileOutputStream = null
  @volatile private var markedForStop = false     // has the appender been asked to stopped
  @volatile private var stopped = false           // has the appender stopped

  // Thread that reads the input stream and writes to file
  private val writingThread = new Thread("File appending thread for " + file) {
    setDaemon(true)
    override def run() {
      try {
        appendStreamToFile()
      } catch {
        case ct: ControlThrowable =>
          throw ct
        case t: Throwable =>
          logError(s"Uncaught exception in thread ${Thread.currentThread().getName}", t)
          throw t
      }
    }
  }
  writingThread.start()

  /**
    * Wait for the appender to stop appending, either because input stream is closed
    * or because of any error in appending
    */
  def awaitTermination() {
    synchronized {
      if (!stopped) {
        wait()
      }
    }
  }

  /** Stop the appender */
  def stop() {
    markedForStop = true
  }

  /** Continuously read chunks from the input stream and append to the file */
  protected def appendStreamToFile() {
    try {
      logDebug("Started appending thread")
      openFile()
      val buf = new Array[Byte](bufferSize)
      var n = 0
      while (!markedForStop && n != -1) {
        n = inputStream.read(buf)
        if (n != -1) {
          appendToFile(buf, n)
        }
      }
    } catch {
      case e: Exception =>
        logError(s"Error writing stream to file $file", e)
    } finally {
      closeFile()
      synchronized {
        stopped = true
        notifyAll()
      }
    }
  }

  /** Append bytes to the file output stream */
  protected def appendToFile(bytes: Array[Byte], len: Int) {
    if (outputStream == null) {
      openFile()
    }
    outputStream.write(bytes, 0, len)
  }

  /** Open the file output stream */
  protected def openFile() {
    outputStream = new FileOutputStream(file, false)
    logDebug(s"Opened file $file")
  }

  /** Close the file output stream */
  protected def closeFile() {
    outputStream.flush()
    outputStream.close()
    logDebug(s"Closed file $file")
  }
}

private[tracker] object FileAppender extends Logging {

  def apply(inputStream: InputStream, file: File): FileAppender = {
    new FileAppender(inputStream, file)
  }
}
