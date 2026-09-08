package com.minecraftloveyou.client.compat;

import com.minecraftloveyou.WorldLovesYouMod;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;

/**
 * Xaero 小地图/世界地图 软联动（全反射，未装 Xaero 时自动跳过）。
 * 在玩家附近检测到新墓碑方块时，添加命名路点"墓碑"。
 * 反射链：
 *   XaeroMinimapSession.getCurrentSession() → (MinimapSession) → getWorldManager()
 *   → getCurrentWorld() → getCurrentWaypointSet() → add(new Waypoint(x,y,z,name,initials,color))
 */
public final class XaeroCompat {

    private static boolean xaeroChecked = false;
    private static boolean xaeroPresent = false;
    private static Method mGetCurrentSession;
    private static Method mGetWorldManager;
    private static Method mGetCurrentWorld;
    private static Method mGetCurrentWaypointSet;
    private static Method mSetAdd;
    private static java.lang.reflect.Constructor<?> waypointCtor;

    private XaeroCompat() {
    }

    public static boolean isXaeroPresent() {
        ensureInit();
        return xaeroPresent;
    }

    private static synchronized void ensureInit() {
        if (xaeroChecked) {
            return;
        }
        xaeroChecked = true;
        try {
            if (FabricLoader.getInstance().isModLoaded("xaerominimap")) {
                Class<?> sessionClass = Class.forName("xaero.common.XaeroMinimapSession");
                Class<?> minimapSession = Class.forName("xaero.hud.minimap.module.MinimapSession");
                Class<?> worldManager = Class.forName("xaero.hud.minimap.world.MinimapWorldManager");
                Class<?> minimapWorld = Class.forName("xaero.hud.minimap.world.MinimapWorld");
                Class<?> waypointSet = Class.forName("xaero.hud.minimap.waypoint.set.WaypointSet");
                Class<?> waypoint = Class.forName("xaero.common.minimap.waypoints.Waypoint");

                mGetCurrentSession = sessionClass.getMethod("getCurrentSession");
                mGetWorldManager = minimapSession.getMethod("getWorldManager");
                mGetCurrentWorld = worldManager.getMethod("getCurrentWorld");
                mGetCurrentWaypointSet = minimapWorld.getMethod("getCurrentWaypointSet");
                mSetAdd = waypointSet.getMethod("add", waypoint);
                // Waypoint(int x, int y, int z, String name, String initials, int colorInt)
                waypointCtor = waypoint.getConstructor(int.class, int.class, int.class, String.class, String.class, int.class);
                xaeroPresent = true;
                WorldLovesYouMod.LOGGER.info("[世界爱你] Xaero 小地图已检测，墓碑联动可用");
            }
        } catch (Throwable t) {
            xaeroPresent = false;
            WorldLovesYouMod.LOGGER.info("[世界爱你] 未检测到可用的 Xaero 小地图 API（{}），墓碑联动关闭", t.toString());
        }
    }

    /** 添加墓碑路点；返回是否成功。 */
    public static boolean addGraveWaypoint(int x, int y, int z, String name) {
        if (!isXaeroPresent()) {
            return false;
        }
        try {
            Object session = mGetCurrentSession.invoke(null);
            if (session == null) {
                return false;
            }
            Object wm = mGetWorldManager.invoke(session);
            Object world = mGetCurrentWorld.invoke(wm);
            if (world == null) {
                return false;
            }
            Object set = mGetCurrentWaypointSet.invoke(world);
            if (set == null) {
                return false;
            }
            Object wp = waypointCtor.newInstance(x, y, z, name, "G", 0xFFAA33);
            mSetAdd.invoke(set, wp);
            return true;
        } catch (Throwable t) {
            WorldLovesYouMod.LOGGER.warn("[世界爱你] 添加 Xaero 路点失败：{}", t.toString());
            return false;
        }
    }
}