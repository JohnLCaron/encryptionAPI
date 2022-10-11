package electionguard.publish


// import io.ktor.utils.io.errors.*
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CArrayPointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.value
import mu.KotlinLogging
import platform.posix.FILE
import platform.posix.fclose
import platform.posix.fread
import platform.posix.lstat
import platform.posix.stat

/** Read everything in the file and return as a ByteArray. */
@Throws(Exception::class)
private fun gulp(filename: String): ByteArray {
    return memScoped {
        val stat = alloc<stat>()
        // lstat(@kotlinx.cinterop.internal.CCall.CString __file: kotlin.String?,
        //   __buf: kotlinx.cinterop.CValuesRef<platform.posix.stat>?)
        // : kotlin.Int { /* compiled code */ }
        if (lstat(filename, stat.ptr) != 0) {
            checkErrno {mess -> throw Exception("Fail lstat $mess on $filename")}
        }
        val size = stat.st_size.toULong()
        val file = openFile(filename, "rb")
        val ba = readFromFile(file, size, filename)
        fclose(file)

        return@memScoped ba
    }
}

@Throws(Exception::class)
private fun readFromFile(file: CPointer<FILE>, nbytes : ULong, filename : String): ByteArray {
    return memScoped {
        val bytePtr: CArrayPointer<ByteVar> = allocArray(nbytes.toInt())

        // fread(
        //   __ptr: kotlinx.cinterop.CValuesRef<*>?,
        //   __size: platform.posix.size_t /* = kotlin.ULong */,
        //   __n: platform.posix.size_t /* = kotlin.ULong */,
        //   __stream: kotlinx.cinterop.CValuesRef<platform.posix.FILE /* = platform.posix._IO_FILE */>?)
        //   : kotlin.ULong { /* compiled code */ }
        val nread = fread(bytePtr, 1, nbytes, file)
        if (nread < 0u) {
            checkErrno { mess -> throw Exception("Fail read $mess on $filename") }
        }
        if (nread != nbytes) {
            throw Exception("Fail read $nread != $nbytes  on $filename")
        }
        return@memScoped bytePtr.readBytes(nread.toInt())
    }
}

/** read variable length (base 128) integer from a stream and return as an Int */
@Throws(Exception::class)
private fun readVlen(input: CPointer<FILE>, filename: String): Int {
    var ib: Int = readByte(input, filename)
    if (ib == 0) {
        return 0
    }

    var result = ib.and(0x7F)
    var shift = 7
    while (ib.and(0x80) != 0) {
        ib = readByte(input, filename)
        if (ib == -1) {
            return -1
        }
        val im = ib.and(0x7F).shl(shift)
        result = result.or(im)
        shift += 7
    }
    return result
}

/** read a single byte from a stream and return as an Int */
@Throws(Exception::class)
private fun readByte(file: CPointer<FILE>, filename: String): Int {
    return memScoped {
        val intPtr = alloc<IntVar>()
        val nread = fread(intPtr.ptr, 1, 1, file)
        if (nread < 0u) {
            checkErrno { mess -> throw Exception("Fail readByte $mess on $filename") }
        }
        return@memScoped intPtr.value
    }
}
