package com.minecraftloveyou;

import com.minecraftloveyou.core.ModuleManager;
import com.minecraftloveyou.module.graves.GravesModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/** 服务端自检（仅当环境变量 WLY_SELFTEST=1 时触发）：逐项断言并打印 PASS/FAIL。 */
public final class SelfTest {

    private SelfTest() {
    }

    public static void run(MinecraftServer server) {
        List<String> out = new ArrayList<>();
        int pass = 0;
        int fail = 0;

        // T1 模块数量
        int mc = ModuleManager.ids().size();
        if (mc >= 35) {
            pass++;
            out.add("PASS T1 模块数量=" + mc);
        } else {
            fail++;
            out.add("FAIL T1 模块数量=" + mc);
        }

        // T2 大堆叠：普通物品 999
        ItemStack spruce = new ItemStack(Items.SPRUCE_LOG, 1);
        if (spruce.getMaxStackSize() == 999) {
            pass++;
            out.add("PASS T2 普通物品堆叠上限=999（实测 " + spruce.getMaxStackSize() + "）");
        } else {
            fail++;
            out.add("FAIL T2 普通物品堆叠上限=" + spruce.getMaxStackSize() + "（期望 999）");
        }

        // T3 耐久物品不可堆
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD, 1);
        if (sword.getMaxStackSize() <= 1) {
            pass++;
            out.add("PASS T3 耐久工具不可堆（实测 " + sword.getMaxStackSize() + "）");
        } else {
            fail++;
            out.add("FAIL T3 耐久工具上限=" + sword.getMaxStackSize() + "（期望 1）");
        }

        // T4 容器层 999
        SimpleContainer cont = new SimpleContainer(27);
        if (cont.getMaxStackSize() == 999) {
            pass++;
            out.add("PASS T4 容器堆叠上限=999");
        } else {
            fail++;
            out.add("FAIL T4 容器上限=" + cont.getMaxStackSize() + "（期望 999）");
        }

        // T5 容器+物品组合
        if (cont.getMaxStackSize(spruce) == 999) {
            pass++;
            out.add("PASS T5 容器×物品组合上限=999");
        } else {
            fail++;
            out.add("FAIL T5 组合上限=" + cont.getMaxStackSize(spruce) + "（期望 999）");
        }

        // T6 CODEC 放宽（此前的踢线点：count 999 重编码须通过）
        try {
            RegistryAccess ra = server.overworld().registryAccess();
            var ops = net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, ra);
            var res = ItemStack.CODEC.encodeStart(ops, new ItemStack(Items.COBBLESTONE, 999));
            if (res.result().isPresent()) {
                pass++;
                out.add("PASS T6 CODEC 重编码 count=999 通过（不会踢线）");
            } else {
                fail++;
                out.add("FAIL T6 CODEC 重编码失败：" + res.error().orElse(null));
            }
        } catch (Throwable t) {
            fail++;
            out.add("FAIL T6 CODEC 异常：" + t);
        }

        // T7 墓碑注册 + 不遮挡面（noOcclusion 修复）
        try {
            var block = BuiltInRegistries.BLOCK.get(GravesModule.GRAVE_ID).map(net.minecraft.core.Holder.Reference::value).orElse(null);
            if (block == null) {
                fail++;
                out.add("FAIL T7 墓碑未注册");
            } else {
                var state = block.defaultBlockState();
                BlockPos pos = new BlockPos(0, 80, 0);
                // 26.2：noOcclusion 只关 canOcclude（该开关负责邻块面剔除修复）；isViewBlocking 不受影响
                boolean occludes = state.canOcclude();
                if (!occludes) {
                    pass++;
                    out.add("PASS T7 墓碑 canOcclude=false（邻块面不剔除，透视修复生效）");
                } else {
                    fail++;
                    out.add("FAIL T7 墓碑 canOcclude=" + occludes);
                }
            }
        } catch (Throwable t) {
            fail++;
            out.add("FAIL T7 墓碑检查异常：" + t);
        }

        // T8 bigstacktest 调试命令存在
        boolean cmd = server.getCommands().getDispatcher().getRoot().getChild("bigstacktest") != null;
        if (cmd) {
            pass++;
            out.add("PASS T8 /bigstacktest 命令已注册");
        } else {
            fail++;
            out.add("FAIL T8 /bigstacktest 命令缺失");
        }

        // T9 ctm 玻璃无缝纹理资源存在
        boolean tex = SelfTest.class.getClassLoader().getResource("assets/minecraft/textures/block/glass.png") != null;
        if (tex) {
            pass++;
            out.add("PASS T9 ctm 玻璃无缝纹理资源存在");
        } else {
            fail++;
            out.add("FAIL T9 ctm 玻璃纹理缺失");
        }

        // T10 语言文件：zh_cn 必备键 + **中英键集必须完全一致**
        //     历史坑：v1.2.4 漏了 en_us 的 "wly.module.anvil-no-limit"（英文界面显示裸键名），
        //     旧版 T10 只做 contains 抽样，抓不到 → 改为逐键比对。
        try (var zin = SelfTest.class.getClassLoader().getResourceAsStream("assets/minecraft_love_you/lang/zh_cn.json");
             var ein = SelfTest.class.getClassLoader().getResourceAsStream("assets/minecraft_love_you/lang/en_us.json")) {
            String z = zin != null ? new String(zin.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8) : "";
            String e = ein != null ? new String(ein.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8) : "";
            java.util.Set<String> zk = langKeys(z);
            java.util.Set<String> ek = langKeys(e);
            java.util.Set<String> onlyZh = new java.util.TreeSet<>(zk);
            onlyZh.removeAll(ek);
            java.util.Set<String> onlyEn = new java.util.TreeSet<>(ek);
            onlyEn.removeAll(zk);
            boolean ok = z.contains("key.minecraft_love_you.sort") && !zk.isEmpty()
                    && onlyZh.isEmpty() && onlyEn.isEmpty();
            if (ok) {
                pass++;
                out.add("PASS T10 语言文件条目存在（zh/en 键集一致，各 " + zk.size() + " 键）");
            } else {
                fail++;
                out.add("FAIL T10 语言键集不一致（仅 zh 有 " + onlyZh + "；仅 en 有 " + onlyEn + "）");
            }
        } catch (Throwable t) {
            fail++;
            out.add("FAIL T10 语言文件异常：" + t);
        }

        // T11 powershot 新字段接线（初速/伤害开关/上限）
        double pv = com.minecraftloveyou.module.powershot.PowershotModule.powerVelocity;
        boolean dmg = com.minecraftloveyou.module.powershot.PowershotModule.damageEnabled;
        int dmx = com.minecraftloveyou.module.powershot.PowershotModule.damageMax;
        if (pv >= 0 && pv <= 10 && !dmg && dmx == 50) {
            pass++;
            out.add("PASS T11 powershot 接线（初速=" + pv + " 伤害=" + dmg + " 上限=" + dmx + "）");
        } else {
            fail++;
            out.add("FAIL T11 powershot 接线（初速=" + pv + " 伤害=" + dmg + " 上限=" + dmx + "）");
        }

        // T12 投掷物伤害运行时测试：雪球 onHitEntity 反射调用，验证血量下降（证明 mixin 生效）
        try {
            var level = server.overworld();
            var zombieType = BuiltInRegistries.ENTITY_TYPE
                    .get(net.minecraft.resources.Identifier.withDefaultNamespace("zombie"))
                    .map(net.minecraft.core.Holder.Reference::value).orElse(null);
            net.minecraft.world.entity.Entity zombie = zombieType == null ? null
                    : zombieType.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
            if (!(zombie instanceof net.minecraft.world.entity.LivingEntity le)) {
                fail++;
                out.add("FAIL T12 无法创建测试僵尸");
            } else {
                le.setPos(0, 80, 0);
                level.addFreshEntity(le);
                double oldPv = com.minecraftloveyou.module.powershot.PowershotModule.powerVelocity;
                boolean oldDmg = com.minecraftloveyou.module.powershot.PowershotModule.damageEnabled;
                int oldDmx = com.minecraftloveyou.module.powershot.PowershotModule.damageMax;
                com.minecraftloveyou.module.powershot.PowershotModule.powerVelocity = 2.0;
                com.minecraftloveyou.module.powershot.PowershotModule.damageEnabled = true;
                com.minecraftloveyou.module.powershot.PowershotModule.damageMax = 50;
                try {
                    var m = net.minecraft.world.entity.projectile.Projectile.class
                            .getDeclaredMethod("onHitEntity", net.minecraft.world.phys.EntityHitResult.class);
                    m.setAccessible(true);
                    var hit = new net.minecraft.world.phys.EntityHitResult(le, le.position());

                    // 反例：没有 owner 的雪球（怪物/发射器来源）→ 新作用域下不应造成伤害
                    var stray = new net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball(
                            level, 0.5, 81, 0.5, net.minecraft.world.item.ItemStack.EMPTY);
                    stray.setPos(0.5, 81, 0.5);
                    float hp0 = le.getHealth();
                    m.invoke(stray, hit);
                    boolean rejected = le.getHealth() == hp0;

                    // 正向：owner 是玩家（本自检构造的假玩家对象）→ 应造成伤害
                    var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "wly_selftest");
                    var fakePlayer = new net.minecraft.server.level.ServerPlayer(server, level, profile,
                            net.minecraft.server.level.ClientInformation.createDefault());
                    // 宠物样本：雪傀儡（无 owner 字段，只能由玩家建造 → 按类型整体归入玩家阵营）
                    var fakePet = new net.minecraft.world.entity.animal.golem.SnowGolem(
                            snowGolemType(), level);
                    var owned = new net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball(
                            level, 0.5, 81, 0.5, net.minecraft.world.item.ItemStack.EMPTY);
                    owned.setPos(0.5, 81, 0.5);
                    owned.setOwner(fakePlayer);
                    float hp1 = le.getHealth();
                    m.invoke(owned, hit);
                    float dealt = hp1 - le.getHealth();

                    // 过滤函数自身行为（玩家=true / 无主=false / 怪物=false / 宠物=true）
                    boolean filterOk = com.minecraftloveyou.core.PlayerSide.isPlayerOrPet(fakePlayer)
                            && !com.minecraftloveyou.core.PlayerSide.isPlayerOrPet(null)
                            && !com.minecraftloveyou.core.PlayerSide.isPlayerOrPet(le)
                            && com.minecraftloveyou.core.PlayerSide.isPlayerOrPet(fakePet);

                    // v=2 → dmg=round(2*5)=10
                    if (rejected && dealt >= 5 && dealt <= 15 && filterOk) {
                        pass++;
                        out.add("PASS T12 投掷物伤害作用域（无主不伤 / 玩家生效 掉血 " + dealt + "）");
                    } else {
                        fail++;
                        out.add("FAIL T12 伤害作用域异常（无主拦截=" + rejected
                                + " 玩家掉血=" + dealt + "（期望 5~15） 过滤=" + filterOk + "）");
                    }
                } finally {
                    le.discard();
                    com.minecraftloveyou.module.powershot.PowershotModule.powerVelocity = oldPv;
                    com.minecraftloveyou.module.powershot.PowershotModule.damageEnabled = oldDmg;
                    com.minecraftloveyou.module.powershot.PowershotModule.damageMax = oldDmx;
                }
            }
        } catch (Throwable t) {
            fail++;
            out.add("FAIL T12 伤害测试异常：" + t);
        }

        // T13 离线追赶：放一个熔炉 + 原料 + 燃料，模拟「离线 10 分钟」，验证烧炼真的被补偿
        try {
            var level = server.overworld();
            var pos = new net.minecraft.core.BlockPos(8, 80, 8);
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.FURNACE.defaultBlockState(), 3);
            var be = level.getBlockEntity(pos);
            if (!(be instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace)) {
                fail++;
                out.add("FAIL T13 无法放置测试熔炉");
            } else {
                furnace.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RAW_IRON, 4));
                furnace.setItem(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COAL, 1));
                com.minecraftloveyou.module.offlinecatchup.CatchupEngine.catchupChunk(
                        level, level.getChunkAt(pos), 600_000L); // 10 分钟
                int produced = furnace.getItem(2).getCount();
                int leftIn = furnace.getItem(0).getCount();
                if (produced >= 1) {
                    pass++;
                    out.add("PASS T13 离线追赶熔炉补偿（模拟离线 10 分钟 → 产出铁锭 " + produced
                            + " 个，输入槽剩 " + leftIn + "）");
                } else {
                    fail++;
                    out.add("FAIL T13 离线追赶未产出（输出槽 " + produced + "，输入槽 " + leftIn + "）");
                }
                level.removeBlock(pos, false);
            }
        } catch (Throwable t) {
            fail++;
            out.add("FAIL T13 离线追赶测试异常：" + t);
        }

        // T14 离线追赶 v1.2.3 新增的两条通路：世界时钟可取 + 玩家周围入队
        //     这两条通路覆盖「区块从未卸载」的场景（暂停/休眠/跳过夜晚），旧实现下恒零补偿。
        try {
            var level = server.overworld();
            long clock = com.minecraftloveyou.module.offlinecatchup.OfflineCatchupModule.worldClock(server);
            var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "wly_selftest_stall");
            var probe = new net.minecraft.server.level.ServerPlayer(server, level, profile,
                    net.minecraft.server.level.ClientInformation.createDefault());
            int queued = com.minecraftloveyou.module.offlinecatchup.OfflineCatchupModule
                    .enqueueAroundForTest(probe, 600_000L); // 10 分钟
            if (clock > 0L && queued > 0) {
                pass++;
                out.add("PASS T14 离线追赶停摆/睡觉通路（世界时钟=" + clock + " 刻，玩家周围入队 " + queued + " 区块）");
            } else {
                fail++;
                out.add("FAIL T14 停摆/睡觉通路不通（世界时钟=" + clock + "（期望 >0） 入队=" + queued + "（期望 >0））");
            }
        } catch (Throwable t) {
            fail++;
            out.add("FAIL T14 停摆/睡觉通路异常：" + t);
        }

        // T15 经验值计费：经验值点数换算必须与原版分段公式一致
        //     已知锚点：升到 30 级共需 1395 点；升到 15 级共需 315 点。
        try {
            var level = server.overworld();
            var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "wly_selftest_xp");
            var probe = new net.minecraft.server.level.ServerPlayer(server, level, profile,
                    net.minecraft.server.level.ClientInformation.createDefault());
            probe.experienceLevel = 30;
            probe.experienceProgress = 0.0F;
            int at30 = com.minecraftloveyou.module.xppoints.XpPointsModule.of(probe);
            probe.experienceLevel = 15;
            int at15 = com.minecraftloveyou.module.xppoints.XpPointsModule.of(probe);
            probe.experienceLevel = 0;
            int at0 = com.minecraftloveyou.module.xppoints.XpPointsModule.of(probe);
            if (at30 == 1395 && at15 == 315 && at0 == 0) {
                pass++;
                out.add("PASS T15 经验值点数换算（0 级=" + at0 + "，15 级=" + at15 + "，30 级=" + at30 + "）");
            } else {
                fail++;
                out.add("FAIL T15 经验值点数换算异常（0 级=" + at0 + "（期望 0），15 级=" + at15
                        + "（期望 315），30 级=" + at30 + "（期望 1395））");
            }
        } catch (Throwable t) {
            fail++;
            out.add("FAIL T15 经验值点数换算异常：" + t);
        }

        // T16 铁砧修复：状态映射（损坏→开裂→完好→null）+ 朝向保留 + 配置接线
        try {
            var facing = net.minecraft.world.level.block.AnvilBlock.FACING;
            var damaged = net.minecraft.world.level.block.Blocks.DAMAGED_ANVIL.defaultBlockState()
                    .setValue(facing, net.minecraft.core.Direction.NORTH);
            var toChipped = com.minecraftloveyou.module.anvilrepair.AnvilRepairModule.repairedState(damaged);
            var toAnvil = toChipped == null ? null
                    : com.minecraftloveyou.module.anvilrepair.AnvilRepairModule.repairedState(toChipped);
            var pristine = toAnvil == null ? null
                    : com.minecraftloveyou.module.anvilrepair.AnvilRepairModule.repairedState(toAnvil);
            boolean facingKept = toChipped != null
                    && toChipped.hasProperty(facing)
                    && toChipped.getValue(facing) == net.minecraft.core.Direction.NORTH;
            int cost = com.minecraftloveyou.module.anvilrepair.AnvilRepairModule.repairCost;
            boolean chain = toChipped != null
                    && toChipped.getBlock() == net.minecraft.world.level.block.Blocks.CHIPPED_ANVIL
                    && toAnvil != null
                    && toAnvil.getBlock() == net.minecraft.world.level.block.Blocks.ANVIL
                    && pristine == null;
            if (chain && facingKept && cost >= 1 && cost <= 31) {
                pass++;
                out.add("PASS T16 铁砧修复映射（损坏→开裂→完好→null，朝向保留，每档 " + cost + " 锭）");
            } else {
                fail++;
                out.add("FAIL T16 铁砧修复映射异常（链路=" + chain + "（期望 true） 朝向保留=" + facingKept
                        + " 每档=" + cost + "（期望 1~31））");
            }
        } catch (Throwable t) {
            fail++;
            out.add("FAIL T16 铁砧修复映射异常：" + t);
        }

        // T17 饵钓超上限的咬钩计时死锁兜底（LureTimerGuardMixin）
        //     真实构造 lureSpeed=800（=饵钓 8 级）的浮漂，反射调一次 catchingFish：
        //     该次调用只会走最后的 else 分支去播种 timeUntilLured，不碰其它状态。
        //     原版算式是 nextInt(100,600) - lureSpeed，饵钓 8 时恒 ≤ 0 ⇒ 永不咬钩；
        //     兜底后必须恒 ≥ 1。同时该注入带 require=1，没绑上会在类加载期直接抛，不会静默。
        try {
            var hookType = BuiltInRegistries.ENTITY_TYPE
                    .get(net.minecraft.resources.Identifier.withDefaultNamespace("fishing_bobber"))
                    .map(net.minecraft.core.Holder.Reference::value).orElse(null);
            if (hookType == null) {
                fail++;
                out.add("FAIL T17 取不到 fishing_bobber 实体类型");
            } else {
                boolean elbWas = com.minecraftloveyou.core.ModuleManager.enabled("elb");
                if (!elbWas) {
                    com.minecraftloveyou.config.WlyConfigData.put("elb", true);
                }
                int minSeen = Integer.MAX_VALUE;
                int minSeenNoGuard = Integer.MAX_VALUE;
                int worstAcrossLevels = Integer.MAX_VALUE;
                int saturatedField = -1;
                try {
                    var ctor = net.minecraft.world.entity.projectile.FishingHook.class
                            .getDeclaredConstructor(net.minecraft.world.entity.EntityType.class,
                                    net.minecraft.world.level.Level.class, int.class, int.class);
                    ctor.setAccessible(true);
                    var catchM = net.minecraft.world.entity.projectile.FishingHook.class
                            .getDeclaredMethod("catchingFish", net.minecraft.core.BlockPos.class);
                    catchM.setAccessible(true);
                    var lureF = net.minecraft.world.entity.projectile.FishingHook.class
                            .getDeclaredField("timeUntilLured");
                    lureF.setAccessible(true);
                    var speedF = net.minecraft.world.entity.projectile.FishingHook.class
                            .getDeclaredField("lureSpeed");
                    speedF.setAccessible(true);
                    var lvl = server.overworld();
                    // 全区间扫描：0 / 原版满级 300 / 出问题的 800 / 饱和点上下 / 溢出边界
                    int[] levels = {0, 100, 300, 500, 600, 800, 5000, 20000, Integer.MAX_VALUE};
                    for (int lure : levels) {
                        for (int trial = 0; trial < 300; trial++) {
                            Object hook = ctor.newInstance(hookType, lvl, 4, lure);
                            catchM.invoke(hook, net.minecraft.core.BlockPos.ZERO);
                            int seeded = lureF.getInt(hook);
                            worstAcrossLevels = Math.min(worstAcrossLevels, seeded);
                            if (lure == 800) {
                                minSeen = Math.min(minSeen, seeded);
                            }
                        }
                    }
                    // 饱和必须真的发生：喂进 MAX_VALUE，字段要被压回 5000（否则 +1 会溢出回绕）
                    saturatedField = speedF.getInt(ctor.newInstance(hookType, lvl, 4, Integer.MAX_VALUE));
                    // 反向锁：关掉模块后同一套调用必须重新变回「恒 ≤ 0」，
                    // 否则说明这条兜底根本没判在真交付的那条路径上。
                    com.minecraftloveyou.config.WlyConfigData.put("elb", false);
                    for (int trial = 0; trial < 400; trial++) {
                        Object hook = ctor.newInstance(hookType, lvl, 4, 800);
                        catchM.invoke(hook, net.minecraft.core.BlockPos.ZERO);
                        minSeenNoGuard = Math.min(minSeenNoGuard, lureF.getInt(hook));
                    }
                } finally {
                    com.minecraftloveyou.config.WlyConfigData.put("elb", elbWas);
                }
                if (minSeen >= 1 && minSeenNoGuard <= 0
                        && worstAcrossLevels >= 1 && saturatedField == 5000) {
                    pass++;
                    out.add("PASS T17 咬钩计时兜底：饵钓8 最小 timeUntilLured=" + minSeen
                            + "；全区间(0~MAX_VALUE)最差=" + worstAcrossLevels
                            + "；喂 MAX_VALUE 后 lureSpeed 饱和为 " + saturatedField
                            + "；关模块最小=" + minSeenNoGuard + "（复现原版死锁）");
                } else {
                    fail++;
                    out.add("FAIL T17 兜底异常：饵钓8=" + minSeen + "（需≥1）"
                            + " 全区间最差=" + worstAcrossLevels + "（需≥1）"
                            + " 饱和后字段=" + saturatedField + "（需=5000，否则 +1 会溢出回绕）"
                            + " 关模块=" + minSeenNoGuard + "（需≤0）");
                }
            }
        } catch (Throwable t) {
            fail++;
            Throwable root = t;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            StackTraceElement top = root.getStackTrace().length > 0 ? root.getStackTrace()[0] : null;
            out.add("FAIL T17 咬钩计时兜底异常：" + t + " ← 根因 " + root.getClass().getName()
                    + ": " + root.getMessage() + " @ " + top);
        }

        // T18 启用状态缓存：配置改动必须让缓存立即失效（否则改开关要重启游戏才生效）
        try {
            String probe = "rendi";
            boolean before = com.minecraftloveyou.core.WlyModule.on(probe);
            com.minecraftloveyou.config.WlyConfigData.put(probe, !before);
            boolean flipped = com.minecraftloveyou.core.WlyModule.on(probe);
            com.minecraftloveyou.config.WlyConfigData.put(probe, before);
            boolean restored = com.minecraftloveyou.core.WlyModule.on(probe);
            if (flipped == !before && restored == before) {
                pass++;
                out.add("PASS T18 启用状态缓存（put 立即生效：原=" + before + " → 切=" + flipped
                        + " → 还原=" + restored + "）");
            } else {
                fail++;
                out.add("FAIL T18 启用状态缓存未失效（原=" + before + "，期望切=" + (!before)
                        + " 实得=" + flipped + "；还原期望=" + before + " 实得=" + restored + "）");
            }
        } catch (Throwable t) {
            fail++;
            out.add("FAIL T18 启用状态缓存异常：" + t);
        }

        WorldLovesYouMod.LOGGER.info("[我的世界爱你][自检] ===== 自检结果 {} PASS / {} FAIL =====", pass, fail);
        for (String line : out) {
            WorldLovesYouMod.LOGGER.info("[我的世界爱你][自检] " + line);
        }
    }

    /**
     * 抽取 lang json 的顶层键名集合（这些语言文件是扁平的 key: value，逐行正则足够）。
     * 用于 T10 做「中英键集必须完全一致」的断言。
     */
    private static java.util.Set<String> langKeys(String json) {
        java.util.Set<String> keys = new java.util.HashSet<>();
        var p = java.util.regex.Pattern.compile("^\\s*\"([^\"]+)\"\\s*:");
        for (String line : json.split("\n")) {
            var m = p.matcher(line);
            if (m.find()) {
                keys.add(m.group(1));
            }
        }
        return keys;
    }

    /**
     * 兼容取「雪傀儡」实体类型 —— 本项目第 3 个跨版本分叉点：
     * <ul>
     *   <li>26.1.2：常量挂在 {@code net.minecraft.world.entity.EntityType.SNOW_GOLEM}；</li>
     *   <li>26.2　：常量整体搬到 {@code net.minecraft.world.entity.EntityTypes.SNOW_GOLEM}
     *       （{@code EntityType} 只剩类型本身，不再持有任何实体常量）。</li>
     * </ul>
     * 两版类名不同，无法用同一个符号编译，故用反射兜底。仅自检用，不影响发布代码。
     */
    @SuppressWarnings("unchecked")
    private static net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.animal.golem.SnowGolem> snowGolemType() {
        for (String holder : new String[]{
                "net.minecraft.world.entity.EntityType",
                "net.minecraft.world.entity.EntityTypes"}) {
            try {
                return (net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.animal.golem.SnowGolem>)
                        Class.forName(holder).getField("SNOW_GOLEM").get(null);
            } catch (ReflectiveOperationException ignored) {
                // 该版本没有这个名字，试下一个
            }
        }
        throw new IllegalStateException("找不到 SNOW_GOLEM 实体类型（EntityType / EntityTypes 两版命名都不匹配）");
    }
}