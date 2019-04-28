package com.soywiz.korgw

import android.app.Activity
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.support.v4.util.Pools
import android.util.Log
import android.view.MotionEvent
import android.widget.LinearLayout
import com.soywiz.kds.Pool
import com.soywiz.kgl.CheckErrorsKmlGlProxy
import com.soywiz.kgl.KmlGl
import com.soywiz.kgl.KmlGlAndroid
import com.soywiz.klock.DateTime
import com.soywiz.korag.AGOpengl
import com.soywiz.korev.*
import com.soywiz.korio.Korio
import com.soywiz.korma.geom.setTo
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

abstract class KorgwActivity : Activity() {
    var gameWindow: AndroidGameWindow? = null
    private lateinit var mGLView: GLSurfaceView
    lateinit var ag: AGOpengl

    var fps: Int = 60

    protected val pauseEvent = PauseEvent()
    protected val resumeEvent = ResumeEvent()
    protected val destroyEvent = DestroyEvent()
    protected val renderEvent = RenderEvent()
    protected val initEvent = InitEvent()
    protected val disposeEvent = DisposeEvent()
    protected val fullScreenEvent = FullScreenEvent()
    protected val keyEvent = KeyEvent()
    protected val mouseEvent = MouseEvent()
    protected val touchEvent = TouchEvent()
    protected val dropFileEvent = DropFileEvent()

    //val touchEvents = Pool { TouchEvent() }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        println("---------------- KorgwActivity.onCreate(savedInstanceState=$savedInstanceState) --------------")
        Log.e("KorgwActivity", "onCreate")
        //println("KorgwActivity.onCreate")

        //ag = AGOpenglFactory.create(this).create(this, AGConfig())
        ag = object : AGOpengl() {
            //override val gl: KmlGl = CheckErrorsKmlGlProxy(KmlGlAndroid())
            override val gl: KmlGl = KmlGlAndroid()
            override val nativeComponent: Any get() = this@KorgwActivity
            override val gles: Boolean = true

            override fun repaint() {
                mGLView.invalidate()
            }
        }

        mGLView = object : GLSurfaceView(this) {
            val view = this

            init {
                var contextLost = false
                var surfaceChanged = false
                var initialized = false

                setEGLContextClientVersion(2)
                setRenderer(object : GLSurfaceView.Renderer {
                    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
                        //GLES20.glClearColor(0.0f, 0.4f, 0.7f, 1.0f)
                        println("---------------- GLSurfaceView.onSurfaceCreated($config) --------------")
                        contextLost = true
                    }

                    override fun onDrawFrame(unused: GL10) {
                        if (contextLost) {
                            contextLost = false
                            println("---------------- Trigger AG.contextLost --------------")
                            ag.contextLost()
                        }
                        if (!initialized) {
                            initialized = true
                            ag.setViewport(0, 0, width, height)
                            gameWindow?.dispatch(initEvent)
                        }
                        if (surfaceChanged) {
                            surfaceChanged = false
                            ag.setViewport(0, 0, width, height)
                            gameWindow?.dispatchReshapeEvent(0, 0, view.width, view.height)
                        }

                        //GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                        gameWindow?.coroutineDispatcher?.executePending()
                        gameWindow?.dispatch(renderEvent)
                    }

                    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
                        println("---------------- GLSurfaceView.onSurfaceChanged($width, $height) --------------")
                        //ag.contextVersion++
                        //GLES20.glViewport(0, 0, width, height)
                        surfaceChanged = true
                    }
                })
            }

            private val touchesEventPool = Pool<TouchEvent> { TouchEvent() }
            private val coords = MotionEvent.PointerCoords()
            private var lastTouchEvent: TouchEvent = TouchEvent()

            override fun onTouchEvent(ev: MotionEvent): Boolean {
                val gameWindow = gameWindow ?: return false

                val currentTouchEvent = synchronized(touchesEventPool) {
                    val currentTouchEvent = touchesEventPool.alloc()
                    currentTouchEvent.copyFrom(lastTouchEvent)

                    currentTouchEvent.startFrame(
                        when (ev.action) {
                            MotionEvent.ACTION_DOWN -> TouchEvent.Type.START
                            MotionEvent.ACTION_MOVE -> TouchEvent.Type.MOVE
                            MotionEvent.ACTION_UP -> TouchEvent.Type.END
                            else -> TouchEvent.Type.END
                        }
                    )

                    for (n in 0 until ev.pointerCount) {
                        ev.getPointerCoords(n, coords)
                        currentTouchEvent.touch(ev.getPointerId(n), coords.x.toDouble(), coords.y.toDouble())
                    }

                    lastTouchEvent.copyFrom(currentTouchEvent)
                    currentTouchEvent
                }

                gameWindow.coroutineDispatcher.dispatch(gameWindow.coroutineContext, Runnable {
                    gameWindow.dispatch(currentTouchEvent)
                    synchronized(touchesEventPool) { touchesEventPool.free(currentTouchEvent) }
                })
                return true
            }

            override fun onResume() {
                //Looper.getMainLooper().
                println("---------------- GLSurfaceView.onResume --------------")
                super.onResume()
            }

            override fun onPause() {
                println("---------------- GLSurfaceView.onPause --------------")
                super.onPause()
            }
        }

        setContentView(mGLView)

        Korio(this) {
            try {
                activityMain()
            } finally {
                println("KorgwActivity.activityMain completed!")
            }
        }
    }

    override fun onResume() {
        //Looper.getMainLooper().
        println("---------------- KorgwActivity.onResume --------------")
        super.onResume()
        mGLView?.onResume()
        gameWindow?.dispatchResumeEvent()
    }

    override fun onPause() {
        println("---------------- KorgwActivity.onPause --------------")
        super.onPause()
        mGLView?.onPause()
        gameWindow?.dispatchPauseEvent()
    }

    override fun onStop() {
        println("---------------- KorgwActivity.onStop --------------")
        super.onStop()
        gameWindow?.dispatchStopEvent()
    }

    override fun onDestroy() {
        println("---------------- KorgwActivity.onDestroy --------------")
        super.onDestroy()
        //mGLView?.
        //mGLView = null
        gameWindow?.dispatchDestroyEvent()
        //gameWindow?.close() // Do not close, since it will be automatically closed by the destroy event
        gameWindow = null
    }

    abstract suspend fun activityMain(): Unit
}

