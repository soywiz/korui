import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korui.*
import java.util.concurrent.CancellationException

fun main(args: Array<String>) = EventLoop.main {
	val image = ResourcesVfs["kotlin.png"].readBitmap()

	Application().frame("Hello Frame!") {
		val c1 = RGBA(220, 220, 220, 255)
		val c2 = RGBA(255, 255, 255, 255)

		image(Bitmap32(50, 50, { x, y -> if ((x + y) % 2 == 0) c1 else c2 })) {
			setSize(100.percent, 100.percent)
		}

		var askButton: Button? = null
		var loadImage: Image? = null

		vertical {
			width = 50.percent
			button("hello") {
				alert("hello")
			}.apply {
				width = 50.percent
			}
			askButton = button("What's your name?") {
				askButton?.text = prompt("What's your name?")
			}
			image(image).apply {
				setSize(width.scale(0.5), height.scale(0.5))
			}
			spacer()
			button("Load Image") {
				try {
					val file = dialogOpenFile()
					loadImage?.image = file.readBitmap()
				} catch (c: CancellationException) {
					loadImage?.image = null
				}
			}
			loadImage = image(image)
		}

		//image(Bitmap32(50, 50, { _, _ -> Colors.WHITE })) {
		//    setBoundsInternal(0, 0, 100, 100)
		//}
		/*
		button {
			top = 50.percent
			width = 50.percent
			height = 50.percent
			setBoundsInternal(0, 0, 100, 100)
			onClick {
				println("click!")
				spawn {
					println("click [work]!")
					alert("Button pressed!")
					try {
						val file = dialogOpenFile()
						println(file.readString())
					} catch (t: CancellationException) {
						println("cancelled!")
					}
				}
			}
		}
		*/
	}
}
