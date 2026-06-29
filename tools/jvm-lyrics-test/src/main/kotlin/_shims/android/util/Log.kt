// JVM shim for android.util.Log so the parsers can run off-device.
// Routes everything to stdout/stderr; signatures match the subset the parsers call.
package android.util

@Suppress("unused", "UNUSED_PARAMETER")
object Log {
    @JvmStatic fun v(tag: String?, msg: String?): Int = println("V/$tag: $msg").let { 0 }
    @JvmStatic fun d(tag: String?, msg: String?): Int = println("D/$tag: $msg").let { 0 }
    @JvmStatic fun d(tag: String?, msg: String?, tr: Throwable?): Int = println("D/$tag: $msg").let { 0 }
    @JvmStatic fun i(tag: String?, msg: String?): Int = println("I/$tag: $msg").let { 0 }
    @JvmStatic fun w(tag: String?, msg: String?): Int = println("W/$tag: $msg").let { 0 }
    @JvmStatic fun e(tag: String?, msg: String?): Int = System.err.println("E/$tag: $msg").let { 0 }
    @JvmStatic fun e(tag: String?, msg: String?, tr: Throwable?): Int =
        System.err.println("E/$tag: $msg").let { tr?.printStackTrace(); 0 }
}
