package com.danil.blackswordsman;

/** Temporary values never mutate saved progression. Timers pause with gameplay. */
public final class PlayerEffects implements java.io.Serializable {
    private static final long serialVersionUID=6L;
    public static final int MEDKIT=0,SPEED=1,POWER=2,ARMOR=3,REGEN=4,AMMO=5,HOUND=6,SERPENT=7,WRAITH=8,BEAST=9;
    public static final String[] NAMES={"Аптечка +80 HP","Леденец скорости · 25 с","Пирожное силы · 30 с","Желе защиты · 30 с","Нектар регенерации · 20 с","Коробка снарядов +2","Печенье гончей · 35 с","Мармелад змея · 35 с","Десерт духа · 30 с","Сердце зверя · 25 с"};
    public float speedTime,powerTime,armorTime,regenTime,formTime;
    public int form=-1;
    public void clear(){speedTime=powerTime=armorTime=regenTime=formTime=0;form=-1;}
    public void tick(float dt,GameWorld.Actor p){
        if(p.dead)return;
        if(regenTime>0)p.hp=Math.min(p.maxHp,p.hp+6f*Math.min(dt,regenTime));
        speedTime=Math.max(0,speedTime-dt);powerTime=Math.max(0,powerTime-dt);armorTime=Math.max(0,armorTime-dt);regenTime=Math.max(0,regenTime-dt);formTime=Math.max(0,formTime-dt);
        if(formTime<=0){form=-1;}
    }
    public void activate(int kind,GameWorld.Actor p){
        if(kind==MEDKIT)p.hp=Math.min(p.maxHp,p.hp+80);
        else if(kind==SPEED)speedTime=25;
        else if(kind==POWER)powerTime=30;
        else if(kind==ARMOR)armorTime=30;
        else if(kind==REGEN)regenTime=20;
        else if(kind>=HOUND&&kind<=BEAST){form=kind;formTime=kind==WRAITH?30:kind==BEAST?25:35;}
    }
    public void apply(GameWorld.Actor p,GameWorld.Progress base){
        p.type=-1;p.name="ГАТС";p.radius=.52f;p.height=2.1f;p.range=2.45f;
        p.colorR=.075f;p.colorG=.075f;p.colorB=.085f;p.metalness=.72f;p.roughness=.34f;
        float speed=1f,damage=1f;
        if(form==HOUND){p.type=GameWorld.FLESH_HOUND;p.name="АСТРАЛЬНАЯ ГОНЧАЯ";p.radius=.60f;p.height=1.05f;speed=1.65f;damage=1.3f;p.colorR=.5f;p.colorG=.08f;p.colorB=.28f;}
        else if(form==SERPENT){p.type=GameWorld.SERPENT_SPAWN;p.name="ЗМЕЁНЫШ";p.radius=.36f;p.height=.78f;p.range=2.8f;speed=1.35f;p.colorR=.16f;p.colorG=.62f;p.colorB=.24f;}
        else if(form==WRAITH){p.type=GameWorld.WRAITH;p.name="АСТРАЛЬНЫЙ ДУХ";p.radius=.38f;p.height=2.2f;speed=1.3f;p.colorR=.13f;p.colorG=.65f;p.colorB=.94f;}
        else if(form==BEAST){p.type=GameWorld.VOID_BEAST;p.name="ЗВЕРЬ БЕЗДНЫ";p.radius=.86f;p.height=1.7f;p.range=3.3f;speed=1.1f;damage=1.9f;p.colorR=.45f;p.colorG=.06f;p.colorB=.64f;}
        if(form>=0){p.metalness=.06f;p.roughness=.53f;}
        p.speed=base.speed*speed*(speedTime>0?1.7f:1f);p.damage=base.attack*damage*(powerTime>0?1.5f:1f);
    }
    public float damageScale(){return (armorTime>0?.55f:1f)*(form==WRAITH?.58f:form==BEAST?.78f:1f);}
    public String status(){String s="";if(speedTime>0)s+="СКОРОСТЬ ×1.7  "+(int)Math.ceil(speedTime)+"с   ";if(powerTime>0)s+="УРОН ×1.5  "+(int)Math.ceil(powerTime)+"с";return s;}
    public String defenseStatus(){String s="";if(armorTime>0)s+="ЗАЩИТА  "+(int)Math.ceil(armorTime)+"с   ";if(regenTime>0)s+="+6 HP/с  "+(int)Math.ceil(regenTime)+"с";return s;}
}
