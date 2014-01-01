package com.persesgames.jogl;

import com.jogamp.common.nio.Buffers;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.opengl.GLWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.*;
import java.nio.FloatBuffer;

/**
 * Date: 10/25/13
 * Time: 7:42 PM
 */
public class Renderer implements GLEventListener  {
    private final static Logger logger = LoggerFactory.getLogger(Renderer.class);

    private volatile boolean stopped    = false;
    private volatile boolean dirty      = true;

    private ShaderProgram shaderProgram;
    private ShaderProgram blendProgram;
    private ShaderProgram textureProgram;

    private final GLWindow glWindow;

    private float[]                 vertices = {
            -0.5f, -0.5f,
             0.5f, -0.5f,
             0.0f,  0.5f,
    };

    private FloatBuffer             fbVertices          = Buffers.newDirectFloatBuffer(vertices);

    private float[]                 txtVerts = {
            -0.8f, -0.8f,   0.0f, 0.0f,
            -0.4f, -0.8f,   1.0f, 0.0f,
            -0.4f, -0.4f,   1.0f, 1.0f,
            -0.8f, -0.4f,   0.0f, 1.0f,
    };

    private FloatBuffer             fbTxtVertices       = Buffers.newDirectFloatBuffer(txtVerts);
    private FloatBuffer             fbFullVertices      = Buffers.newDirectFloatBuffer( new float [] {
            -1f, -1f, -0.1f,
             1f, -1f, -0.1f,
             1f,  1f, -0.1f,
            -1f,  1f, -0.1f,
    } );

    private int                     width = 100, height = 100;

    private int                     vboVertices;
    private int                     txtVertices;
    private int                     fullVertices;
    private int                     frameBuffer;
    private int                     renderedTexture;

    private int                     textureUniformLocation;

    private Keyboard                keyboard;

    private int                     frameCount;
    private long                    frameTime;
    private long                    lastTiming;

    private long                    start = System.currentTimeMillis();
    public Renderer(GLWindow glWindow, Keyboard keyboard) {
        this.glWindow = glWindow;
        this.keyboard = keyboard;
    }

    public void stop() {
        stopped = true;
    }

    public void redraw() {
        dirty = true;
    }

    public void run() {
        Renderer.this.glWindow.display();

        while(!stopped) {
            if (dirty) {
                //logger.info("rendering+" + System.currentTimeMillis());
                Renderer.this.glWindow.display();
                //Renderer.this.glWindow.swapBuffers();
                dirty = true;
            } else {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    logger.warn(e.getMessage(), e);
                }
            }

            stopped = keyboard.isPressed(KeyEvent.VK_ESCAPE);
        }

        Renderer.this.glWindow.destroy();
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        logger.info("Chosen GLCapabilities: " + drawable.getChosenGLCapabilities());
        logger.info("INIT GL IS: " + gl.getClass().getName());
        logger.info("GL_VENDOR: " + gl.glGetString(GL.GL_VENDOR));
        logger.info("GL_RENDERER: " + gl.glGetString(GL.GL_RENDERER));
        logger.info("GL_VERSION: " + gl.glGetString(GL.GL_VERSION));

        int [] result = new int[1];
        gl.glGetIntegerv(GL2.GL_MAX_VERTEX_ATTRIBS, result, 0);
        logger.info("GL_MAX_VERTEX_ATTRIBS=" + result[0]);

        gl.setSwapInterval(1);

        shaderProgram = new ShaderProgram(gl, Util.loadAsText(getClass(), "simpleShader.vert"), Util.loadAsText(getClass(), "simpleShader.frag"));
        blendProgram = new ShaderProgram(gl, Util.loadAsText(getClass(), "blendShader.vert"), Util.loadAsText(getClass(), "blendShader.frag"));
        textureProgram = new ShaderProgram(gl, Util.loadAsText(getClass(), "textureShader.vert"), Util.loadAsText(getClass(), "textureShader.frag"));

        textureUniformLocation = textureProgram.getUniformLocation("u_texture");

        int[] tmpHandle = new int[3];
        gl.glGenBuffers(3, tmpHandle, 0);

        vboVertices = tmpHandle[0];
        txtVertices = tmpHandle[1];
        fullVertices = tmpHandle[2];

        // Select the VBO, GPU memory data, to use for vertices
        gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, vboVertices);

        // transfer data to VBO, this perform the copy of data from CPU -> GPU memory
        gl.glBufferData(GL.GL_ARRAY_BUFFER, vertices.length * 4, fbVertices, GL.GL_DYNAMIC_DRAW);

        // Select the VBO, GPU memory data, to use for vertices
        gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, txtVertices);

        // transfer data to VBO, this perform the copy of data from CPU -> GPU memory
        gl.glBufferData(GL.GL_ARRAY_BUFFER, txtVerts.length * 4, fbTxtVertices, GL.GL_STATIC_DRAW);

        // Select the VBO, GPU memory data, to use for vertices
        gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, fullVertices);

        // transfer data to VBO, this perform the copy of data from CPU -> GPU memory
        gl.glBufferData(GL.GL_ARRAY_BUFFER, 48, fbFullVertices, GL.GL_STATIC_DRAW);

        gl.glGenFramebuffers(1, tmpHandle, 0);

        frameBuffer = tmpHandle[0];

        // The texture we're going to render to
        gl.glGenTextures(1, tmpHandle, 0);
        renderedTexture = tmpHandle[0];

        // "Bind" the newly created texture : all future texture functions will modify this texture
        gl.glBindTexture(GL.GL_TEXTURE_2D, renderedTexture);

        // Give an empty image to OpenGL ( the last "0" )
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGB, 512, 512, 0, GL.GL_RGB, GL.GL_UNSIGNED_BYTE, null);

        // Poor filtering. Needed !
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {

    }

    @Override
    public void display(GLAutoDrawable drawable) {
        long frameStart = System.nanoTime();
        //logger.info("display+" + System.currentTimeMillis());

        GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, frameBuffer);
        gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0, GL.GL_TEXTURE_2D, renderedTexture, 0);

        gl.glViewport(0, 0, 512, 512);

        gl.glEnable(GL.GL_BLEND);
        gl.glDisable(GL.GL_DEPTH_TEST);
        //gl.glBlendEquation(GL.GL_FUNC_SUBTRACT);
        gl.glBlendFunc(GL.GL_DST_COLOR, GL.GL_ONE_MINUS_SRC_ALPHA);
        //gl.glBlendEquationSeparate(GL.GL_SRC_ALPHA, GL.GL_DST_ALPHA);

        // Clear screen
//        gl.glClearColor(0.1f, 0.1f, 0.1f, 0.1f);
//        gl.glClear(GL2ES2.GL_COLOR_BUFFER_BIT);


        blendProgram.begin();

        // Select the VBO, GPU memory data, to use for vertices
        gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, fullVertices);

        // Associate Vertex attribute 0 with the last bound VBO
        gl.glVertexAttribPointer(0 /* the vertex attribute */, 3,
                GL2ES2.GL_FLOAT, false /* normalized? */, 0 /* stride */,
                0 /* The bound VBO data offset */);


        gl.glEnableVertexAttribArray(0);

        //gl.glUniform1i(textureUniformLocation, 0);

        gl.glDrawArrays(GL2ES2.GL_TRIANGLE_FAN, 0, 4); //Draw the vertices as triangle

        gl.glDisableVertexAttribArray(0); // Allow release of vertex position memory

        blendProgram.end();
        gl.glDisable(GL.GL_BLEND);

        // Select the VBO, GPU memory data, to use for vertices
        gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, vboVertices);

        // Associate Vertex attribute 0 with the last bound VBO
        gl.glVertexAttribPointer(0 /* the vertex attribute */, 2,
                GL2ES2.GL_FLOAT, false /* normalized? */, 0 /* stride */,
                0 /* The bound VBO data offset */);

        float time = (System.currentTimeMillis() - start) / 1000f;
        float x = (float) (Math.sin(time) / 4f);
        float y = (float) (Math.cos(time) / 4f);

        fbVertices.put(0, -0.5f + x);
        fbVertices.put(1, -0.5f + y);
        fbVertices.put(2,  0.5f + x);
        fbVertices.put(3, -0.5f + y);
        fbVertices.put(4,  0.0f + x);
        fbVertices.put(5,  0.5f + y);

        // transfer data to VBO, this perform the copy of data from CPU -> GPU memory
        gl.glBufferData(GL.GL_ARRAY_BUFFER, vertices.length * 4, fbVertices, GL.GL_DYNAMIC_DRAW);

        shaderProgram.begin();

        gl.glEnableVertexAttribArray(0);

        gl.glDrawArrays(GL2ES2.GL_TRIANGLES, 0, 3); //Draw the vertices as triangle

        gl.glDisableVertexAttribArray(0); // Allow release of vertex position memory

        shaderProgram.end();

        /* Draw to screen */

        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
        gl.glViewport(0, 0, width, height);

        // Clear screen
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1f);
        gl.glClear(GL2ES2.GL_COLOR_BUFFER_BIT);

        shaderProgram.begin();

        gl.glEnableVertexAttribArray(0);

        gl.glDrawArrays(GL2ES2.GL_TRIANGLES, 0, 3); //Draw the vertices as triangle

        gl.glDisableVertexAttribArray(0); // Allow release of vertex position memory

        shaderProgram.end();

        textureProgram.begin();

        // Select the VBO, GPU memory data, to use for vertices
        gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, txtVertices);

        // Associate Vertex attribute 0 with the last bound VBO
        gl.glVertexAttribPointer(0 /* the vertex attribute */, 2,
                GL2ES2.GL_FLOAT, false /* normalized? */, 16 /* stride */,
                0 /* The bound VBO data offset */);

        // Associate Vertex attribute 0 with the last bound VBO
        gl.glVertexAttribPointer(1 /* the vertex attribute */, 2,
                GL2ES2.GL_FLOAT, false /* normalized? */, 16 /* stride */,
                8 /* The bound VBO data offset */);

        gl.glEnableVertexAttribArray(0);
        gl.glEnableVertexAttribArray(1);

        //gl.glUniform1i(textureUniformLocation, 0);

        gl.glDrawArrays(GL2ES2.GL_TRIANGLE_FAN, 0, 4); //Draw the vertices as triangle

        gl.glDisableVertexAttribArray(1);
        gl.glDisableVertexAttribArray(0); // Allow release of vertex position memory

        textureProgram.end();

        frameTime += (System.nanoTime() - frameStart);
        frameCount++;

        if (lastTiming < (System.currentTimeMillis() - 1000)) {
            logger.info("FPS: {}, drawing time/s: {}ns, per frame: {}", frameCount, frameTime, frameCount > 0 ? (frameTime/frameCount) : 0);

            frameTime  = 0L;
            frameCount = 0;
            lastTiming = System.currentTimeMillis();
        }
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
        logger.info("reshape+" + System.currentTimeMillis());

        GL2ES2 gl = drawable.getGL().getGL2ES2();

        this.width = w;
        this.height = h;
    }

}
