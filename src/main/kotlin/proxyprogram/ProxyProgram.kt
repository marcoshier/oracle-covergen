package org.openrndr.extra.proxyprogram

import kotlinx.coroutines.runBlocking
import org.openrndr.*
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class ProxyApplication(val proxy: Application) : Application() {
    override var clipboardContents: String?
        get() = proxy.clipboardContents
        set(value) {
            proxy.clipboardContents = value
        }
    override var configuration: Configuration
        get() = proxy.configuration
        set(value) {
            error("cannot set configuration through proxy")
        }
    override var cursorHideMode: MouseCursorHideMode
        get() = TODO("Not yet implemented")
        set(value) {}
    override var cursorPosition: Vector2
        get() = proxy.cursorPosition
        set(value) {
            error("cannot set cursor position through proxy")
        }
    override var cursorType: CursorType
        get() = TODO("Not yet implemented")
        set(value) {}
    override var cursorVisible: Boolean
        get() = proxy.cursorVisible
        set(value) {
            proxy.cursorVisible = value
        }
    override val pointers: List<Pointer>
        get() = TODO("Not yet implemented")
    override var presentationMode: PresentationMode
        get() = proxy.presentationMode
        set(value) {
            proxy.presentationMode = value
        }
    override var program: Program = Program()

    override val seconds: Double
        get() = proxy.seconds
    override var windowContentScale: Double
        get() = proxy.windowContentScale
        set(value) {
            proxy.windowContentScale = value
        }
    override var windowMultisample: WindowMultisample
        get() = proxy.windowMultisample
        set(value) {
            proxy.windowMultisample = value
        }
    override var windowPosition: Vector2
        get() = TODO("Not yet implemented")
        set(value) {}
    override var windowResizable: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    override var windowSize: Vector2
        get() = TODO("Not yet implemented")
        set(value) {}
    override var windowTitle: String
        get() = TODO("Not yet implemented")
        set(value) {}

    override fun exit() {
        TODO("Not yet implemented")
    }

    override fun loop() {
        TODO("Not yet implemented")
    }

    override fun requestDraw() {
        TODO("Not yet implemented")
    }

    override fun requestFocus() {
        TODO("Not yet implemented")
    }

    override suspend fun setup() {
        TODO("Not yet implemented")
    }
}


class ProxyApplicationBuilder : ApplicationBuilder {
    val parentProgram: Program
    val clientArea: Rectangle?
    override val configuration: Configuration = Configuration()

    constructor(parentProgram: Program, clientArea: Rectangle?) : super() {
        this.parentProgram = parentProgram
        this.clientArea = clientArea
        this.displays = emptyList()
        this.program = Program()
    }

    override val applicationBase: ApplicationBase
        get() = TODO("Not yet implemented")
    override val displays: List<Display>

    override var program: Program
    override fun application(build: ApplicationBuilder.() -> Unit): Nothing {
        TODO("Not yet implemented")
    }

    override fun applicationAsync(build: ApplicationBuilder.() -> Unit): Nothing {
        TODO("Not yet implemented")
    }

    override fun configure(init: Configuration.() -> Unit) {
        //
    }

    override fun program(init: suspend Program.() -> Unit): Program {

        program = object : Program() {
            override suspend fun setup() {
                init()
            }
        }

        return program
    }

    override fun Program.program(init: Program.() -> Unit): Nothing {
        TODO("Not yet implemented")
    }

    fun build(): ProxyApplication {
        return ProxyApplication(parentProgram.application).apply {
            program = this@ProxyApplicationBuilder.program
            program.application = this
            program.drawer = parentProgram.drawer

            program.mouse.dragged.postpone = false
            program.mouse.buttonUp.postpone = false
            program.mouse.buttonDown.postpone = false
            program.mouse.scrolled.postpone = false

            parentProgram.mouse.dragged.listen(program.mouse.dragged)
            parentProgram.mouse.buttonUp.listen(program.mouse.buttonUp)
            parentProgram.mouse.buttonDown.listen(program.mouse.buttonDown)
            parentProgram.mouse.scrolled.listen(program.mouse.scrolled)

            program.keyboard.character.postpone = false
            program.keyboard.keyDown.postpone = false
            program.keyboard.keyUp.postpone = false


            program.width = clientArea?.width?.toInt() ?: parentProgram.width
            program.height = clientArea?.height?.toInt() ?: parentProgram.height

            parentProgram.keyboard.character.listen(program.keyboard.character)
            parentProgram.keyboard.keyUp.listen(program.keyboard.keyUp)
            parentProgram.keyboard.keyDown.listen(program.keyboard.keyDown)

            runBlocking {
                this@ProxyApplicationBuilder.program.setup()
            }
        }
    }

}


@OptIn(ExperimentalContracts::class)
fun proxyApplication(parentApplication: Application? = null, config: ApplicationBuilder.() -> Unit): Program {
    contract {
        callsInPlace(config, InvocationKind.EXACTLY_ONCE)
    }

    if (parentApplication != null) {
        val appBuilder = ProxyApplicationBuilder(parentApplication.program, null)
        appBuilder.config()
        appBuilder.build()
        require(appBuilder.program != null)
        return appBuilder.program
    } else {
        ApplicationBuilderJVM().apply {
            config()
            return run().program
        }
    }
}

@OptIn(ExperimentalContracts::class)
fun Program.proxyApplication(config: ApplicationBuilder.() -> Unit): Program {
    contract {
        callsInPlace(config, InvocationKind.EXACTLY_ONCE)
    }
    return proxyApplication(this.application, config)
}


