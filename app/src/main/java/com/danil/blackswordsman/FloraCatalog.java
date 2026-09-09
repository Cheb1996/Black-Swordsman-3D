package com.danil.blackswordsman;
/** Six regional guilds, 24 ordinary fungi, 8 magic fungi and 16 vascular plants. */
public final class FloraCatalog {
 public static final int MUSHROOMS=32,COUNT=48;
 public static final String[] NAMES={"Боровик","Лисичка","Мухомор","Опёнок","Болотный зонтик","Синий млечник","Чёрная воронка","Коралловый гриб","Каменный дождевик","Горный сморчок","Серебряная вешенка","Ледяной бокал","Песочный трюфель","Пустынный моховик","Золотая чашечка","Дюнная звезда","Пепельный трутовик","Кровавый зуб","Фиолетовый рогатик","Лунная сыроежка","Астральная спираль","Гриб фонарь","Призрачный сморчок","Звёздный дождевик","Тихий колокол","Небесное крыло","Магнитная шляпка","Громовой толчок","Хронокоралл","Живой нектар","Лёгкое перо","Глаз чащи","Папоротник","Камыш","Рогоз","Хвощ","Клевер","Чертополох","Вереск","Алоэ","Плющ","Лопух","Лунный цветок","Крапива","Кувшинка берега","Сухоцвет","Светящийся мох","Древовидный суккулент"};
 public static final float[][] PALETTE={{.52f,.27f,.11f},{.16f,.55f,.57f},{.54f,.51f,.68f},{.83f,.57f,.22f},{.42f,.07f,.18f},{.36f,.20f,.77f}};
 public static boolean forest(int env,float x,float z){return RegionLayout.biome(x,z)==0&&(Math.max(Math.abs(x),Math.abs(z))>145||env==0||env==3||env==7);}
 public static int region(int env,float x,float z){if(env==8)return 5;if(WaterField.depth(env,x+5,z)>0||WaterField.depth(env,x,z+5)>0||env==5)return 1;int b=RegionLayout.biome(x,z);if(b==1)return 3;if(b==2)return 2;if(env==2||env==4||env==6)return 4;return 0;}
 public static int select(int env,float x,float z,java.util.Random r){int group=region(env,x,z);return (r.nextFloat()<.82f?group:r.nextInt(6))*4+r.nextInt(4);}
 public static int plant(int region,java.util.Random r){int[][] species={{0,4,6,8,9,11},{1,2,3,12,14},{5,6,13},{7,13,15},{5,8,11,13},{0,10,14}};int[] p=species[region];return 32+p[r.nextInt(p.length)];}
 public static float[] color(int id){if(id>=24&&id<32)return MushroomEffects.COLORS[id-24];float[][] colors={{.46f,.20f,.075f},{.95f,.53f,.055f},{.75f,.055f,.035f},{.67f,.39f,.13f},{.34f,.47f,.30f},{.08f,.34f,.65f},{.13f,.15f,.16f},{.91f,.70f,.39f},{.65f,.62f,.52f},{.58f,.34f,.15f},{.64f,.68f,.72f},{.57f,.75f,.87f},{.36f,.23f,.13f},{.60f,.44f,.16f},{.96f,.69f,.13f},{.80f,.56f,.29f},{.30f,.25f,.27f},{.73f,.08f,.13f},{.52f,.15f,.61f},{.46f,.29f,.72f},{.45f,.12f,.76f},{.15f,.74f,.61f},{.46f,.67f,.80f},{.69f,.41f,.92f}};return colors[Math.min(23,id)];}
 private FloraCatalog(){}
}
