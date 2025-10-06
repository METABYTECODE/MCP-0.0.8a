package com.mojang.minecraft;

import com.mojang.minecraft.level.Level;
// Using InputManager instead of direct Keyboard access

public class Player extends Entity {
   public Player(Level level) {
      super(level);
      this.heightOffset = 1.62F;
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      float xa = 0.0F;
      float ya = 0.0F;
      if (InputManager.isKeyDown(InputManager.KEY_R)) {
         this.resetPos();
      }

      if (InputManager.isKeyDown(InputManager.KEY_W)) {
         --ya;
      }

      if (InputManager.isKeyDown(InputManager.KEY_S)) {
         ++ya;
      }

      if (InputManager.isKeyDown(InputManager.KEY_A)) {
         --xa;
      }

      if (InputManager.isKeyDown(InputManager.KEY_D)) {
         ++xa;
      }

      if ((InputManager.isKeyDown(InputManager.KEY_SPACE) || InputManager.isKeyDown(InputManager.KEY_LEFT_SHIFT)) && this.onGround) {
         this.yd = 0.5F;
      }

      this.moveRelative(xa, ya, this.onGround ? 0.1F : 0.02F);
      this.yd = (float)((double)this.yd - 0.08D);
      this.move(this.xd, this.yd, this.zd);
      this.xd *= 0.91F;
      this.yd *= 0.98F;
      this.zd *= 0.91F;
      if (this.onGround) {
         this.xd *= 0.7F;
         this.zd *= 0.7F;
      }

   }
}
