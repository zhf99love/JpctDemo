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
import com.threed.jpct.GLSLShader;
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
    private Light light1, light2, light3;

    private FrameBuffer frameBuffer = null;

    private ScaleGestureDetector gestureDec = null;

    private RGBColor back = new RGBColor(50, 50, 100);

    private float touchTurn = 0;
    private float touchTurnUp = 0;

    private float xpos = -1;
    private float ypos = -1;

    private float scale = 0f;

    private Object3D mainFace;
    private Object3D[] facePoints;


    //    表示是否在放大
    private boolean isScale = false;

    private GLSLShader shader;

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
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            }

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
                mainFace.setRotationMatrix(matrix);
            }

            @Override
            public void onDrawFrame(GL10 gl) {
//                plane.scale(scale);
                if (touchTurn != 0) {
                    mainFace.rotateY(touchTurn);
                    touchTurn = 0;
                }

                Log.i("zhong", "onDrawFrame: " + mainFace.getScale() +
                        " || " + mainFace.getRotationMatrix());
                if (scale != 0.0f) {
                    float tempScale = 1.0f + scale;
                    if (tempScale >= 0.0f)
                        mainFace.scale(tempScale);
                    else
                        mainFace.scale(1.0f);
                    scale = 0.0f;
                }

                shader.setUniform("heightScale", scale);

                if (touchTurnUp != 0) {
                    mainFace.rotateX(touchTurnUp);
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
                    world = new World();

                    Log.i("happy", "create start");
                    facePoints = Loader.loadOBJ(getStream("assets/face/head3d.obj"),
//                    facePoints = Loader.loadOBJ(getStream("assets/headex/merge3d.obj"),
                            getStream("assets/face/head3d.mtl"), 0.2f);
//                            getStream("assets/headex/merge3d.obj.mtl"), 0.8f);
                    Log.i("happy", "object3Ds: end");
                    textureInit();
                    shader = new GLSLShader(
                            Loader.loadTextFile(getResources().openRawResource(R.raw.vertexshader_offset)),
                            Loader.loadTextFile(getResources().openRawResource(R.raw.fragmentshader_offset)));
                    shader.setStaticUniform("invRadius", 0.01f);

                    mainFace = facePoints[0];
//                    mainFace.calcTextureWrapSpherical();
                    mainFace.setTexture("face");
                    mainFace.setSpecularLighting(true);
                    mainFace.setShader(shader);
                    for (int i = 1; i < facePoints.length;i++) {
                        mainFace.addChild(facePoints[i]);
                        facePoints[i].build();
                        facePoints[i].strip();
                    }
                    mainFace.build();
                    mainFace.strip();

                    Log.i("happy", "obj build: end + " + facePoints.length);
                    world.addObjects(facePoints);
                    lightInit(world, mainFace);

                    Camera cam = world.getCamera();
                    cam.moveCamera(Camera.CAMERA_MOVEOUT, 80);
                    cam.lookAt(mainFace.getTransformedCenter());
                    world.compileAllObjects();
                    MemoryHelper.compact();
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

    /**
     * 贴图初始化
     * @throws Exception
     */
    private void textureInit() throws IOException {
        Bitmap bitmap1 = BitmapFactory.decodeStream(getStream("assets/face/head3d.jpg"));
//        Bitmap bitmap1 = BitmapFactory.decodeStream(getStream("assets/headex/merge3d.jpg"));
//        Bitmap bitmap2 = BitmapFactory.decodeStream(getStream("assets/headex/Bellus3D_logo_square.png"));
        Log.i("happy", "bitmap: end");
        Texture texture1 = new Texture(bitmap1);
//        Texture texture2 = new Texture(bitmap2);
        TextureManager.getInstance().addTexture("face", texture1);
//        TextureManager.getInstance().addTexture("logo", texture2);
        Log.i("happy", "Texture: end");
    }

    /**
     * 光线初始化
     * @param world
     */
    private void lightInit(World world, Object3D object3D) {
//        环境光
        world.setAmbientLight(150, 150, 150);

        SimpleVector vector = object3D.getTransformedCenter();
        vector.y += 50;
        vector.z -= 70;
        vector.x -= 70;

        light1 = new Light(world);
        light1.enable();
        light1.setAttenuation(10.0f);
        light1.setDiscardDistance(2.0f);
        light1.setIntensity(10, 10, 10);
        light1.setPosition(vector);

//        light2 = new Light(world);
//        light2.enable();
//        light2.setIntensity(60, 50, 50);
//        light2.setPosition(SimpleVector.create(2000, 0, 0));
////
//        light3 = new Light(world);
//        light3.enable();
//        light3.setIntensity(60, 50, 50);
//        light3.setPosition(SimpleVector.create(0, 2000, 0));
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
            Thread.sleep(10);
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
