package com.tyron.builder.gradle.internal.res

import com.tyron.builder.internal.aapt.v2.Aapt2Exception
import com.tyron.builder.internal.aapt.AaptPackageConfig
import com.android.ide.common.resources.CompileResourceRequest
import com.tyron.builder.plugin.options.SyncOptions
import org.gradle.api.logging.Logger
import java.io.File

/**
 * Utilidades para reescribir excepciones de AAPT2 con contexto de CodeAssist.
 */

fun rewriteCompileException(
    exception: Aapt2Exception,
    request: CompileResourceRequest,
    errorFormatMode: SyncOptions.ErrorFormatMode,
    enableBlame: Boolean,
    logger: Logger
): Exception {
    return RuntimeException("AAPT2 compilation failed for ${request.inputFile}", exception)
}

fun rewriteLinkException(
    exception: Aapt2Exception,
    errorFormatMode: SyncOptions.ErrorFormatMode,
    mergeBlameDirectory: File?,
    manifestMergeBlameFile: File?,
    identifiedSourceSetMap: Map<File, String>?,
    logger: Logger
): Exception {
    return RuntimeException("AAPT2 linking failed", exception)
}
