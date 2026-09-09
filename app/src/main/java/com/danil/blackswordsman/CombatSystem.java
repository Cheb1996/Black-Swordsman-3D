package com.danil.blackswordsman;

/** Swept projectiles and obstacle-aware, local steering shared across enemies. */
public final class CombatSystem {
    private CombatSystem(){}
    public static void projectiles(GameWorld w,float dt){
        for(int i=w.projectiles.size()-1;i>=0;i--){
            GameWorld.Projectile p=w.projectiles.get(i);float ax=p.x,ay=p.y,az=p.z;
            p.life-=dt;p.vy+=PhysicsWorld.GRAVITY*dt*.24f;float bx=ax+p.vx*dt,by=ay+p.vy*dt,bz=az+p.vz*dt;
            float wall=w.physics.raycast(ax,ay,az,bx,by,bz,p.radius,null,false);boolean remove=p.life<=0;
            for(int pass=0;pass<16&&!remove;pass++){
                GameWorld.Actor nearest=null;float time=wall;
                if(p.friendly){for(GameWorld.Actor a:w.enemies){if(a.dead||p.hitIds.contains(Integer.valueOf(a.id)))continue;float t=CollisionMath.segmentActor(ax,ay,az,bx,by,bz,a,p.radius);if(t<time){time=t;nearest=a;}}}
                else if(!w.player.dead&&!p.hitIds.contains(Integer.valueOf(w.player.id))){float t=CollisionMath.segmentActor(ax,ay,az,bx,by,bz,w.player,p.radius);if(t<time){time=t;nearest=w.player;}}
                if(nearest==null)break;p.hitIds.add(Integer.valueOf(nearest.id));
                if(nearest.player){boolean vulnerable=nearest.invulnerable<=0;w.damagePlayer(p.damage,ax,az);if(vulnerable&&p.poison>0)nearest.poisonTime=Math.max(nearest.poisonTime,p.poison);}else w.damageEnemy(nearest,p.damage,p.vx*.35f,p.vz*.35f,.55f);
                p.damage*=.76f;p.pierce--;if(p.pierce<=0){bx=ax+(bx-ax)*time;by=ay+(by-ay)*time;bz=az+(bz-az)*time;remove=true;}
            }
            if(!remove&&wall<=1){bx=ax+(bx-ax)*wall;by=ay+(by-ay)*wall;bz=az+(bz-az)*wall;remove=true;w.physics.impulseSphere(bx,by,bz,1.7f,p.vx*.20f,2.5f,p.vz*.20f,true);}
            p.x=bx;p.y=by;p.z=bz;w.spawnTrail(bx,by,bz,p.r,p.g,p.b,p.radius*.7f);
            if(remove||Math.abs(bx)>GameWorld.OCEAN_HALF+3||Math.abs(bz)>GameWorld.OCEAN_HALF+3){w.fireBurst(bx,by,bz,8);w.projectiles.remove(i);}
        }
    }
    public static boolean visible(GameWorld w,GameWorld.Actor a,GameWorld.Actor b){return w.physics.raycast(a.x,a.y+a.height*.55f,a.z,b.x,b.y+b.height*.55f,b.z,.035f,null,a.type==GameWorld.WRAITH)>1;}
    public static float steer(GameWorld w,GameWorld.Actor a,float desired){
        float y=a.y+Math.min(.55f,a.height*.55f),distance=2.2f;float best=desired,bestScore=-999;
        for(int i=0;i<9;i++){float offset=i==0?0:((i+1)/2)*.42f*(i%2==0?-1:1),angle=desired+offset;
            float x=a.x+(float)Math.sin(angle)*distance,z=a.z+(float)Math.cos(angle)*distance;
            float hit=w.physics.raycast(a.x,y,a.z,x,y,z,a.radius*.65f,null,a.type==GameWorld.WRAITH);
            float score=Math.min(1,hit)*3f-Math.abs(offset)*.35f;if(score>bestScore){bestScore=score;best=angle;}}
        return best;
    }
}
