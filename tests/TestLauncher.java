package com.danil.blackswordsman;
public final class TestLauncher {
 public static void main(String[] args)throws Exception{EnemyDefinitions.load(new java.io.FileInputStream("app/src/main/assets/enemies.tsv"));if(new java.io.File("app/src/main/assets/items.tsv").exists())SurrealCatalog.load(new java.io.FileInputStream("app/src/main/assets/items.tsv"));try{Class.forName("com.danil.blackswordsman."+args[0]).getMethod("main",String[].class).invoke(null,(Object)new String[0]);}catch(java.lang.reflect.InvocationTargetException error){Throwable cause=error.getCause();cause.printStackTrace();System.exit(1);}}
}
