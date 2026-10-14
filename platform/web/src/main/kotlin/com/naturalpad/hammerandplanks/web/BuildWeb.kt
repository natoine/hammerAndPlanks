package com.naturalpad.hammerandplanks.web

import com.github.xpenatan.gdx.backends.teavm.config.AssetFileHandle
import com.github.xpenatan.gdx.backends.teavm.config.TeaBuildConfiguration
import com.github.xpenatan.gdx.backends.teavm.config.TeaBuilder
import org.teavm.tooling.TeaVMTargetType
import org.teavm.tooling.TeaVMTool
import org.teavm.vm.TeaVMOptimizationLevel
import java.io.File

/** Construit la version web. Arguments : dossier assets, dossier de sortie. */
fun main(args: Array<String>) {
    require(args.size == 2) { "Usage : BuildWeb <assetsDir> <distDir>" }
    val config = TeaBuildConfiguration().apply {
        assetsPath.add(AssetFileHandle(args[0]))
        webappPath = File(args[1]).canonicalPath
        targetType = TeaVMTargetType.JAVASCRIPT
        htmlTitle = "Hammer and Planks"
    }
    TeaBuilder.config(config)
    val tool = TeaVMTool().apply {
        optimizationLevel = TeaVMOptimizationLevel.ADVANCED
        mainClass = WebLauncher::class.java.name
        setObfuscated(true)
    }
    TeaBuilder.build(tool)
}
