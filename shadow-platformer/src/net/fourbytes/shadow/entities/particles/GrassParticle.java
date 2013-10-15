package net.fourbytes.shadow.entities.particles;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import net.fourbytes.shadow.Images;
import net.fourbytes.shadow.Layer;
import net.fourbytes.shadow.Shadow;
import net.fourbytes.shadow.entities.Particle;

public class GrassParticle extends Particle {

	public Color color;

	public GrassParticle(Vector2 position, Layer layer) {
		super(position, layer, 100 + Shadow.rand.nextInt(100));

		float size = 0.0625f + 0.05f*Shadow.rand.nextFloat();
		setSize(size, size);

		color = new Color(0f, 0.4f + 0.5f*Shadow.rand.nextFloat(), 0.225f + 0.2f*Shadow.rand.nextFloat(), 1f);

		light.set(color);
		light.mul(0.4f);
		light.a = 0.0775f;

		solid = false;
		objgravity = Shadow.rand.nextFloat()-0.4f;
	}
	
	@Override
	public TextureRegion getTexture() {
		return Images.getTextureRegion("white");
	}
	
	@Override
	public void tick() {
		super.tick();
		movement.x = (Shadow.rand.nextFloat()-0.5f)*rec.width * 2f;
		movement.y = (Shadow.rand.nextFloat()-0.5f)*rec.height * 0.75f;
	}
	
	@Override
	public void preRender() {
		super.preRender();
		tmpimg.setColor(tmpc.set(color).mul(tmpcc.set(1f, 1f, 1f, time/spawntime)).mul(layer.tint));
	}

}