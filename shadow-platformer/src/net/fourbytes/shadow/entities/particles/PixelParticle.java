package net.fourbytes.shadow.entities.particles;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import net.fourbytes.shadow.Images;
import net.fourbytes.shadow.Layer;
import net.fourbytes.shadow.Shadow;
import net.fourbytes.shadow.entities.Particle;

import java.util.Random;

public class PixelParticle extends Particle {
	
	public Color color;
	
	public PixelParticle(Vector2 position, Layer layer, int time, float size, Color color) {
		super(position, layer, time);
		setSize(size, size);
		this.color = color;
		
		if (time == 0) {
			Random r = new Random();
			while (this.time < 1) {
				this.time += r.nextInt(15)+10;
			}
		}
		spawntime = this.time;
		
		fade = true;
		objgravity = 0.5f*(8f*size);
	}
	
	@Override
	public TextureRegion getTexture(int id) {
		return Images.getTextureRegion("white");
	}
	
	@Override
	public void tick() {
		super.tick();
		if (time == spawntime-1) {
			movement.x = (Shadow.rand.nextFloat()-0.5f)*rec.width * 2f;
			movement.y = (Shadow.rand.nextFloat()-0.5f)*rec.height * 3f;
		}
	}
	
	@Override
	public void preRender() {
		super.preRender();
	}

	@Override
	public void tint(int id, Image img) {
		super.tint(id, img);
		img.setColor(tmpc.set(color).mul(tmpcc.set(1f, 1f, 1f, time / spawntime)).mul(layer.tint));
	}
}
