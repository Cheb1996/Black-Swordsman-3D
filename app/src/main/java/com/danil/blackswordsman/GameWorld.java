package com.danil.blackswordsman;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

/** Deterministic gameplay simulation. All mutation happens on the GL thread. */
public final class GameWorld implements SimulationClock.Step, java.io.Serializable {
    private static final long serialVersionUID=6L;
    private transient final SimulationClock clock=new SimulationClock();
    private int lastInputMode=-1;
    public Actor lockedTarget;
    /** Ten times the previous 284 x 284 m map area; ocean exploration extends beyond the island. */
    public static final float WORLD_HALF = 142f*(float)Math.sqrt(10);
    public static final float OCEAN_HALF = WORLD_HALF+160f;
    public static final int MENU = 0;
    public static final int STORY = 1;
    public static final int PLAYING = 2;
    public static final int UPGRADE = 3;
    public static final int PAUSED = 4;
    public static final int GAME_OVER = 5;
    public static final int ENDING = 6;

    public static final int BANDIT = 0;
    public static final int CAPTAIN = 1;
    public static final int GUARD = 2;
    public static final int CROSSBOW = 3;
    public static final int SKELETON = 4;
    public static final int WRAITH = 5;
    public static final int BONE_KNIGHT = 6;
    public static final int ZEALOT = 7;
    public static final int EXECUTIONER = 8;
    public static final int GHOUL = 9;
    public static final int FLESH_HOUND = 10;
    public static final int ABYSS_DEMON = 11;
    public static final int JAILER = 12;
    public static final int SERPENT_SPAWN = 13;
    public static final int SNAKE_BARON = 14;
    public static final int APOSTLE_SPAWN = 15;
    public static final int COUNT_APOSTLE = 16;
    public static final int SHADE = 17;
    public static final int VOID_BEAST = 18;
    public static final int CLOCK_MOTH=19,MIRROR_HARE=20,MAGNET_JELLY=21,DICE_CRAB=22,BALLOON_KEEPER=23,TOY_GOLEM=24;

    public static final int TUSK_BOAR=25,MIRE_SPITTER=26,STORM_SPIRIT=27,GROVE_WARDEN=28,SENTRY=29,WALKER=30,ALIEN=31,SCAN_DRONE=32;
    public static final int FISH=11,TURTLE=12,GULL=13,CAMEL=14,LIZARD=15,OTTER=16;
    public static final int DEER=0,HARE=1,CROW=2,FOX=3,DOG=4,HORSE=5,RAT=6;
    public static final int ELF=7,WISP=8,FOREST_SPIRIT=9,GOAT=10;

    public static final int EVENT_NONE = 0;
    public static final int EVENT_SLASH = 1;
    public static final int EVENT_HIT = 2;
    public static final int EVENT_CANNON = 3;
    public static final int EVENT_DODGE = 4;
    public static final int EVENT_RAGE = 5;
    public static final int EVENT_PAGE = 6;
    public static final int EVENT_BOSS = 7;

    public interface MotionInput {
        float getTiltX();
        float getTiltY();
        float consumeGyroYaw();
        float consumeGyroPitch();
        boolean hasGyroscope();
        boolean hasAccelerometer();
        void calibrate();
    }

    public interface ProgressStore {
        Progress load();
        void save(Progress progress);
        void clear();
    }

    public static final class Progress implements java.io.Serializable {
        private static final long serialVersionUID=6L;
        public int chapter;
        public int unlocked;
        public float maxHp = 240f;
        public float attack = 38f;
        public float armor;
        public float speed = 5.15f;
        public int maxAmmo = 5;
        public boolean completed,pendingUpgrade;
        public int totalKills;

        public Progress copy() {
            Progress p = new Progress();
            p.chapter = chapter; p.unlocked = unlocked; p.maxHp = maxHp;
            p.attack = attack; p.armor = armor; p.speed = speed;
            p.maxAmmo = maxAmmo; p.completed = completed;p.pendingUpgrade=pendingUpgrade; p.totalKills = totalKills;
            return p;
        }
    }

    public static final class Actor implements java.io.Serializable {
        private static final long serialVersionUID=6L;
        private static int nextActorId;
        public int id=++nextActorId;
        public boolean motionReady,previousGrounded,flying;
        public float motionX,motionY,motionZ,motionYaw,previousForward,previousSide,leanForward,leanSide,turnLean,landingTime,collisionTime,collisionX,collisionZ,hitReaction,previousAttackTime,attackDuration,flightY,featherTime;
        public int type;
        public boolean grounded,swimming,hovering;public float vy;
        public int category;public boolean visitor;public float abilityTime,shieldTime,poisonTime,slowTime,markX,markZ,poisonTick;public int abilityState,abilityCount;
        public boolean strikePending;public float strikeAt,strikeDamage,strikeRange,strikeDot,strikeKnock;
        public boolean player;
        public boolean boss;
        public String name;
        public float x, y, z, vx, vz, yaw;
        public float hp, maxHp, damage, speed, range, radius, height;
        public float attackTime, attackCooldown, heavyCooldown, cannonCooldown;
        public float dodgeTime, dodgeCooldown, invulnerable, stun, hitFlash;
        public float rage, rageTime, deathTime, specialCooldown, spawnTime;
        public float gaitPhase,animationSpeed,attackBlend,navCooldown,navDirection;
        public float comboWindow;
        public int combo, attackKind, phase;
        public boolean dead, summonedA, summonedB;
        public boolean ambient;
        public boolean surreal,peaceful;
        public boolean ragdollSpawned;
        public boolean weaponDropped;
        public int severedMask;
        public float colorR, colorG, colorB, metalness, roughness;
        public float homeX,homeZ,wanderTime,wanderYaw;
        public int chunkX,chunkZ;
    }

    /** Peaceful fauna and astral life, articulated procedurally by the renderer. */
    public static final class Neutral implements java.io.Serializable {
        private static final long serialVersionUID=6L;
        public int type,chunkX,chunkZ;
        public String name;
        public boolean magical,companion;public int category;
        public float x,y,z,yaw,speed,phase,gait,moveBlend,bump,wanderTime,homeX,homeZ,scale=1f;
    }

    public static final class Projectile implements java.io.Serializable {
        private static final long serialVersionUID=6L;
        public float x, y, z, vx, vy, vz, radius, damage, life;
        public boolean friendly;
        public float poison;public int pierce; public final java.util.HashSet<Integer> hitIds=new java.util.HashSet<Integer>();
        public float r, g, b;
    }

    public static final class Particle implements java.io.Serializable {
        private static final long serialVersionUID=6L;
        public float x, y, z, vx, vy, vz, size, life, maxLife;
        public float r, g, b;
        public int kind;public boolean floating;
    }

    private transient final GameInput input;
    private transient final MotionInput motion;
    private transient final ProgressStore store;
    private Random random = new Random(0xB3E53L);
    private ArrayList<Integer> spawnQueue = new ArrayList<Integer>();
    private HashSet<Long> populatedChunks = new HashSet<Long>();

    public ArrayList<Actor> enemies = new ArrayList<Actor>();
    public ArrayList<Projectile> projectiles = new ArrayList<Projectile>();
    public ArrayList<Particle> particles = new ArrayList<Particle>();
    public ArrayList<Neutral> wildlife = new ArrayList<Neutral>();
    public Actor player = new Actor();
    public PhysicsWorld physics = new PhysicsWorld();
    public int worldVersion=8;
    public UfoEncounters encounters=new UfoEncounters();
    public SurrealWorld surreal=new SurrealWorld();
    public PlayerEffects effects=new PlayerEffects();
    public MushroomEffects mushrooms=new MushroomEffects(); public FloraWorld flora=new FloraWorld();
    public volatile String hudMushroom0="",hudMushroom1="",hudMushroom2="",hudMushroom3="";
    public QuestSystem quest=new QuestSystem();
    public ExplorationSystem exploration=new ExplorationSystem();
    public transient GameSettings settings=new GameSettings();
    public transient volatile boolean settingsOpen;
    private java.util.HashMap<Long,ArrayList<Actor>> archivedEnemies=new java.util.HashMap<Long,ArrayList<Actor>>();
    private java.util.HashMap<Long,ArrayList<Neutral>> archivedWildlife=new java.util.HashMap<Long,ArrayList<Neutral>>();
    private HashSet<Long> visitedPopulation=new HashSet<Long>();
    public float hudQuestX,hudQuestZ;
    public volatile String hudCreature="",hudPlayerName="ГАТС",hudEffectAttack="",hudEffectDefense="",hudForm="",hudPickup="",hudZone="";
    public volatile int hudDiscovered,hudLoadedToys;
    public volatile float hudPlayerX,hudPlayerZ;

    public volatile int mode = MENU;
    public volatile int chapter;
    public volatile int storyLine;
    public volatile boolean storyOutro;
    public volatile int wave;
    public volatile int waveCount;
    public volatile int enemiesAlive;
    public volatile int ambientEnemies;
    public volatile int neutralCreatures;
    public volatile String biomeLabel="СТАРАЯ ДОРОГА";
    public volatile float hudHealth;
    public volatile float hudMaxHealth;
    public volatile float hudRage;
    public volatile int hudAmmo;
    public volatile int hudMaxAmmo;
    public volatile String objective = "";
    public volatile String banner = "";
    public volatile float bannerTime;
    public volatile String bossName = "";
    public volatile float bossHealth;
    public volatile float bossMaxHealth;
    public volatile int feedbackEvent;
    public volatile int feedbackSerial;
    public volatile boolean gyroEnabled = true;
    /** Disabled on every launch: no sensor can move the player without explicit opt-in. */
    public volatile boolean tiltEnabled;
    public volatile boolean gyroAvailable;
    public volatile boolean accelerometerAvailable;
    public volatile boolean hasSave;
    public volatile float cameraYaw = 0.12f;
    public volatile float cameraPitch = 0.32f;
    public volatile float cameraDistance = 8.4f;
    public volatile float cinematicTime;
    public volatile float damageFlash;
    public volatile float slowMotion;
    public volatile int qualityLevel = 3;
    public volatile String performanceLabel = "QUALITY: ULTRA";
    public volatile String glLabel = "OPENGL ES 3.2";
    public volatile int fps;
    public volatile int hitChain;
    public volatile float hitChainTime;

    private Progress progress;
    private int previousMode = PLAYING;
    private int waveIndex = -1;
    private float spawnClock;
    private float waveDelay;
    private float clearClock;
    private int ammo;
    private int maxAmmo;
    private int populationChunkX=Integer.MIN_VALUE,populationChunkZ=Integer.MIN_VALUE;
    private float magicClock;
    private float checkpointClock;public volatile String checkpointStatus="";

    public GameWorld(GameInput input, MotionInput motion, ProgressStore store) {
        this.input = input;
        this.motion = motion;
        this.store = store;
        Progress loaded = store.load();
        progress = loaded == null ? new Progress() : loaded;
        hasSave = loaded != null;
        chapter = clampInt(progress.chapter, 0, StoryData.CHAPTERS.length - 1);
        gyroAvailable = motion.hasGyroscope();
        accelerometerAvailable = motion.hasAccelerometer();
        resetPlayer();
        publishHud();
    }

    public Progress getProgressCopy() { return progress.copy(); }

    public StoryData.Chapter currentChapter() { return StoryData.CHAPTERS[clampInt(chapter, 0, StoryData.CHAPTERS.length - 1)]; }

    public String[] currentStoryLine() {
        String[][] lines = storyOutro ? currentChapter().outro : currentChapter().intro;
        if (lines.length == 0) return new String[] { "", "" };
        return lines[clampInt(storyLine, 0, lines.length - 1)];
    }

    public void update(float elapsed) {clock.advance(elapsed,this);}
    @Override public void simulate(float rawDt) {
        if(settingsOpen){input.clearMovement();return;}
        float dt = rawDt;
        if (slowMotion > 0f) { slowMotion -= rawDt; dt *= 0.28f; }
        cinematicTime += dt;
        if(hitChainTime>0f){hitChainTime=Math.max(0f,hitChainTime-rawDt);if(hitChainTime<=0f)hitChain=0;}
        bannerTime = Math.max(0f, bannerTime - rawDt);
        damageFlash = Math.max(0f, damageFlash - rawDt * 2.8f);

        if (input.consumeGyroToggle()) {
            gyroEnabled = !gyroEnabled;
            input.gyroEnabled = gyroEnabled;
            banner = gyroEnabled ? "ГИРОСКОП КАМЕРЫ ВКЛЮЧЕН" : "ГИРОСКОП КАМЕРЫ ВЫКЛЮЧЕН";
            bannerTime = 1.5f;
            emit(EVENT_PAGE);
        }
        if (input.consumeTiltToggle()) {
            if (!accelerometerAvailable) {
                tiltEnabled = false; input.tiltEnabled = false;
                banner = "АКСЕЛЕРОМЕТР НЕДОСТУПЕН";
            } else {
                tiltEnabled = !tiltEnabled; input.tiltEnabled = tiltEnabled;
                if (tiltEnabled) motion.calibrate();
                banner = tiltEnabled ? "НАКЛОН ВКЛЮЧЕН • ДЕРЖИТЕ РОВНО" : "НАКЛОН ВЫКЛЮЧЕН";
            }
            bannerTime = 1.7f; emit(EVENT_PAGE);
        }
        if (input.consumeCalibration()) {
            motion.calibrate();
            banner = "ПОЛОЖЕНИЕ ОТКАЛИБРОВАНО";
            bannerTime = 1.4f;
        }

        float touchYaw = input.consumeCameraX() * 0.0053f*settings.sensitivity;
        float touchPitch = input.consumeCameraY() * 0.0044f*settings.sensitivity;
        cameraYaw -= touchYaw;
        cameraPitch = clamp(cameraPitch + touchPitch, -0.02f, 0.78f);
        if (gyroEnabled) {
            cameraYaw += motion.consumeGyroYaw() * 1.18f;
            cameraPitch = clamp(cameraPitch + motion.consumeGyroPitch() * 0.82f, -0.02f, 0.78f);
        } else {
            motion.consumeGyroYaw(); motion.consumeGyroPitch();
        }

        if (input.consumePause()) {
            if (mode == PLAYING) { previousMode = mode; mode = PAUSED; input.clearMovement(); }
            else if (mode == PAUSED) mode = previousMode;
        }

        if (mode == MENU) updateMenu();
        else if (mode == STORY) updateStory();
        else if (mode == PLAYING) updatePlaying(dt);
        else if (mode == UPGRADE) updateUpgrade();
        else if (mode == PAUSED) updatePaused();
        else if (mode == GAME_OVER) updateGameOver();
        else if (mode == ENDING && input.consumeAction()) mode = MENU;
        if(mode!=lastInputMode){input.resetAll();lastInputMode=mode;if(mode!=MENU)saveCheckpoint();}if(mode==PLAYING){checkpointClock+=rawDt;if(checkpointClock>=20){checkpointClock=0;saveCheckpoint();}}publishHud();
    }

    private void updateMenu() {
        if (input.consumeAction()) startCampaign(hasSave ? progress.chapter : 0, false);
        if (input.consumeMenu()) startCampaign(0, true);
    }

    private void startCampaign(int startChapter, boolean fresh) {
        if(!fresh&&store instanceof SnapshotCodec.Store&&SnapshotCodec.restore(this,((SnapshotCodec.Store)store).loadCheckpoint())){if(mode==PAUSED)mode=PLAYING;input.resetAll();banner="ПРОХОЖДЕНИЕ ВОССТАНОВЛЕНО";bannerTime=2;return;}
        if (fresh) {
            if(store instanceof SnapshotCodec.Store)((SnapshotCodec.Store)store).clearCheckpoint();store.clear(); progress = new Progress(); hasSave = false;
        }
        chapter = clampInt(startChapter, 0, StoryData.CHAPTERS.length - 1);
        progress.chapter = chapter;
        if(progress.pendingUpgrade){mode=UPGRADE;}else enterStory(false);
        save();
    }

    private void enterStory(boolean outro) {
        effects.clear();mushrooms.clear(player);effects.apply(player,progress);
        mode = STORY; storyOutro = outro; storyLine = 0; cinematicTime = 0f;
        input.clearMovement(); enemies.clear(); projectiles.clear(); particles.clear(); wildlife.clear(); populatedChunks.clear();
        banner = "ГЛАВА " + currentChapter().roman + " • " + currentChapter().title;
        bannerTime = 2.5f; objective = currentChapter().objective; emit(EVENT_PAGE);
    }

    private void updateStory() {
        if(input.consumeMenu()){save();mode=MENU;return;}
        if (!input.consumeAction()) return;
        String[][] lines = storyOutro ? currentChapter().outro : currentChapter().intro;
        if (storyLine + 1 < lines.length) {
            storyLine++; cinematicTime = 0f; emit(EVENT_PAGE); return;
        }
        if (!storyOutro) startCombat();
        else if (chapter == StoryData.CHAPTERS.length - 1) {
            progress.completed = true; progress.unlocked = StoryData.CHAPTERS.length - 1;
            save(); mode = ENDING; emit(EVENT_PAGE);
        } else mode = UPGRADE;
    }

    private void updateUpgrade() {
        if(input.consumeMenu()){save();mode=MENU;return;}
        int choice = input.consumeUpgradeChoice();
        if (choice < 0) return;
        if (choice == 0) progress.attack += 7f;
        else if (choice == 1) { progress.maxHp += 45f; progress.armor = Math.min(0.38f, progress.armor + 0.03f); }
        else { progress.maxAmmo += 1; progress.speed += 0.18f; }
        progress.pendingUpgrade=false;chapter++;
        progress.chapter = chapter;
        progress.unlocked = Math.max(progress.unlocked, chapter);
        save(); enterStory(false);
    }

    private void updatePaused() {
        if (input.consumeAction()) mode = previousMode;
        if (input.consumeMenu()) { save(); mode = MENU; }
    }

    private void updateGameOver() {
        if (input.consumeAction()) startCombat();
        if (input.consumeMenu()) mode = MENU;
    }

    private void resetPlayer() {
        effects.clear();mushrooms.clear(player);
        player.type = -1; player.player = true; player.boss = false; player.name = "ГАТС";
        player.motionReady=false;player.flying=false;player.landingTime=player.hitReaction=player.collisionTime=0;player.vy=0;player.grounded=true;player.strikePending=false;lockedTarget=null;player.x = 0f; player.y = physics.terrainHeight(0,5); player.z = 5f; player.vx = 0f; player.vz = 0f; player.yaw = 0f;
        player.maxHp = progress.maxHp; player.hp = player.maxHp;
        player.damage = progress.attack; player.speed = progress.speed; player.range = 2.45f;
        player.radius = 0.52f; player.height = 2.1f; player.rage = 0f; player.rageTime = 0f;
        player.combo = 0; player.attackKind = 0; player.attackTime = 0f; player.attackCooldown = 0f;
        player.heavyCooldown = 0f; player.cannonCooldown = 0f; player.dodgeTime = 0f; player.dodgeCooldown = 0f;
        player.comboWindow = 0f;
        player.invulnerable = 0f; player.stun = 0f; player.dead = false; player.deathTime = 0f;
        player.ragdollSpawned=false;player.weaponDropped=false;player.severedMask=0;
        player.colorR = .075f; player.colorG = .075f; player.colorB = .085f;
        player.metalness = .72f; player.roughness = .34f;
        ammo = progress.maxAmmo; maxAmmo = progress.maxAmmo;
    }

    private void startCombat() {
        mode = PLAYING; input.clearMovement(); resetPlayer(); enemies.clear(); projectiles.clear(); particles.clear(); wildlife.clear();populatedChunks.clear();populationChunkX=populationChunkZ=Integer.MIN_VALUE;magicClock=0f;
        spawnQueue.clear(); waveIndex = -1; wave = 0; waveCount = currentChapter().waves.length;
        waveDelay = 1.0f; spawnClock = 0f; clearClock = 0f; cinematicTime = 0f;
        banner = currentChapter().place.toUpperCase(); bannerTime = 2.2f;
        physics.resetForChapter(chapter,currentChapter().environment);player.y=physics.terrainHeight(player.x,player.z);
        flora.reset();surreal.reset(physics,chapter);encounters.reset(this);quest.reset(this);exploration.reset(this);archivedEnemies.clear();archivedWildlife.clear();visitedPopulation.clear();
        if(chapter>0)spawnPuck();updatePopulation();
        objective = physics.hasPuzzle()?physics.puzzleObjective():currentChapter().objective;
    }

    private void queueWave(int index) {
        waveIndex = index; wave = index + 1; spawnQueue.clear();
        int[] spec = currentChapter().waves[index];
        for (int i = 0; i + 1 < spec.length; i += 2) {
            for (int count = 0; count < spec[i + 1]; count++) spawnQueue.add(Integer.valueOf(spec[i]));
        }
        spawnClock = .12f; clearClock = 0f;
        banner = index + 1 == waveCount ? "ПОСЛЕДНЯЯ ВОЛНА" : "ВОЛНА " + (index + 1);
        bannerTime = 1.45f; emit(EVENT_PAGE);
    }

    private void updatePlaying(float dt) {
        if(!player.dead){int previousForm=effects.form;effects.tick(dt,player);effects.apply(player,progress);if(previousForm>=0&&effects.form<0){physics.recoverForm(player);banner="ГАТС ВЕРНУЛСЯ";bannerTime=1.5f;astralBurst(player.x,player.y+1f,player.z,.45f,.70f,1f,16);}}
        if (waveDelay > 0f&&quest.battleReady(this)) {
            waveDelay -= dt;
            if (waveDelay <= 0f) queueWave(0);
        }
        mushrooms.update(this,dt);updatePlayer(dt);flora.update(this,dt);quest.update(this,dt);exploration.update(this,dt);hudQuestX=quest.x();hudQuestZ=quest.z();
        updatePopulation();
        updateWildlife(dt);
        if(!player.dead){surreal.update(this,dt);encounters.update(this,dt);}
        if(input.consumeInteract()){
            if(!player.dead&&!quest.interact(this)&&!exploration.interact(this)&&!flora.interact(this)&&!surreal.interact(this)){int result=physics.interact(player);PhysicsWorld.Body held=physics.getHeld();banner=result==1?(held!=null&&held.catalogId>=0?SurrealCatalog.ITEMS[held.catalogId].name:"ПРЕДМЕТ ЗАХВАЧЕН"):result==2?"БРОСОК":"ТОЛЧОК";bannerTime=1.1f;emit(EVENT_DODGE);}
        }
        if (!spawnQueue.isEmpty()) {
            spawnClock -= dt;
            if (spawnClock <= 0f) {
                spawnEnemy(spawnQueue.remove(0).intValue());
                spawnClock = .42f + random.nextFloat() * .30f;
            }
        }
        for (int i = 0; i < enemies.size(); i++) updateEnemy(enemies.get(i), dt);
        boolean puzzleBefore=physics.isPuzzleSolved();
        physics.update(dt,player,enemies);AnimationSystem.update(player,dt);for(Actor actor:enemies)if(!actor.dead)AnimationSystem.update(actor,dt);
        for(int i=0;i<physics.impacts.size();i++){PhysicsWorld.Impact hit=physics.impacts.get(i);if(hit.actor.dead)continue;if(hit.actor.player)damagePlayer(hit.damage,hit.fromX,hit.fromZ);else damageEnemy(hit.actor,hit.damage,hit.impulseX,hit.impulseZ,.32f);}
        if(!puzzleBefore&&physics.isPuzzleSolved()){objective=currentChapter().objective;banner="МЕХАНИЗМ ОТКРЫТ";bannerTime=1.8f;waveDelay=.65f;emit(EVENT_PAGE);}
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Actor e = enemies.get(i);
            if (e.dead && e.deathTime <= 0f) enemies.remove(i);
        }
        updateProjectiles(dt);
        updateParticles(dt);

        boolean living = false;
        for (int i = 0; i < enemies.size(); i++) if (!enemies.get(i).dead&&!enemies.get(i).ambient) { living = true; break; }
        if (!player.dead && waveIndex >= 0 && !living && spawnQueue.isEmpty()) {
            clearClock += dt;
            if (waveIndex + 1 < waveCount && clearClock > 1.35f) queueWave(waveIndex + 1);
            else if (waveIndex + 1 >= waveCount && clearClock > 2.05f) {
                progress.unlocked = Math.max(progress.unlocked, chapter);
                quest.combatComplete=true;
            }
        } else clearClock = 0f;
        if(!player.dead&&quest.complete){progress.chapter=chapter;progress.pendingUpgrade=chapter<StoryData.CHAPTERS.length-1;enterStory(true);save();}
    }

    /** Pickups are proximity+intent interactions. Full health/ammo does not consume loot. */
    public boolean collectPickup(int kind){
        if(player.dead||mode!=PLAYING||kind<0||kind>=PlayerEffects.NAMES.length)return false;
        if(kind==PlayerEffects.MEDKIT&&player.hp>=player.maxHp)return false;
        if(kind==PlayerEffects.AMMO){if(ammo>=maxAmmo)return false;ammo=Math.min(maxAmmo,ammo+2);}
        else effects.activate(kind,player);
        effects.apply(player,progress);banner=PlayerEffects.NAMES[kind];bannerTime=2.3f;
        astralBurst(player.x,player.y+1f,player.z,.30f,.90f,.64f,18);emit(EVENT_PAGE);return true;
    }
    public void teleportTo(float x,float z){
        player.x=clamp(x,-WORLD_HALF+3,WORLD_HALF-3);player.z=clamp(z,-WORLD_HALF+3,WORLD_HALF-3);player.y=physics.terrainHeight(player.x,player.z);player.vx=player.vz=0;player.dodgeTime=0;input.clearMovement();player.invulnerable=Math.max(player.invulnerable,1f);banner="СКВОЗЬ СЛОМАННОЕ ПРОСТРАНСТВО";bannerTime=2;astralBurst(player.x,player.y+1f,player.z,.72f,.30f,1f,26);
    }

    public void spawnOddity(int kind,float x,float z){
        Actor e=new Actor();configureEnemy(e,CLOCK_MOTH+kind);e.surreal=true;e.ambient=true;e.peaceful=kind==0||kind==2||kind==4;e.x=e.homeX=x;e.z=e.homeZ=z;e.y=physics.terrainHeight(x,z);e.specialCooldown=1.8f;e.spawnTime=.5f;enemies.add(e);
    }

    private void updateOddity(Actor e,float dt){
        float dx=player.x-e.x,dz=player.z-e.z,d=length(dx,dz);if(d>44)return;
        e.specialCooldown-=dt;e.yaw=lerpAngle(e.yaw,(float)Math.atan2(dx,dz),dt*3);
        if(e.stun>0){e.x+=e.vx*dt;e.z+=e.vz*dt;e.vx*=.85f;e.vz*=.85f;constrain(e);return;}
        if(e.peaceful){float a=cinematicTime*.25f+e.type;e.x+=(e.homeX+(float)Math.sin(a)*2-e.x)*dt*.65f;e.z+=(e.homeZ+(float)Math.cos(a)*2-e.z)*dt*.65f;e.vx=e.vz=0;}
        else if(d>2.4f){e.vx=dx/Math.max(1,d)*e.speed;e.vz=dz/Math.max(1,d)*e.speed;e.x+=e.vx*dt;e.z+=e.vz*dt;}
        else if(e.attackCooldown<=0){e.attackTime=.5f;e.attackCooldown=1.3f;damagePlayer(e.damage,e.x,e.z);}
        if(e.type==MAGNET_JELLY)physics.levitate(e.x,e.z,3.8f,physics.terrainHeight(e.x,e.z)+2.5f,dt);
        if(e.specialCooldown<=0){
            e.specialCooldown=4.6f;astralBurst(e.x,e.y+1f,e.z,e.colorR,e.colorG,e.colorB,14);
            if(e.type==CLOCK_MOTH){physics.freezeNear(e.x,e.z,7f,1.65f);for(int i=0;i<enemies.size();i++){Actor a=enemies.get(i);if(a!=e&&!a.dead&&!a.peaceful&&length(a.x-e.x,a.z-e.z)<7f)a.stun=Math.max(a.stun,1.65f);}}
            else if(e.type==MIRROR_HARE){float angle=random.nextFloat()*6.283185f;e.x=clamp(player.x+(float)Math.sin(angle)*5.2f,-WORLD_HALF+2,WORLD_HALF-2);e.z=clamp(player.z+(float)Math.cos(angle)*5.2f,-WORLD_HALF+2,WORLD_HALF-2);e.y=physics.terrainHeight(e.x,e.z);hostileProjectile(e,7f,10f,0);}
            else if(e.type==DICE_CRAB){physics.impulseSphere(e.x,e.y+.5f,e.z,5.5f,dx/Math.max(1,d)*6f,5.0f,dz/Math.max(1,d)*6f,true);if(d<3.5f)damagePlayer(15,e.x,e.z);}
            else if(e.type==BALLOON_KEEPER&&d<5.0f){effects.speedTime=Math.max(effects.speedTime,6f);banner="ХРАНИТЕЛЬ ШАРИКОВ: УСКОРЕНИЕ";bannerTime=1.4f;}
            else if(e.type==TOY_GOLEM){PhysicsWorld.Body b=physics.createToy(SurrealCatalog.ITEMS[48+random.nextInt(24)],e.x,e.z);b.y=e.y+2;b.vx=dx/Math.max(1,d)*9;b.vz=dz/Math.max(1,d)*9;b.vy=5;b.life=14;b.mass=2.2f;b.invMass=1f/b.mass;}
        }
        constrain(e);
    }

    void astralBurst(float x,float y,float z,float r,float g,float b,int count){for(int i=0;i<count;i++){Particle p=makeParticle(x,y,z,r,g,b,.04f+random.nextFloat()*.055f,.6f+random.nextFloat()*.4f,5);p.vx=(random.nextFloat()-.5f)*4f;p.vy=(random.nextFloat()-.2f)*3f;p.vz=(random.nextFloat()-.5f)*4f;particles.add(p);}}

    private void updatePlayer(float dt) {
        tickActorTimers(player, dt);
        if(player.poisonTime>0){player.poisonTick-=dt;if(player.poisonTick<=0){player.poisonTick=1;damagePlayer(3,player.x,player.z);}}
        if (player.dead) { player.deathTime -= dt; if (player.deathTime <= 0f) mode = GAME_OVER; return; }

        if(player.strikePending&&player.attackTime<=player.strikeAt){player.strikePending=false;meleeHit(player.strikeRange,player.strikeDot,player.strikeDamage,player.strikeKnock);}
        if(input.consumeLock()){if(lockedTarget!=null)lockedTarget=null;else lockedTarget=nearestEnemy(18);}
        if(lockedTarget!=null&&(lockedTarget.dead||distance(player,lockedTarget)>24))lockedTarget=null;
        if(input.consumeJump()){if(player.flying)player.flightY=Math.min(player.flightY+2.5f,physics.terrainHeight(player.x,player.z)+14);else if(player.grounded||player.swimming){player.vy=mushrooms.active(MushroomEffects.FEATHER)?9f:5.6f;player.grounded=false;}}
        if(input.consumePlace())physics.placeHeld(player,true);if(input.consumeRotate())physics.rotateHeld();
        if (input.consumeLight()) lightAttack();
        if (input.consumeHeavy()) heavyAttack();
        if (input.consumeDodge()) dodge();
        if (input.consumeCannon()) cannon();
        if (input.consumeRage()) rage();

        float moveX = input.moveX;
        float moveY = input.moveY;
        if (tiltEnabled) {
            moveX = clamp(moveX + motion.getTiltX() * .72f, -1f, 1f);
            moveY = clamp(moveY + motion.getTiltY() * .58f, -1f, 1f);
        }
        float magnitude = length(moveX, moveY);
        if (magnitude > 1f) { moveX /= magnitude; moveY /= magnitude; magnitude = 1f; }

        if (player.dodgeTime > 0f) {
            player.x += player.vx * dt; player.z += player.vz * dt;
            spawnTrail(player.x, .8f, player.z, .08f, .08f, .09f, .28f);
        } else if (player.stun <= 0f) {
            float forwardX = (float)Math.sin(cameraYaw);
            float forwardZ = (float)Math.cos(cameraYaw);
            float rightX = forwardZ;
            float rightZ = -forwardX;
            float desiredX = rightX * moveX + forwardX * -moveY;
            float desiredZ = rightZ * moveX + forwardZ * -moveY;
            float speed = player.speed * (player.slowTime>0?.48f:1f) * (player.swimming?.66f:1f) * (player.rageTime > 0f ? 1.25f : 1f);
            if (player.attackTime > 0f) speed *= .36f;
            float response = Math.min(1f, dt * 11f);
            player.vx += (desiredX * speed - player.vx) * response;
            player.vz += (desiredZ * speed - player.vz) * response;
            if (magnitude < .035f && Math.abs(player.vx) + Math.abs(player.vz) < .045f) player.vx = player.vz = 0f;
            player.x += player.vx * dt; player.z += player.vz * dt;
            if (magnitude > .12f) player.yaw = lerpAngle(player.yaw, (float)Math.atan2(desiredX, desiredZ), Math.min(1f, dt * 13f));
        }
        if(player.stun>0){player.x+=player.vx*dt;player.z+=player.vz*dt;player.vx*=SimulationClock.damping(8,dt);player.vz*=SimulationClock.damping(8,dt);}
        constrain(player);
    }

    private void lightAttack() {
        if (player.dead || player.attackCooldown > 0f || player.dodgeTime > 0f) return;
        player.combo = player.comboWindow > 0f ? (player.combo % 4) + 1 : 1;
        player.comboWindow = .72f;
        Actor target = lockedTarget!=null?lockedTarget:nearestEnemy(4.2f);
        if (target != null) player.yaw = (float)Math.atan2(target.x - player.x, target.z - player.z);
        player.attackKind = player.combo;
        player.attackTime = player.combo == 4 ? .52f : .31f;
        player.attackCooldown = player.combo == 4 ? .34f : .19f;
        float multiplier = player.combo == 2 ? 1.12f : player.combo == 3 ? 1.25f : player.combo == 4 ? 1.8f : 1f;
        if (player.rageTime > 0f) multiplier *= 1.52f;
        scheduleStrike(player.range+(player.combo==4?.8f:0),player.combo==4?.18f:.42f,player.damage*multiplier,player.combo==4?8.5f:5.2f,player.combo==4?.18f:.10f);
        emit(EVENT_SLASH);
    }

    private void heavyAttack() {
        if (player.dead || player.heavyCooldown > 0f || player.dodgeTime > 0f || player.attackTime > 0f) return;
        Actor target = lockedTarget!=null?lockedTarget:nearestEnemy(5f);
        if (target != null) player.yaw = (float)Math.atan2(target.x - player.x, target.z - player.z);
        player.attackKind = 5; player.attackTime = .82f; player.attackCooldown = .58f; player.heavyCooldown = 1.4f;
        scheduleStrike(effects.form==PlayerEffects.BEAST?4.8f:3.45f,.05f,player.damage*(player.rageTime>0?3.2f:2.25f),12f,.32f);
        cameraDistance = 8.9f; emit(EVENT_HIT);
    }

    private void scheduleStrike(float range,float dot,float damage,float knock,float windup){mushrooms.attack();player.strikePending=true;player.strikeAt=player.attackTime-windup;player.strikeRange=range;player.strikeDot=dot;player.strikeDamage=damage;player.strikeKnock=knock;}
    private void meleeHit(float range, float minDot, float damage, float knock) {
        float fx = (float)Math.sin(player.yaw), fz = (float)Math.cos(player.yaw);
        boolean hit = false;
        for (int i = 0; i < enemies.size(); i++) {
            Actor e = enemies.get(i); if (e.dead) continue;
            float dx = e.x - player.x, dz = e.z - player.z, d = length(dx, dz);
            if (Math.abs((e.y+e.height*.5f)-(player.y+player.height*.5f))<(e.height+player.height)*.5f&&CombatSystem.visible(this,player,e)&&d < range + e.radius && (dx * fx + dz * fz) / Math.max(.01f, d) > minDot) {
                damageEnemy(e, damage, fx * knock, fz * knock, .26f); hit = true;
            }
        }
        slashBurst(player.x + fx * 1.2f, player.y+1.15f, player.z + fz * 1.2f, hit ? 16 : 9);
        physics.impulseSphere(player.x+fx*1.4f,player.y+.65f,player.z+fz*1.4f,range,fx*knock,3.2f,fz*knock,true);
        if (hit) emit(EVENT_HIT);
    }

    private void dodge() {
        if(player.flying){player.flightY=Math.max(physics.terrainHeight(player.x,player.z)+.1f,player.flightY-2.5f);return;}
        if (player.dead || player.dodgeCooldown > 0f || player.attackTime > .25f) return;
        float mx = input.moveX, my = input.moveY;
        float forwardX = (float)Math.sin(cameraYaw), forwardZ = (float)Math.cos(cameraYaw);
        float dx = forwardZ * mx + forwardX * -my;
        float dz = -forwardX * mx + forwardZ * -my;
        if (length(dx, dz) < .12f) { dx = -(float)Math.sin(player.yaw); dz = -(float)Math.cos(player.yaw); }
        float len = Math.max(.001f, length(dx, dz));
        player.vx = dx / len * 13.5f; player.vz = dz / len * 13.5f;
        player.dodgeTime = .34f; player.dodgeCooldown = .82f; player.invulnerable = Math.max(player.invulnerable,.48f);
        dustBurst(player.x, .1f, player.z, 12); emit(EVENT_DODGE);
    }

    private void cannon() {
        boolean astral=effects.form>=0;
        if (player.dead || (!astral&&ammo <= 0) || player.cannonCooldown > 0f || player.dodgeTime > 0f) return;
        mushrooms.attack();Actor target = lockedTarget!=null?lockedTarget:nearestEnemy(13f);
        if (target != null) player.yaw = (float)Math.atan2(target.x - player.x, target.z - player.z);
        float fx = (float)Math.sin(player.yaw), fz = (float)Math.cos(player.yaw);
        Projectile p = new Projectile();
        p.x = player.x + fx * .8f; p.y = player.y+Math.max(.75f,player.height*.65f); p.z = player.z + fz * .8f;
        p.vx = fx * 23f; p.vy = target==null?0:(target.y+target.height*.50f-p.y)/Math.max(.2f,distance(player,target)/23f); p.vz = fz * 23f; p.radius = .24f;
        p.damage = player.damage * 3.65f; p.life = 1.6f; p.friendly = true; p.pierce = 4;
        p.r = astral?.25f:1f; p.g = astral?.80f:.43f; p.b = astral?1f:.12f; projectiles.add(p);
        if(!astral)ammo--;else p.damage*=.55f; player.cannonCooldown = astral?2.3f:1.05f; player.attackKind = 6; player.attackTime = .48f;
        player.vx -= fx * 3.5f; player.vz -= fz * 3.5f; damageFlash = .18f;
        fireBurst(p.x, p.y, p.z, 26); emit(EVENT_CANNON);
    }

    private void rage() {
        if (player.dead || player.rage < 100f) return;
        mushrooms.attack();player.rage = 0f; player.rageTime = 7f; player.invulnerable = Math.max(player.invulnerable,.65f);
        for (int i = 0; i < enemies.size(); i++) {
            Actor e = enemies.get(i); float d = distance(player, e);
            if (!e.dead && d < 4.2f&&CombatSystem.visible(this,player,e)) {
                float dx = (e.x - player.x) / Math.max(.1f, d), dz = (e.z - player.z) / Math.max(.1f, d);
                damageEnemy(e, player.damage * 1.7f, dx * 12f, dz * 12f, .8f);
            }
        }
        bloodBurst(player.x, 1.2f, player.z, 40); damageFlash = .75f; slowMotion = .22f; emit(EVENT_RAGE);
        physics.impulseSphere(player.x,player.y+1f,player.z,5.2f,0,8f,0,true);
    }

    private Actor nearestEnemy(float maxDistance) {
        Actor best = null; float bestDistance = maxDistance;
        for (int i = 0; i < enemies.size(); i++) {
            Actor e = enemies.get(i); if (e.dead||e.peaceful) continue;
            float d = distance(player, e); if (d < bestDistance) { best = e; bestDistance = d; }
        }
        return best;
    }

    private void spawnEnemy(int type) {
        Actor e = new Actor(); e.type = type; e.player = false; configureEnemy(e, type);
        float angle = random.nextFloat() * 6.28318f;
        float distance = e.boss ? 10f : 9.5f + random.nextFloat() * 4f;
        e.x = clamp(player.x + (float)Math.sin(angle) * distance, -WORLD_HALF+3f, WORLD_HALF-3f);
        e.z = clamp(player.z + (float)Math.cos(angle) * distance, -WORLD_HALF+3f, WORLD_HALF-3f);
        e.yaw = (float)Math.atan2(player.x - e.x, player.z - e.z);
        for(int attempt=0;attempt<20;attempt++){e.y=physics.terrainHeight(e.x,e.z);if(physics.actorClear(e,e.x,e.y,e.z))break;float angle2=angle+attempt*.65f;e.x=player.x+(float)Math.sin(angle2)*distance;e.z=player.z+(float)Math.cos(angle2)*distance;}
        e.spawnTime = .7f; e.specialCooldown = 1.5f + random.nextFloat() * 2f;
        enemies.add(e); dustBurst(e.x, 0f, e.z, e.boss ? 28 : 8);
        if (e.boss) { banner = e.name; bannerTime = 2.0f; emit(EVENT_BOSS); }
    }

    public Actor spawnCreature(int type,float x,float z){Actor a=new Actor();configureEnemy(a,type);a.ambient=true;a.boss=false;a.x=a.homeX=x;a.z=a.homeZ=z;a.y=physics.terrainHeight(x,z);a.chunkX=floorChunk(x);a.chunkZ=floorChunk(z);a.specialCooldown=2.5f;enemies.add(a);return a;}
    public void spawnNeutral(int type,float x,float z,int cx,int cz){Neutral n=new Neutral();configureNeutral(n,type);n.x=n.homeX=x;n.z=n.homeZ=z;n.y=physics.terrainHeight(x,z);n.chunkX=cx;n.chunkZ=cz;wildlife.add(n);}
    public void spawnPuck(){
        Neutral n=new Neutral();n.type=ELF;n.name="ПАК";n.magical=true;n.category=CreatureSystem.MAGICAL;n.companion=true;n.scale=.72f;n.x=player.x-1.4f;n.z=player.z+.8f;n.y=physics.terrainHeight(n.x,n.z)+1.45f;n.phase=.7f;wildlife.add(n);
    }

    /** Streams lore-appropriate ambient life independently from authored combat waves. */
    private void updatePopulation(){
        int cx=floorChunk(player.x),cz=floorChunk(player.z);biomeLabel=biomeName(currentChapter().environment,player.x,player.z);
        if(cx==populationChunkX&&cz==populationChunkZ)return;populationChunkX=cx;populationChunkZ=cz;populatedChunks.clear();
        for(int i=enemies.size()-1;i>=0;i--){Actor e=enemies.get(i);if(!e.ambient||e.surreal||e.visitor)continue;if(Math.abs(e.chunkX-cx)>1||Math.abs(e.chunkZ-cz)>1){long k=chunkKey(e.chunkX,e.chunkZ);ArrayList<Actor> list=archivedEnemies.get(k);if(list==null){list=new ArrayList<Actor>();archivedEnemies.put(k,list);}list.add(e);enemies.remove(i);}else populatedChunks.add(chunkKey(e.chunkX,e.chunkZ));}
        for(int i=wildlife.size()-1;i>=0;i--){Neutral n=wildlife.get(i);if(n.companion)continue;if(Math.abs(n.chunkX-cx)>1||Math.abs(n.chunkZ-cz)>1){long k=chunkKey(n.chunkX,n.chunkZ);ArrayList<Neutral> list=archivedWildlife.get(k);if(list==null){list=new ArrayList<Neutral>();archivedWildlife.put(k,list);}list.add(n);wildlife.remove(i);}else populatedChunks.add(chunkKey(n.chunkX,n.chunkZ));}
        for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){int x=cx+dx,z=cz+dz;long key=chunkKey(x,z);if(!populatedChunks.contains(key)){if(visitedPopulation.add(key))populateChunk(x,z);else{ArrayList<Actor> ea=archivedEnemies.remove(key);if(ea!=null)enemies.addAll(ea);ArrayList<Neutral> na=archivedWildlife.remove(key);if(na!=null)wildlife.addAll(na);}populatedChunks.add(key);}}
    }

    private void populateChunk(int cx,int cz){
        int env=currentChapter().environment;Random rng=new Random(0x4c4f5245L+env*0x9e3779b9L+cx*83492791L+cz*2971215073L);
        if(CreatureSystem.populate(this,cx,cz,rng))return;
        int hostileCount=(env==8?1:0)+rng.nextInt(3);int peacefulCount=2+rng.nextInt(env==0||env==3?4:3);
        for(int i=0;i<hostileCount;i++){
            float[] pos=populationPosition(rng,cx,cz);if(pos==null)continue;Actor e=new Actor();configureEnemy(e,ambientType(env,rng));e.ambient=true;e.boss=false;e.x=e.homeX=pos[0];e.z=e.homeZ=pos[1];e.y=physics.terrainHeight(e.x,e.z);e.chunkX=cx;e.chunkZ=cz;e.wanderYaw=rng.nextFloat()*6.28318f;e.wanderTime=.4f+rng.nextFloat()*2.8f;e.spawnTime=0f;enemies.add(e);
        }
        for(int i=0;i<peacefulCount;i++){
            float[] pos=populationPosition(rng,cx,cz);if(pos==null)continue;Neutral n=new Neutral();configureNeutral(n,neutralType(env,rng));n.x=n.homeX=pos[0];n.z=n.homeZ=pos[1];n.y=physics.terrainHeight(n.x,n.z);n.chunkX=cx;n.chunkZ=cz;n.yaw=rng.nextFloat()*6.28318f;n.wanderTime=.5f+rng.nextFloat()*3f;n.phase=rng.nextFloat()*6.28318f;n.scale=.82f+rng.nextFloat()*.34f;wildlife.add(n);
        }
    }

    private float[] populationPosition(Random rng,int cx,int cz){
        for(int attempt=0;attempt<5;attempt++){float x=(cx+rng.nextFloat())*PhysicsWorld.CHUNK_SIZE,z=(cz+rng.nextFloat())*PhysicsWorld.CHUNK_SIZE;if(Math.abs(x)>WORLD_HALF-3f||Math.abs(z)>WORLD_HALF-3f)continue;float dx=x-player.x,dz=z-player.z;if(dx*dx+dz*dz>64f)return new float[]{x,z};}return null;
    }

    private int ambientType(int env,Random rng){
        int roll=rng.nextInt(100);
        if(env==0)return roll<68?BANDIT:roll<88?WRAITH:CAPTAIN;
        if(env==1)return roll<58?GUARD:roll<82?CROSSBOW:BANDIT;
        if(env==2)return roll<58?SERPENT_SPAWN:roll<84?GUARD:CROSSBOW;
        if(env==3)return roll<52?SKELETON:roll<88?WRAITH:BONE_KNIGHT;
        if(env==4)return roll<48?ZEALOT:roll<80?GUARD:roll<94?CROSSBOW:EXECUTIONER;
        if(env==5)return roll<55?GHOUL:roll<87?FLESH_HOUND:WRAITH;
        if(env==6)return roll<47?JAILER:roll<78?GUARD:roll<92?ZEALOT:GHOUL;
        if(env==7)return roll<48?APOSTLE_SPAWN:roll<78?GHOUL:FLESH_HOUND;
        return roll<57?SHADE:roll<91?WRAITH:VOID_BEAST;
    }

    private int neutralType(int env,Random rng){
        int roll=rng.nextInt(100);
        if(env==0)return roll<25?DEER:roll<47?HARE:roll<63?FOX:roll<78?CROW:roll<90?GOAT:FOREST_SPIRIT;
        if(env==1)return roll<30?DOG:roll<52?HORSE:roll<75?CROW:roll<92?RAT:HARE;
        if(env==2)return roll<34?GOAT:roll<62?CROW:roll<88?RAT:WISP;
        if(env==3)return roll<38?CROW:roll<63?HARE:roll<84?WISP:FOREST_SPIRIT;
        if(env==4)return roll<28?DOG:roll<48?HORSE:roll<72?CROW:RAT;
        if(env==5)return roll<68?RAT:WISP;
        if(env==6)return roll<48?RAT:roll<78?CROW:WISP;
        if(env==7)return roll<42?RAT:roll<65?CROW:WISP;
        return roll<38?WISP:roll<67?ELF:FOREST_SPIRIT;
    }

    private void configureNeutral(Neutral n,int type){
        n.type=type;n.speed=type==DEER?2.0f:type==HARE?2.4f:type==FOX||type==DOG?1.8f:type==HORSE?1.7f:type==RAT?1.45f:1.05f;
        n.magical=type==ELF||type==WISP||type==FOREST_SPIRIT;n.category=n.magical?CreatureSystem.MAGICAL:CreatureSystem.BIOLOGICAL;
        String[] names={"ОЛЕНЬ","ЗАЯЦ","ВОРОН","ЛИСА","ПЁС","ЛОШАДЬ","КРЫСА","ЭЛЬФ ВЕТРА","АСТРАЛЬНЫЙ ОГОНЁК","ДУХ ЛЕСА","ГОРНЫЙ КОЗЁЛ","РЫБА","ЧЕРЕПАХА","ЧАЙКА","ВЕРБЛЮД","ЯЩЕРИЦА","ВЫДРА"};n.name=names[type];
    }

    private void updateWildlife(float dt){
        magicClock-=dt;boolean magicPulse=magicClock<=0f;if(magicPulse)magicClock=.075f;
        for(int i=0;i<wildlife.size();i++){
            Neutral n=wildlife.get(i);n.phase+=dt*(n.magical?2.2f:4.5f);
            if(n.companion){float s=(float)Math.sin(player.yaw),c=(float)Math.cos(player.yaw),tx=player.x-c*1.15f-s*.45f,tz=player.z+s*1.15f-c*.45f;n.x+=(tx-n.x)*Math.min(1f,dt*3.8f);n.z+=(tz-n.z)*Math.min(1f,dt*3.8f);n.y=physics.terrainHeight(n.x,n.z)+1.35f+(float)Math.sin(n.phase)*.18f;n.yaw=player.yaw;if(magicPulse)magicParticle(n,.82f,.78f,.32f);continue;}
            float dx=player.x-n.x,dz=player.z-n.z,d=length(dx,dz);n.wanderTime-=dt;
            if(d<5.2f&&!n.magical){n.yaw=(float)Math.atan2(-dx,-dz);n.wanderTime=1.2f;}
            else if(n.wanderTime<=0f){float hx=n.homeX-n.x,hz=n.homeZ-n.z;n.yaw=length(hx,hz)>8f?(float)Math.atan2(hx,hz):n.yaw+(random.nextFloat()-.5f)*2.4f;n.wanderTime=1.6f+random.nextFloat()*3.4f;}
            float oldX=n.x,oldZ=n.z;float move=n.magical?.32f:(d<5.2f?1.8f:.42f);n.x+=(float)Math.sin(n.yaw)*n.speed*move*dt;n.z+=(float)Math.cos(n.yaw)*n.speed*move*dt;n.x=clamp(n.x,-OCEAN_HALF,OCEAN_HALF);n.z=clamp(n.z,-OCEAN_HALF,OCEAN_HALF);
            float waterDepth=WaterField.depth(currentChapter().environment,n.x,n.z);if(n.type==FISH&&waterDepth<.4f||n.type==CAMEL&&waterDepth>.5f||n.type==LIZARD&&waterDepth>.15f){n.x=oldX;n.z=oldZ;n.yaw+=2.3f;}
            float ground=physics.terrainHeight(n.x,n.z);if(!n.magical&&n.type!=CROW&&n.type!=GULL&&n.type!=FISH&&physics.neutralBlocked(n,ground)){n.x=oldX;n.z=oldZ;n.yaw+=dt*3;n.bump=.22f;ground=physics.terrainHeight(n.x,n.z);}float moved=length(n.x-oldX,n.z-oldZ);n.gait+=moved*7/Math.max(.4f,n.scale);n.moveBlend+=(Math.min(1,moved/Math.max(.001f,dt*n.speed))-n.moveBlend)*Math.min(1,dt*10);n.bump=Math.max(0,n.bump-dt);n.y=n.type==CROW||n.type==GULL?ground+2.1f+(float)Math.sin(n.phase)*.35f:n.magical?ground+(n.type==FOREST_SPIRIT?1.05f:1.25f)+(float)Math.sin(n.phase)*.25f:ground;
            if(n.type==FISH||n.type==TURTLE||n.type==OTTER){float water=WaterField.surface(currentChapter().environment,n.x,n.z,physics.waterTime());if(WaterField.depth(currentChapter().environment,n.x,n.z)>.4f)n.y=water-(n.type==FISH?.45f:.09f);}if(n.type==GULL)n.y=Math.max(n.y,WaterField.level(currentChapter().environment,n.x,n.z)+2.2f);
            if(n.magical&&magicPulse&&((i+(int)(n.phase*3))&1)==0)magicParticle(n,n.type==WISP?.24f:.30f,n.type==FOREST_SPIRIT?.72f:.78f,n.type==WISP?1f:.82f);
        }
    }

    private void magicParticle(Neutral n,float r,float g,float b){Particle p=makeParticle(n.x+(random.nextFloat()-.5f)*.45f,n.y+(random.nextFloat()-.5f)*.32f,n.z+(random.nextFloat()-.5f)*.45f,r,g,b,.025f+random.nextFloat()*.055f,.55f+random.nextFloat()*.55f,5);p.vx=(random.nextFloat()-.5f)*.32f;p.vy=.18f+random.nextFloat()*.42f;p.vz=(random.nextFloat()-.5f)*.32f;particles.add(p);}

    private static int floorChunk(float value){return (int)Math.floor(value/PhysicsWorld.CHUNK_SIZE);}
    private static long chunkKey(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}
    private static String biomeName(int env,float x,float z){if(Math.max(Math.abs(x),Math.abs(z))>145||WaterField.depth(env,x,z)>.5f)return RegionLayout.label(env,x,z);if(env==0)return Math.abs(x-(float)Math.sin(z*.035f)*5f)<7f?"СТАРАЯ ДОРОГА":"ДРЕМУЧАЯ ЧАЩА";String[] names={"СТАРАЯ ДОРОГА","УЛИЦЫ КОКИ","СКАЛЫ КРЕПОСТИ","ДОЛИНА ВИСЕЛИЦ","ПЛОЩАДЬ ОЧИЩЕНИЯ","СТАРЫЕ СТОКИ","ВЕРХНИЙ ЗАМОК","ВЛАДЕНИЯ ГРАФА","МЕЖДУМИРЬЕ"};return names[env];}

    private void configureEnemy(Actor e, int type) {
        float scale = 1f + chapter * .075f;
        e.type = type; e.radius = .48f; e.height = 1.85f; e.range = 1.42f;
        e.speed = 3.1f; e.damage = 15f * scale; e.maxHp = 105f * scale;
        e.colorR = .28f; e.colorG = .24f; e.colorB = .22f; e.roughness = .72f; e.metalness = .12f;
        e.name = "ВРАГ";
        EnemyDefinitions.apply(e,type,scale);
        e.category=CreatureSystem.category(type);e.hp = e.maxHp; e.phase = 1; e.attackCooldown = .45f + random.nextFloat();
    }

    private void updateEnemy(Actor e, float dt) {
        if(e.slowTime>0)dt*=.45f;
        tickActorTimers(e, dt);
        if (e.dead) { e.deathTime -= dt; e.x += e.vx * dt; e.z += e.vz * dt; e.vx *= SimulationClock.damping(12.6f,dt); e.vz *= SimulationClock.damping(12.6f,dt); return; }
        if (e.spawnTime > 0f) return;
        if(mushrooms.active(MushroomEffects.PEACE)){e.strikePending=false;e.attackTime=0;e.vx*=SimulationClock.damping(8,dt);e.vz*=SimulationClock.damping(8,dt);return;}
        if(e.strikePending){if(e.stun>0||player.dead)e.strikePending=false;else if(e.attackTime<=e.strikeAt){e.strikePending=false;enemyStrike(e,distance(player,e));}}
        if(e.type>=TUSK_BOAR){CreatureSystem.update(this,e,dt);constrain(e);return;}
        if(e.surreal){updateOddity(e,dt);return;}

        if (e.type == COUNT_APOSTLE) {
            float ratio = e.hp / e.maxHp; e.phase = ratio < .32f ? 3 : ratio < .66f ? 2 : 1;
            if (ratio < .66f && !e.summonedA) { e.summonedA=true; spawnNear(e, GHOUL, 3); banner="ПЛОТЬ ЗОВЁТ СЛУГ"; bannerTime=1.4f; }
            if (ratio < .32f && !e.summonedB) { e.summonedB=true; spawnNear(e, APOSTLE_SPAWN, 2); damageFlash=.45f; }
        }

        float dx = player.x - e.x, dz = player.z - e.z, d = length(dx, dz);
        if(e.ambient&&d>19f){
            e.wanderTime-=dt;if(e.wanderTime<=0f){float hx=e.homeX-e.x,hz=e.homeZ-e.z;e.wanderYaw=length(hx,hz)>7f?(float)Math.atan2(hx,hz):e.wanderYaw+(random.nextFloat()-.5f)*2.2f;e.wanderTime=1.2f+random.nextFloat()*3.2f;}
            e.yaw=lerpAngle(e.yaw,e.wanderYaw,Math.min(1f,dt*3f));float roam=e.speed*.22f;e.vx+=((float)Math.sin(e.yaw)*roam-e.vx)*Math.min(1f,dt*3f);e.vz+=((float)Math.cos(e.yaw)*roam-e.vz)*Math.min(1f,dt*3f);e.x+=e.vx*dt;e.z+=e.vz*dt;constrain(e);return;
        }
        e.yaw = lerpAngle(e.yaw, (float)Math.atan2(dx, dz), Math.min(1f, dt * 7f));
        if (e.stun > 0f) { e.x += e.vx * dt; e.z += e.vz * dt; e.vx *= SimulationClock.damping(15.3f,dt); e.vz *= SimulationClock.damping(15.3f,dt); constrain(e); return; }

        e.specialCooldown -= dt;
        if ((e.boss || e.type == VOID_BEAST) && e.specialCooldown <= 0f) {
            enemySpecial(e); e.specialCooldown = e.type == COUNT_APOSTLE ? Math.max(1.65f, 3.2f - e.phase * .42f) : 3f;
        }

        float desiredRange = e.type == CROSSBOW ? 7.3f : e.range * .82f;
        if (d > desiredRange) {
            float steering=CombatSystem.steer(this,e,NavigationSystem.direction(this,e,(float)Math.atan2(dx,dz)));
            float speed = e.speed * (e.type == COUNT_APOSTLE && e.phase == 3 ? 1.22f : 1f);
            e.vx += ((float)Math.sin(steering) * speed - e.vx) * Math.min(1f, dt * 6f);
            e.vz += ((float)Math.cos(steering) * speed - e.vz) * Math.min(1f, dt * 6f);
            e.x += e.vx * dt; e.z += e.vz * dt;
        } else if (e.attackCooldown <= 0f) {
            e.attackTime = e.boss ? .7f : .46f;
            e.attackCooldown = e.type == CROSSBOW ? 1.7f : e.boss ? 1.25f : .95f + random.nextFloat() * .45f;
            e.strikePending=true;e.strikeAt=e.attackTime-(e.boss?.40f:.24f);
        }
        constrain(e);
    }

    private void enemyStrike(Actor e, float distance) {
        if(!CombatSystem.visible(this,e,player))return;
        if (e.type == CROSSBOW) { hostileProjectile(e, 12f, e.damage, 0f); emit(EVENT_SLASH); return; }
        if (distance <= e.range + player.radius + .55f) damagePlayer(e.damage, e.x, e.z);
        slashBurst(e.x, e.height * .62f, e.z, e.boss ? 12 : 5);
    }

    private void enemySpecial(Actor e) {
        if (e.type == COUNT_APOSTLE) {
            int shots = 3 + e.phase * 2;
            for (int i = 0; i < shots; i++) hostileProjectile(e, 8.3f + i * .25f, 19f + e.phase * 3f, (i - (shots - 1) * .5f) * .17f);
        } else if (e.type == SNAKE_BARON || e.type == ABYSS_DEMON || e.type == VOID_BEAST) {
            float dx = player.x - e.x, dz = player.z - e.z, len = Math.max(.1f, length(dx, dz));
            e.vx = dx / len * 11f; e.vz = dz / len * 11f;
            if (len < 4.2f) damagePlayer(e.damage * .82f, e.x, e.z);
            if (e.type == SNAKE_BARON) { hostileProjectile(e, 9f, 22f, -.16f); hostileProjectile(e, 9f, 22f, .16f); }
        }
        emit(EVENT_BOSS);
    }

    private void hostileProjectile(Actor e, float speed, float damage, float spread) {
        float angle = (float)Math.atan2(player.x - e.x, player.z - e.z) + spread;
        Projectile p = new Projectile(); p.x=e.x; p.y=e.y+e.height*.58f; p.z=e.z;
        p.vx=(float)Math.sin(angle)*speed; p.vz=(float)Math.cos(angle)*speed; p.vy=(player.y+player.height*.55f-p.y)/Math.max(.3f,distance(player,e)/speed);
        p.radius=e.boss?.28f:.16f; p.damage=damage; p.life=3f; p.friendly=false; p.pierce=1;
        p.r=.78f; p.g=.12f; p.b=.22f; projectiles.add(p);
    }

    void damagePlayer(float damage, float fromX, float fromZ) {
        if (player.dead || player.invulnerable > 0f) {
            if (!player.dead && player.dodgeTime > .08f && slowMotion <= 0f) {
                slowMotion = .42f; player.rage = Math.min(100f, player.rage + 18f);
                hitChain=Math.max(2,hitChain+1);hitChainTime=3.4f;
                banner = "ИДЕАЛЬНЫЙ РЫВОК"; bannerTime = .75f;
            }
            return;
        }
        float actual = Math.max(1f, damage * (1f - progress.armor)*effects.damageScale());
        player.hitReaction=1;player.hp -= actual; player.hitFlash = 1f; player.stun = .22f; player.invulnerable = .52f;
        float dx=player.x-fromX,dz=player.z-fromZ,len=Math.max(.1f,length(dx,dz));player.vx=dx/len*6f;player.vz=dz/len*6f;
        player.rage = Math.min(100f, player.rage + 16f); damageFlash = .55f; bloodBurst(player.x,1.1f,player.z,12); emit(EVENT_HIT);
        if (player.hp <= 0f) {player.hp=0f;player.dead=true;player.deathTime=1.2f;input.clearMovement();if(random.nextFloat()<.35f)physics.detachLimb(player,random.nextBoolean()?CharacterSkeleton.GROUP_LEFT_ARM:CharacterSkeleton.GROUP_RIGHT_ARM,player.vx,player.vz);dropWeaponOnce(player);physics.spawnRagdoll(player);}
    }

    void damageEnemy(Actor e, float damage, float knockX, float knockZ, float stun) {
        if (e.dead || e.invulnerable > 0f) return;
        e.peaceful=false;
        float amount = damage * (e.shieldTime>0?.32f:1f) * (.92f + random.nextFloat() * .16f);
        e.hitReaction=1;e.hp -= amount; e.hitFlash=1f; e.stun=Math.max(e.stun,stun); e.vx+=knockX; e.vz+=knockZ;
        hitChain++;hitChainTime=3.2f;
        player.rage=Math.min(100f,player.rage+(e.boss?5f:8f)); if(e.category==CreatureSystem.MECHANICAL)fireBurst(e.x,e.y+e.height*.58f,e.z,10);else if(e.category==CreatureSystem.MAGICAL)astralBurst(e.x,e.y+e.height*.58f,e.z,e.colorR,e.colorG,e.colorB,10);else bloodBurst(e.x,e.y+e.height*.58f,e.z,e.boss?18:8);
        tryDismember(e,amount,knockX,knockZ);
        if(e.hp<=0f){e.hp=0f;e.dead=true;e.deathTime=.85f;dropWeaponOnce(e);physics.spawnRagdoll(e);progress.totalKills++;player.rage=Math.min(100f,player.rage+12f);if(random.nextFloat()<.14f&&ammo<maxAmmo)ammo++;if(e.boss){slowMotion=.6f;emit(EVENT_BOSS);}}
    }

    private void tryDismember(Actor e,float amount,float knockX,float knockZ){
        if(e.boss||e.type==WRAITH||e.type==SHADE)return;float chance=player.attackKind==6?.72f:player.attackKind==5?.34f:player.attackKind==4?.22f:.055f;if(amount<player.damage*1.05f||random.nextFloat()>chance)return;
        int[] groups={CharacterSkeleton.GROUP_HEAD,CharacterSkeleton.GROUP_LEFT_ARM,CharacterSkeleton.GROUP_RIGHT_ARM,CharacterSkeleton.GROUP_LEFT_LEG,CharacterSkeleton.GROUP_RIGHT_LEG};int group=groups[random.nextInt(groups.length)];
        if(physics.detachLimb(e,group,knockX*.45f,knockZ*.45f)){bloodBurst(e.x,e.height*.62f,e.z,18);if(group==CharacterSkeleton.GROUP_HEAD)e.hp=0;else if(group==CharacterSkeleton.GROUP_LEFT_LEG||group==CharacterSkeleton.GROUP_RIGHT_LEG)e.speed*=.56f;else{e.damage*=.62f;if(group==CharacterSkeleton.GROUP_RIGHT_ARM)dropWeaponOnce(e);}}
    }

    private void dropWeaponOnce(Actor actor){if(actor.weaponDropped)return;actor.weaponDropped=true;physics.spawnWeapon(actor);}

    private void updateProjectiles(float dt){CombatSystem.projectiles(this,dt);}

    private void updateParticles(float dt) {
        int limit=qualityLevel>=3?260:qualityLevel==2?170:100;
        while(particles.size()>limit)particles.remove(0);
        for(int i=particles.size()-1;i>=0;i--){Particle p=particles.get(i);if(!p.floating&&WaterField.depth(currentChapter().environment,p.x,p.z)>.1f&&p.y-WaterField.level(currentChapter().environment,p.x,p.z)<3)p.life=Math.max(p.life,2.5f);p.life-=dt;if(p.life<=0f){particles.remove(i);continue;}p.x+=p.vx*dt;p.y+=p.vy*dt;p.z+=p.vz*dt;if(p.kind==5){p.vy+=.12f*dt;p.vx*=SimulationClock.damping(.96f,dt);p.vz*=SimulationClock.damping(.96f,dt);}else{p.vy-=3.8f*dt;p.vx*=SimulationClock.damping(1.81f,dt);p.vz*=SimulationClock.damping(1.81f,dt);float floor=physics.terrainHeight(p.x,p.z)+.03f;if(p.y<floor){p.y=floor;p.vy*=-.18f;}}WaterField.particle(currentChapter().environment,p,physics.waterTime(),dt);}
    }

    private void tickActorTimers(Actor a,float dt){a.poisonTime=Math.max(0,a.poisonTime-dt);a.slowTime=Math.max(0,a.slowTime-dt);a.shieldTime=Math.max(0,a.shieldTime-dt);a.navCooldown=Math.max(0,a.navCooldown-dt);float blend=1-SimulationClock.damping(12,dt);a.attackBlend+=((a.attackTime>0?1:0)-a.attackBlend)*blend;a.attackTime=Math.max(0f,a.attackTime-dt);a.attackCooldown=Math.max(0f,a.attackCooldown-dt);a.heavyCooldown=Math.max(0f,a.heavyCooldown-dt);a.cannonCooldown=Math.max(0f,a.cannonCooldown-dt);a.dodgeTime=Math.max(0f,a.dodgeTime-dt);a.dodgeCooldown=Math.max(0f,a.dodgeCooldown-dt);a.invulnerable=Math.max(0f,a.invulnerable-dt);a.stun=Math.max(0f,a.stun-dt);a.hitFlash=Math.max(0f,a.hitFlash-dt*5f);a.rageTime=Math.max(0f,a.rageTime-dt);a.spawnTime=Math.max(0f,a.spawnTime-dt);a.comboWindow=Math.max(0f,a.comboWindow-dt);}
    private void constrain(Actor a){a.x=clamp(a.x,-OCEAN_HALF,OCEAN_HALF);a.z=clamp(a.z,-OCEAN_HALF,OCEAN_HALF);}

    private void spawnNear(Actor source,int type,int count){for(int i=0;i<count;i++){Actor e=new Actor();configureEnemy(e,type);float a=i*6.28318f/count+random.nextFloat()*.4f;e.x=clamp(source.x+(float)Math.sin(a)*3.5f,-WORLD_HALF+2f,WORLD_HALF-2f);e.z=clamp(source.z+(float)Math.cos(a)*3.5f,-WORLD_HALF+2f,WORLD_HALF-2f);e.yaw=(float)Math.atan2(player.x-e.x,player.z-e.z);e.spawnTime=.7f;enemies.add(e);dustBurst(e.x,0,e.z,10);}}
    private Particle makeParticle(float x,float y,float z,float r,float g,float b,float size,float life,int kind){Particle p=new Particle();p.x=x;p.y=y;p.z=z;p.r=r;p.g=g;p.b=b;p.size=size;p.life=life;p.maxLife=life;p.kind=kind;return p;}
    private void bloodBurst(float x,float y,float z,int count){for(int i=0;i<count;i++){Particle p=makeParticle(x,y,z,.55f,.015f,.035f,.04f+random.nextFloat()*.08f,.35f+random.nextFloat()*.45f,1);p.vx=(random.nextFloat()-.5f)*6f;p.vy=random.nextFloat()*5f;p.vz=(random.nextFloat()-.5f)*6f;particles.add(p);}}
    private void dustBurst(float x,float y,float z,int count){for(int i=0;i<count;i++){Particle p=makeParticle(x,y,z,.26f,.24f,.22f,.08f+random.nextFloat()*.12f,.35f+random.nextFloat()*.5f,2);p.vx=(random.nextFloat()-.5f)*3f;p.vy=.5f+random.nextFloat()*1.5f;p.vz=(random.nextFloat()-.5f)*3f;particles.add(p);}}
    void fireBurst(float x,float y,float z,int count){for(int i=0;i<count;i++){Particle p=makeParticle(x,y,z,1f,.25f,.045f,.045f+random.nextFloat()*.09f,.2f+random.nextFloat()*.35f,3);p.vx=(random.nextFloat()-.5f)*8f;p.vy=(random.nextFloat()-.2f)*6f;p.vz=(random.nextFloat()-.5f)*8f;particles.add(p);}}
    private void slashBurst(float x,float y,float z,int count){for(int i=0;i<count;i++){Particle p=makeParticle(x,y,z,.9f,.86f,.76f,.025f+random.nextFloat()*.055f,.12f+random.nextFloat()*.2f,4);p.vx=(random.nextFloat()-.5f)*10f;p.vy=(random.nextFloat()-.25f)*5f;p.vz=(random.nextFloat()-.5f)*10f;particles.add(p);}}
    void spawnTrail(float x,float y,float z,float r,float g,float b,float size){if(random.nextFloat()>.65f)return;Particle p=makeParticle(x,y,z,r,g,b,size,.18f+random.nextFloat()*.15f,3);p.vy=.25f;particles.add(p);}

    private void publishHud(){Actor observed=lockedTarget;float near=14;for(Actor a:enemies){float d=distance(player,a);if(!a.dead&&d<near){near=d;if(lockedTarget==null)observed=a;}}hudCreature=observed==null?"":observed.name+" · "+CreatureSystem.CATEGORIES[observed.category];hudMushroom0=mushrooms.status(0);hudMushroom1=mushrooms.status(1);hudMushroom2=mushrooms.status(2);hudMushroom3=mushrooms.status(3);hudPlayerName=player.name;hudEffectAttack=effects.status();hudEffectDefense=effects.defenseStatus();hudForm=effects.form>=0?"ФОРМА: "+player.name+" · "+(int)Math.ceil(effects.formTime)+"с":"";hudPickup=quest.hint(this).length()>0?quest.hint(this):exploration.hint.length()>0?exploration.hint:flora.hint.length()>0?flora.hint:surreal.hint;hudDiscovered=surreal.discovered;hudLoadedToys=surreal.loadedProps;hudZone=player.swimming?"ПЛАВАНИЕ · ПРЫЖОК ДЛЯ ВЫХОДА ИЗ ВОДЫ":Math.max(Math.abs(player.x),Math.abs(player.z))>145?RegionLayout.label(currentChapter().environment,player.x,player.z):surreal.props.isEmpty()?"":SurrealWorld.ZONE_NAMES[surreal.nearestZone];hudPlayerX=player.x;hudPlayerZ=player.z;hudHealth=player.hp;hudMaxHealth=player.maxHp;hudRage=player.rage;hudAmmo=ammo;hudMaxAmmo=maxAmmo;enemiesAlive=0;ambientEnemies=0;neutralCreatures=wildlife.size();bossName="";bossHealth=0f;bossMaxHealth=0f;for(int i=0;i<enemies.size();i++){Actor e=enemies.get(i);if(!e.dead){if(e.peaceful)neutralCreatures++;else if(e.ambient)ambientEnemies++;else enemiesAlive++;if(e.boss&&!e.ambient){bossName=e.name;bossHealth=e.hp;bossMaxHealth=e.maxHp;}}}}
    private void save(){store.save(progress.copy());hasSave=true;saveCheckpoint();}
    public void saveCheckpoint(){if(!(store instanceof SnapshotCodec.Store)||mode==MENU)return;try{((SnapshotCodec.Store)store).saveCheckpoint(SnapshotCodec.encode(this));checkpointStatus="СОХРАНЕНО";}catch(Exception error){checkpointStatus="ОШИБКА СОХРАНЕНИЯ";}}
    public void pauseAndSave(){input.resetAll();tiltEnabled=false;input.tiltEnabled=false;if(mode==PLAYING)mode=PAUSED;save();}
    public void migrateWorld(){if(worldVersion<7||encounters==null){encounters=new UfoEncounters();encounters.reset(this);worldVersion=7;for(Actor a:enemies)a.category=CreatureSystem.category(a.type);for(ArrayList<Actor> list:archivedEnemies.values())for(Actor a:list)a.category=CreatureSystem.category(a.type);for(Neutral n:wildlife)n.category=n.magical?CreatureSystem.MAGICAL:CreatureSystem.BIOLOGICAL;}if(mushrooms==null)mushrooms=new MushroomEffects();if(flora==null)flora=new FloraWorld();worldVersion=8;reserveActorIds();}
    public void reserveActorIds(){int max=player.id;for(Actor a:enemies)max=Math.max(max,a.id);for(ArrayList<Actor> list:archivedEnemies.values())for(Actor a:list)max=Math.max(max,a.id);Actor.nextActorId=Math.max(Actor.nextActorId,max+1);}

    public java.util.ArrayDeque<Integer> feedbackQueue=new java.util.ArrayDeque<Integer>();
    private void emit(int event){feedbackEvent=event;feedbackSerial++;if(feedbackQueue.size()<24)feedbackQueue.add(event);}
    public void setQualityLevel(int level){qualityLevel=clampInt(level,1,3);physics.solverIterations=6;performanceLabel=qualityLevel==3?"QUALITY: ULTRA":qualityLevel==2?"QUALITY: HIGH":"QUALITY: SUSTAINED";}

    static float distance(Actor a,Actor b){return length(a.x-b.x,a.z-b.z);}
    private static float length(float x,float y){return(float)Math.sqrt(x*x+y*y);}
    private static float clamp(float v,float lo,float hi){return v<lo?lo:v>hi?hi:v;}
    private static int clampInt(int v,int lo,int hi){return v<lo?lo:v>hi?hi:v;}
    private static float lerpAngle(float a,float b,float t){float d=b-a;while(d>Math.PI)d-=(float)(Math.PI*2);while(d<-Math.PI)d+=(float)(Math.PI*2);return a+d*t;}
}
