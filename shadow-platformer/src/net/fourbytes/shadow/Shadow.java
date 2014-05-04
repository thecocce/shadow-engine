package net.fourbytes.shadow;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import net.fourbytes.shadow.Input.Key;
import net.fourbytes.shadow.Input.Key.Triggerer;
import net.fourbytes.shadow.Input.KeyListener;
import net.fourbytes.shadow.Input.TouchPoint;
import net.fourbytes.shadow.Input.TouchPoint.TouchMode;
import net.fourbytes.shadow.map.Converter;
import net.fourbytes.shadow.mod.ModManager;
import net.fourbytes.shadow.network.NetClient;
import net.fourbytes.shadow.network.NetServer;
import net.fourbytes.shadow.network.NetStream;
import net.fourbytes.shadow.utils.*;
import net.fourbytes.shadow.utils.backend.BackendHelper;

import java.io.File;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URLDecoder;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;

public final class Shadow implements ApplicationListener, InputProcessor, KeyListener {

	public static Random rand = new Random();
	
	public static Level level;
	public static ControllerHelper controllerHelper;
	
	public static Camera cam;
	public static float dispw = 1f;
	public static float disph = 1f;
 	public static byte viewmode = ViewModes.def;
	/**
	 * View Fixed Factor
	 */
	public static float viewff = 32f;
	public static float vieww = 1f;
	public static float viewh = 1f;
	public static float touchw = 1f;
	public static float touchh = 1f;
	public static SpriteBatch spriteBatch;
	
	public static int frames = 0;
	public static long lastfmicro = 0;
	public static int fps = 0;
	public static int efps = 0;
	
	public static boolean isAndroid = false;
	public static boolean isOuya = false;
	public static boolean gdxpaused = false;
	
	public static int loadstate = 0;
	public static int loadtick = 0;
	public static int[][] loadticks = {{0, 1, 2, 3, 4, 5, 6, 7}};
	
	public static String clientID = getSecureRandomBytes(512);
	public static NetStream client;
	public static NetStream server;

	public static boolean record = false;
	public static String recordDirName;

	public Shadow() {
		super();
	}
	
	/**
	 * Creates an string containing an given amount of random hexadecimal bytes. <br> 
	 * It's using SecureRandom instead of Java's default Random.
	 * @param amount Amount of random bytes
	 * @return Random client identifier.
	 */
	public static String getSecureRandomBytes(int amount) {
		SecureRandom random = new SecureRandom();
		byte[] bytes = new byte[amount];
		random.nextBytes(bytes);
		
		String str = "";
		for (byte aByte : bytes) {
			str += Integer.toHexString(aByte & 0xFF);
		}
		return str;
	}

	public static FileHandle dir;
	public static FileHandle getDir(String subdir) {
		if (isAndroid) {
			if (dir == null) {
				dir = Gdx.files.external("shadowenginetest");
			}
			if (subdir == null || subdir.isEmpty()) {
				return dir;
			}
			FileHandle child = dir.child(subdir);
			child.mkdirs();
			return child;
		} else {
			if (dir == null) {
				String path = "";
				try {
					String rawpath = Shadow.class.getProtectionDomain().getCodeSource().getLocation().getPath();
					path = URLDecoder.decode(rawpath, "UTF-8");
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				
				dir = Gdx.files.absolute(path).parent();
			}
			FileHandle child = dir.child(subdir);
			child.mkdirs();
			return child;
		}
	}

	@Override
	public void create() {
		Options.setup();

		Gdx.input.setCatchBackKey(true);
		Gdx.input.setCatchMenuKey(true);
		UncaughtExceptionHandler eh = new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				try {
					File dir = getDir("logs").file();
					File logfile = new File(dir, "log_"+(new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()))+".txt");
					if (logfile.createNewFile()) {
						PrintStream fos = new PrintStream(logfile);
						e.printStackTrace(fos);
						fos.close();
					}
				} catch (Throwable e1) {
					e1.printStackTrace();
				}
				e.printStackTrace();
				System.exit(1);
			}
		};

		Thread.currentThread().setUncaughtExceptionHandler(eh);
		Thread.setDefaultUncaughtExceptionHandler(eh);

		dispw = Gdx.graphics.getWidth();
		disph = Gdx.graphics.getHeight();

		//TODO replace ViewModes.def
		viewmode = ViewModes.def;

		//Alternate values for view: vieww = 12.5f; viewh = 15f;
		switch (viewmode) {
		case ViewModes.dynamic:
			vieww = dispw/viewff;
			viewh = disph/viewff;
		case ViewModes.fixedh:
			vieww = dispw/viewff;
			break;
		case ViewModes.fixedw:
			viewh = disph/viewff;
			break;
		default:
			break;
		}
		
		touchh = 7f;
		touchw = touchh*dispw/disph;

		Gdx.input.setInputProcessor(this);
		Input.setUp();
		Input.keylisteners.add(this);

		cam = new Camera();
		resize();
	}

	@Override
	public void dispose() {
		ModManager.dispose();
		Gdx.input.setInputProcessor(null);
		if (Shadow.isAndroid && !Shadow.isOuya) {
			System.exit(0);
		}
		//TODO: save data
		//TODO: cleanup resources
	}

	@Override
	public void render() {
		subtick();
		
		if (loadstate == 0) {
			return;
		}

		tick();

		/*
		Sidenote: LightSystem.render() is rendering, yes,
		but it's rendering to another FrameBuffer than the
		default one and switching "back" while rendering
		to another FBO (f.e. when  == true) glitches out.
		 */
		if (level != null) {
			Level llevel = level;
			while (llevel != null && llevel instanceof MenuLevel) {
				llevel = ((MenuLevel)llevel).bglevel;
			}
			if (llevel != null && llevel.lights != null) {
				llevel.lights.render();
			}
		}

		if (Options.getBoolean("gfx.clear", true)) {
			Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		}

		spriteBatch.setColor(1f, 1f, 1f, 1f);
		ModManager.preRender();
		Input.isInMenu = level != null && level instanceof MenuLevel;
		cam.render();
		ModManager.postRender();

		if (record) {
			ScreenshotUtil.frameRecord();
		}

		long time = System.currentTimeMillis();
		
		frames++;
		if (lastfmicro == 0) {
			lastfmicro = time;
		}
		if (time - lastfmicro >= 1000) {
			lastfmicro = time;
			fps = frames;
			frames = 0;
		}

		efps = (int)(1f/Gdx.graphics.getDeltaTime());
	}
	
	public void subtick() {
		while (Gdx.graphics == null) {
			Thread.yield();
		}
		if (loadstate == 0) {
			//Gdx.graphics.setVSync(true);

			ShaderProgram.pedantic = false;

			ShaderProgram defaultShader = ShaderHelper.loadShader("shaders/default");
			ShaderHelper.addShader(defaultShader);

			ShaderProgram noiseOffsetShader = ShaderHelper.loadShader("shaders/noiseoffs");
			ShaderHelper.addShader(noiseOffsetShader, "noiseoffs");

			spriteBatch = new SpriteBatch(4096);

			ShaderHelper.resetCurrentShader();

			Images.loadBasic();

			//Init controller helper here as JGLFW needs a window before doing controller stuff
			controllerHelper = new ControllerHelper();

			//At this point it's safe to assume that most stuff needeed for a backend setup is set up.
			BackendHelper.setUp();
			
			loadstate = 1;
			loadtick = 0;

			//Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f);
			//Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
			
			//cam.render();
		} else if (loadstate == 1) {
			if (loadtick == loadticks[0][0]) {
				resize();
			}
			if (loadtick == loadticks[0][1]) {
				//Fonts
				Fonts.load();
			}
			if (loadtick == loadticks[0][2]) {
				//More images
				Images.loadImages();
				//Sounds
				Sounds.loadSounds();
			}
			if (loadtick == loadticks[0][3]) {
				//Set up various smaller values, options or machine-dependent / Android-only stuff.
				if (isAndroid) {
					GameObject.pixffac = 2;
					Level.maxParticles = 128;
					Camera.blursize = 4f;
					resize();
				}
			}
			if (loadtick == loadticks[0][4]) {
				if (!Converter.convertOnly && !isAndroid) {
					//TODO Set up streams
					client = new NetClient();
					server = new NetServer();
				}
			}
			if (loadtick == loadticks[0][5]) {
				//TODO add random stuff to load
			}
			if (loadtick == loadticks[0][6]) {
				ModManager.loader.init(null);
				ModManager.create();
			}
			if (loadtick == loadticks[0][7]) {
				//Jump into first level (TitleLevel).
				if (Converter.convertOnly) {
					System.out.println("Starting internal converter...");

					Converter.convertAll();

					Gdx.app.exit();
				} else {
					level = new TitleLevel();
				}
				loadstate = 2;
			}
			loadtick++;
		}
	}
	
	public void tick() {
		ModManager.preTick();
		controllerHelper.tick();
		Input.tick();
		if (!Converter.convertOnly && level != null) {
			level.tick();
		}
		if (!Converter.convertOnly && server != null) {
			server.tick();
		}
		if (!Converter.convertOnly && client != null) {
			client.tick();
		}
		ModManager.postTick();
	}
	
	@Override
	public void resize(int width, int height) {
		resize();
	}
	
	public static void resize() {
		if (Gdx.graphics != null) {
			dispw = Gdx.graphics.getWidth();
			disph = Gdx.graphics.getHeight();

			switch (viewmode) {
			case ViewModes.dynamic:
				break;
			case ViewModes.fixedh:
				viewh = vieww*disph/dispw;
				break;
			case ViewModes.fixedw:
				vieww = viewh*dispw/disph;
				break;
			case ViewModes.fixed:
				vieww = dispw/viewff;
				viewh = disph/viewff;
				break;
			case ViewModes.auto:
				vieww = dispw/viewff;
				viewh = disph/viewff;
				if (isAndroid || (dispw/viewff >= 21.75f && disph/viewff >= 18f)) {
					vieww = vieww/1.5f;
					viewh = viewh/1.5f;
				}
				break;
			default:
				break;
			}
			
			touchw = touchh*dispw/disph;
			cam.resize();
			Input.resize();

			ShaderHelper.set("s_resolution", dispw, disph);

			if (Camera.tmpFB != null) {
				Camera.tmpFB.dispose();
			}
			Camera.tmpFB = new FrameBuffer(Pixmap.Format.RGB888,
					(int)dispw, (int)disph, false);
			Camera.tmpFB.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear,
					Texture.TextureFilter.Linear);

			if (Camera.blurFB != null) {
				Camera.blurFB.dispose();
			}
			Camera.blurFB = new FrameBuffer(Pixmap.Format.RGB888,
					(int)(dispw/Camera.blursize), (int)(disph/Camera.blursize), false);
			Camera.blurFB.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear,
					Texture.TextureFilter.Linear);

			if (Camera.blurXFB != null) {
				Camera.blurXFB.dispose();
			}
			Camera.blurXFB = new FrameBuffer(Pixmap.Format.RGB888,
					(int)(dispw/Camera.blursize), (int)(disph/Camera.blursize), false);
			Camera.blurXFB.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear,
					Texture.TextureFilter.Linear);

			if (LightSystem.lightFB != null) {
				LightSystem.lightFB.dispose();
				LightSystem.lightFB = null;
			}
		}
	}

	@Override
	public void pause() {
		gdxpaused = true;
	}

	@Override
	public void resume() {
		gdxpaused = false;
	}

	@Override
	public boolean keyDown(int keycode) {
		boolean handle = false;
		for (Input.Key k : Input.all) {
			for (int id : k.keyid) {
				if (id == keycode) {
					k.triggerer = Triggerer.KEYBOARD;
					k.nextState = true;
					handle = true;
				}
			}
		}
		return handle;
	}

	@Override
	public boolean keyUp(int keycode) {
		boolean handle = false;
		for (Input.Key k : Input.all) {
			for (int id : k.keyid) {
				if (id == keycode) {
					k.triggerer = Triggerer.KEYBOARD;
					k.nextState = false;
					handle = true;
				}
			}
		}
		return handle;
	}

	@Override
	public boolean keyTyped(char c) {
		return false;
	}
	
	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		boolean handle = false;
		if (!Input.isAndroid) {
			pointer = -1;
		}
		//System.out.println("X: "+screenX+"; Y: "+screenY+"; P: "+pointer+"; B: "+button+"; M: D");
		if (Input.isAndroid && !Input.isOuya && !Input.isInMenu) {
			for (Input.Key k : Input.all) {
				if (k.rec.contains(screenX, screenY)) {
					k.triggerer = Triggerer.SCREEN;
					k.nextState = true;
					k.pointer = pointer;
					TouchPoint tp = new TouchPoint(screenX, screenY, pointer, button, TouchMode.KeyInput);
					if (Input.touches.containsValue(pointer, true)) {
						Input.touches.remove(pointer);
					}
					Input.touches.put(pointer, tp);
					handle = true;
				}
			}
		}
		
		if (!handle && level != null && level.ready && cam.camrec != null) {
			TouchPoint tp = new TouchPoint(screenX, screenY, pointer, button, TouchMode.Cursor);
			Input.touches.put(pointer, tp);
			handle = true;
		}
		
		return handle;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		if (!Input.isAndroid) {
			pointer = -1;
		}
		if (Input.isAndroid && !Input.isOuya && !Input.isInMenu) {
			//System.out.println("X: "+screenX+"; Y: "+screenY+"; P: "+pointer+"; B: "+button+"; M: U");
			for (Input.Key k : Input.all) {
				if (k.rec.contains(screenX, screenY)) {
					k.triggerer = Triggerer.SCREEN;
					k.nextState = false;
					k.pointer = -2;
				}
			}
		}
		
		Input.touches.remove(pointer);
		
		return true;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		if (!Input.isAndroid) {
			pointer = -1;
		}
		TouchPoint tp = Input.touches.get(pointer);
		if (tp != null) {
			tp.pos.set(screenX, screenY);
			if (Input.isAndroid && !Input.isOuya && !Input.isInMenu && tp.touchmode == TouchMode.KeyInput) {
				//System.out.println("X: "+screenX+"; Y: "+screenY+"; P: "+pointer+"; M: D");
				for (Input.Key k : Input.all) {
					if (k.pointer == pointer && !k.rec.contains(screenX, screenY)) {
						k.triggerer = Triggerer.SCREEN;
						k.nextState = false;
						k.pointer = -2;
					}
				}
				for (Input.Key k : Input.all) {
					if (k.rec.contains(screenX, screenY)) {
						k.triggerer = Triggerer.SCREEN;
						k.nextState = true;
						k.pointer = pointer;
					}
				}
			}
		}
		
		return true;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		boolean handled = false;
		if (level != null && level.c != null) {
			//No touch point while moving as no "touch" is occuring.
			Garbage.vec2s.next();
			Garbage.vec2s.get().x = screenX;
			Garbage.vec2s.get().y = screenY;
			level.c.pos.set(level.c.calcPos(Garbage.vec2s.get()));
			level.c.render = true;
			handled = true;
		}
		return handled;
	}

	@Override
	public boolean scrolled(int amount) {
		boolean handled = false;
		if (level != null && level.c != null) {
			level.c.scroll(amount);
			handled = true;
		}
		return handled;
	}
	
	@Override
	public void keyDown(Key key) {
		if (!(level instanceof MenuLevel) &&
				(key == Input.pause ||
						(isAndroid && (key == Input.androidBack || key == Input.androidMenu)))) {
			MenuLevel pause = new PauseLevel();
			pause.bglevel = level;
			level = pause;
		}
		if (key == Input.screenshot) {
			ScreenshotUtil.frameSave();
		}
		if (key == Input.record) {
			record = !record;
			if (record) {
				recordDirName = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
			} else {
				ScreenshotUtil.framesPull();
			}
		}
	}

	@Override
	public void keyUp(Key key) {
	}
	
}
