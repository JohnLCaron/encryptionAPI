package electionguard.publish

import electionguard.ballot.*
// import io.ktor.utils.io.errors.*
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CArrayPointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.set
import platform.posix.FILE
import platform.posix.fclose
import platform.posix.fflush
import platform.posix.fwrite

@Throws(Exception::class)
private fun writeToFile(file: CPointer<FILE>, filename: String, buffer: ByteArray) {
    memScoped {
        val bytePtr: CArrayPointer<ByteVar> = allocArray(buffer.size)
        // TODO avoid copy
        buffer.forEachIndexed { index, element -> bytePtr[index] = element }

        // fwrite(
        //    __ptr: kotlinx.cinterop.CValuesRef<*>?,
        //    __size: platform.posix.size_t /* = kotlin.ULong */,
        //    __n: platform.posix.size_t /* = kotlin.ULong */,
        //    __s: kotlinx.cinterop.CValuesRef<platform.posix.FILE /* = platform.posix._IO_FILE */>?)
        // : kotlin.ULong { /* compiled code */ }
        val nwrite = fwrite(bytePtr, 1, buffer.size.toULong(), file)
        if (nwrite < 0u) {
            checkErrno { mess -> throw Exception("Fail fwrite $mess on $filename") }
        }
        if (nwrite != buffer.size.toULong()) {
            throw Exception("Fail fwrite $nwrite != $buffer.size  on $filename")
        }
    }
}

@Throws(Exception::class)
private fun writeVlen(file: CPointer<FILE>, filename: String, length: Int): Int {
    var value = length
    var count = 0

    // stolen from protobuf.CodedOutputStream.writeRawVarint32()
    while (true) {
        value = if (value and 0x7F.inv() == 0) {
            writeByte(file, filename, value.toByte())
            count++
            break
        } else {
            writeByte(file, filename, (value and 0x7F or 0x80).toByte())
            count++
            value ushr 7
        }
    }
    return count + 1
}

////////////////////////////////////////////////////////////

@Throws(Exception::class)
private fun writeByte(file: CPointer<FILE>, filename: String, b: Byte) {
    memScoped {
        val bytePtr: CArrayPointer<ByteVar> = allocArray(1)
        // TODO avoid copy
        bytePtr[0] = b

        val nwrite = fwrite(bytePtr, 1, 1, file)
        if (nwrite < 0u) {
            checkErrno { mess -> throw Exception("Fail writeByte $mess on $filename") }
        }
        if (nwrite.compareTo(1u) != 0) {
            throw Exception("Fail writeByte2 $nwrite on $filename")
        }
    }
}
