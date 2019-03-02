package com.example.jpctdemo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.threed.jpct.Camera;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Light;
import com.threed.jpct.Loader;
import com.threed.jpct.Matrix;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.RGBColor;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureInfo;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;
import com.threed.jpct.util.MemoryHelper;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends AppCompatActivity implements ScaleGestureDetector.OnScaleGestureListener {

    private GLSurfaceView mGLView;
    private ContentLoadingProgressBar progressBar;

    private World world = null;
    private Light light;

    private FrameBuffer frameBuffer = null;

    private Object3D[] object3Ds;
    private Bitmap bitmap;

    private ScaleGestureDetector gestureDec = null;

    private RGBColor back = new RGBColor(50, 50, 100);

    private float touchTurn = 0;
    private float touchTurnUp = 0;

    private float xpos = -1;
    private float ypos = -1;

    private float scale = 0f;

    private Object3D plane;

//    表示是否在放大
    private boolean isScale = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        mGLView = new GLSurfaceView(this);
        setContentView(R.layout.activity_main);
        mGLView = findViewById(R.id.surface);
        progressBar = findViewById(R.id.progress_bar);
        // Enable the OpenGL ES2.0 context
        mGLView.setEGLContextClientVersion(2);

        Texture.defaultToMipmapping(true);
        Texture.defaultTo4bpp(true);

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                progressBar.show();
                init();
            }
        });

        mGLView.setRenderer(new GLSurfaceView.Renderer() {

            @Override
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {}

            @Override
            public void onSurfaceChanged(GL10 gl, int width, int height) {
                Log.i("happy", "onSurfaceChanged: start");
                frameBuffer = null;
                frameBuffer = new FrameBuffer(width, height);
                Log.i("happy", "onSurfaceChanged: end");

                Matrix matrix = new Matrix();
                matrix.setColumn(0, 1.0f, 0.0f, 0.0f, 0.0f);
                matrix.setColumn(1, 0.0f, -1.0f, 0.0f, 0.0f);
                matrix.setColumn(2, 0.0f, 0.0f, -1.0f, 0.0f);
                matrix.setColumn(3, 0.0f, 0.0f, 0.0f, 1.0f);
                plane.setRotationMatrix(matrix);
            }

            @Override
            public void onDrawFrame(GL10 gl) {
//                plane.scale(scale);
                if (touchTurn != 0) {
                    plane.rotateY(touchTurn);
                    touchTurn = 0;
                }

                Log.i("zhong", "onDrawFrame: " + plane.getScale() +
                        " || " + plane.getRotationMatrix());
                if (scale != 0.0f) {
                    float tempScale = 1.0f + scale;
                    if (tempScale >= 0.0f)
                        plane.scale(tempScale);
                    else
                        plane.scale(1.0f);
                    scale = 0.0f;
                }

                if (touchTurnUp != 0) {
                    plane.rotateX(touchTurnUp);
                    touchTurnUp = 0;
                }

                frameBuffer.clear(back);
                if(world != null) {
                    world.renderScene(frameBuffer);
                    world.draw(frameBuffer);
                    frameBuffer.display();
                }
            }
        });

        gestureDec = new ScaleGestureDetector(this, this);
    }

    private void init() {
        new AsyncTask<Void, Void, Void>(){

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    Log.i("happy", "create start");
//                    object3Ds = Loader.loadOBJ(getStream("assets/pikachu/pikachu.obj"),
//                            getStream("assets/pikachu/pikachu.mtl"), 0.2f);
                    object3Ds = Loader.loadOBJ(getStream("assets/face/head3d.obj"),
                            getStream("assets/face/head3d.mtl"), 0.25f);

                    Log.i("happy", "object3Ds: end");

                    bitmap = BitmapFactory.decodeStream(getStream("assets/face/head3d.jpg"));

                    Log.i("happy", "bitmap: end");

                    Texture texture = new Texture(bitmap);
                    TextureManager.getInstance().addTexture("face", texture);

                    Log.i("happy", "Texture: end");

                    world = new World();

                    plane = object3Ds[0];
                    for (int i = 1;i < object3Ds.length;i++) {
                        plane.addChild(object3Ds[i]);
                        object3Ds[i].build();
                        object3Ds[i].strip();
                    }

                    plane.setTexture("face");
                    plane.setSpecularLighting(true);

                    plane.build();
                    plane.strip();

                    Log.i("happy", "obj build: end");

                    Log.v("zhong", object3Ds.length + " || onSurfaceChanged: ");

                    world.addObjects(object3Ds);

                    light = new Light(world);
                    light.enable();

                    light.setIntensity(250, 250, 250);
                    light.setPosition(SimpleVector.create(0, 0, -200));
                    light.setDiscardDistance(2000f);
                    world.setAmbientLight(20, 20, 20);


                    Camera cam = world.getCamera();
                    cam.moveCamera(Camera.CAMERA_MOVEOUT, 70);
                    cam.lookAt(plane.getTransformedCenter());

                    MemoryHelper.compact();

                    world.compileAllObjects();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                progressBar.setVisibility(View.GONE);
                mGLView.setVisibility(View.VISIBLE);
            }
        }.execute();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGLView.onPause();
    }

    /**
     * 获取输出流
     * @param file
     * @return
     * @throws IOException
     */
    private InputStream getStream(String file) throws IOException {
        InputStream inputStream;
        if (file.startsWith("assets/")) {
            String path = file.substring(7);
            inputStream = MainActivity.this.getAssets().open(path);
        } else {
            inputStream = new FileInputStream(file);
        }
        Log.e("obj", file);
        return inputStream;
    }

    /**
     * 触摸事件
     * @param me
     * @return
     */
    public boolean onTouchEvent(MotionEvent me) {
        gestureDec.onTouchEvent(me);

        if (me.getAction() == MotionEvent.ACTION_DOWN) {
            xpos = me.getX();
            ypos = me.getY();
            return true;
        }

        if (me.getAction() == MotionEvent.ACTION_UP) {
            xpos = -1;
            ypos = -1;
            touchTurn = 0;
            touchTurnUp = 0;
            return true;
        }

        if (me.getAction() == MotionEvent.ACTION_MOVE) {
            if (!isScale) {
                float xd = me.getX() - xpos;
                float yd = me.getY() - ypos;

                xpos = me.getX();
                ypos = me.getY();

                touchTurn = xd / -100f;
                touchTurnUp = yd / -100f;
            }
            return true;
        }

        try {
            Thread.sleep(15);
        } catch (Exception e) {
            // No need for this...
        }

        return super.onTouchEvent(me);
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float div = detector.getCurrentSpan() - detector.getPreviousSpan();
        div /= 200;
        scale += div;
        Log.v("zhong", "onScale: " + div + " || " + scale);
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        Log.i("zhong", "onScaleBegin: ");
        isScale = true;
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        Log.i("zhong", "onScaleEnd: ");
        isScale = false;
    }
}
