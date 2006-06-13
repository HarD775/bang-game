#
# $Id$
#
# Proguard configuration file for Game Gardens client

-injars ../lib/jme.jar(!META-INF/*)
-injars ../lib/jme-effects.jar(!META-INF/*)
-injars ../lib/jme-awt.jar(!META-INF/*)

-libraryjars ../lib/lwjgl.jar
-libraryjars <java.home>/lib/rt.jar

-dontobfuscate

-outjars ../dist/vmodel-pro.jar

-keep public class * implements com.jme.util.export.Savable {
    *;
}

-keep public class jmetest.effects.RenParticleEditor {
    public static void main (java.lang.String[]);
}
